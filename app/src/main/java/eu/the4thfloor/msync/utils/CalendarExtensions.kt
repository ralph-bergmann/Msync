/*
 * Copyright 2017 Ralph Bergmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.the4thfloor.msync.utils

import android.Manifest
import android.accounts.Account
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract.Attendees
import android.provider.CalendarContract.CALLER_IS_SYNCADAPTER
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import android.support.annotation.RequiresPermission
import android.text.format.Time
import eu.the4thfloor.msync.R
import eu.the4thfloor.msync.api.models.Event
import org.jetbrains.anko.defaultSharedPreferences
import timber.log.Timber
import java.util.concurrent.TimeUnit.HOURS

@RequiresPermission(allOf = [(Manifest.permission.READ_CALENDAR), (Manifest.permission.WRITE_CALENDAR)])
fun Context.getCalendarID(): Long? {

    val account = getAccount() ?: return null

    val calendarCursor =
        contentResolver.query(Calendars.CONTENT_URI,
                              arrayOf(Calendars._ID),
                              "${Calendars.ACCOUNT_NAME} = ? AND ${Calendars.ACCOUNT_TYPE} = ?",
                              arrayOf(account.name, account.type),
                              null)

    return if (calendarCursor.count <= 0) {
        calendarCursor.close()
        null
    } else {
        calendarCursor.moveToFirst()
        val id = calendarCursor.getLong(calendarCursor.getColumnIndex(Calendars._ID))
        calendarCursor.close()
        id
    }
}

@RequiresPermission(allOf = [(Manifest.permission.READ_CALENDAR), (Manifest.permission.WRITE_CALENDAR)])
fun Context.createCalendar(): Long? {

    val account = getAccount() ?: return null

    val calendarName = defaultSharedPreferences.getString("pref_key_calendar_name", getString(R.string.pref_default_calendar_name))
    val calendarColor = defaultSharedPreferences.getInt("pref_key_calendar_color", resources.getColor(R.color.red))

    val values = ContentValues()
    values.put(Calendars.NAME, calendarName)
    values.put(Calendars.CALENDAR_DISPLAY_NAME, calendarName)
    values.put(Calendars.CALENDAR_COLOR, calendarColor)
    values.put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_OWNER)
    values.put(Calendars.OWNER_ACCOUNT, account.name)
    values.put(Calendars.ACCOUNT_NAME, account.name)
    values.put(Calendars.ACCOUNT_TYPE, account.type)
    values.put(Calendars.SYNC_EVENTS, 1)
    values.put(Calendars.CALENDAR_TIME_ZONE, Time.getCurrentTimezone())

    return ContentUris.parseId(contentResolver.insert(contentUri(Calendars.CONTENT_URI, account), values))
}

fun Context.updateCalendarName() {

    val account = getAccount() ?: return
    val id = getCalendarID() ?: return

    val calendarName = defaultSharedPreferences.getString("pref_key_calendar_name", getString(R.string.pref_default_calendar_name))

    val values = ContentValues()
    values.put(Calendars.NAME, calendarName)
    values.put(Calendars.CALENDAR_DISPLAY_NAME, calendarName)

    val where = "${Calendars._ID} = ?"
    contentResolver.update(contentUri(Calendars.CONTENT_URI, account),
                           values,
                           where,
                           arrayOf(id.toString()))
}

fun Context.updateCalendarColor() {

    val account = getAccount() ?: return
    val id = getCalendarID() ?: return

    val calendarColor = defaultSharedPreferences.getInt("pref_key_calendar_color", resources.getColor(R.color.red))

    val values = ContentValues()
    values.put(Calendars.CALENDAR_COLOR, calendarColor)

    val where = "${Calendars._ID} = ?"
    contentResolver.update(contentUri(Calendars.CONTENT_URI, account),
                           values,
                           where,
                           arrayOf(id.toString()))
}


/**
 * @return id of the entry in sqlite or null if something is broken
 */
fun Context.addEvent(event: Event): Long? {

    val account = getAccount() ?: return null
    val calendarId = getCalendarID() ?: return null

    val cursor = contentResolver.query(contentUri(Events.CONTENT_URI, account),
                                       arrayOf(Events._ID,
                                               Events.SELF_ATTENDEE_STATUS,
                                               Events.SYNC_DATA1,
                                               Events.DELETED),
                                       "${Events.CALENDAR_ID} = ? AND ${Events._SYNC_ID} = ?",
                                       arrayOf(calendarId.toString(), event.id),
                                       null)

    if (cursor.count == 0) {
        cursor.close()
        Timber.v("event %s not found -> insert", event.name)
        return insertEvent(account, contentValues(calendarId, event, true))
    }

    cursor.moveToFirst()
    val id = cursor.getLong(cursor.getColumnIndex(Events._ID))
    val rsvpStatus = cursor.getInt(cursor.getColumnIndex(Events.SELF_ATTENDEE_STATUS))
    val updatedTime = cursor.getLong(cursor.getColumnIndex(Events.SYNC_DATA1))
    val deleted = cursor.getInt(cursor.getColumnIndex(Events.DELETED))
    cursor.close()


    if (updatedTime != event.updated || deleted == 1) {
        Timber.v("event %s changed -> update", event.name)
        updateEvent(account, id, contentValues(calendarId, event))
    }


    event.self.rsvp?.response?.let { rsvp ->
        if (rsvpStatus != rsvp.value) {
            Timber.v("event %s rsvp changed -> update from %d to %d",
                     event.name,
                     rsvpStatus,
                     rsvp.value)
            val values = ContentValues()
            values.put(Attendees.ATTENDEE_STATUS, rsvp.value)
            updateSelfAttendeeStatus(account, id, values)
        }
    }


    return id
}

fun Context.deleteEventsNotIn(ids: List<Long>) {
    val account = getAccount() ?: return
    val where = "${Events._ID} NOT IN (${ids.joinToString()})"
    Timber.v("delete removed events where \"%s\"", where)
    contentResolver.delete(contentUri(Events.CONTENT_URI, account), where, null)
}

private fun contentValues(calendarId: Long,
                          event: Event,
                          forInsert: Boolean = false): ContentValues {

    val values = ContentValues()

    values.put(Events._SYNC_ID, event.id)
    values.put(Events.CALENDAR_ID, calendarId)
    values.put(Events.SYNC_DATA1, event.updated)
    values.put(Events.DELETED, 0)

    values.put(Events.DTSTART, event.time)
    values.put(Events.DTEND, event.time + (event.duration ?: HOURS.toMillis(3)))
    values.put(Events.EVENT_TIMEZONE, "UTC")

    values.put(Events.TITLE, event.name.trim())
    values.put(Events.DESCRIPTION, StringBuilder().apply {
        event.plain_text_description?.let { append(it.trim()) }
        append("\n\nDetails: ${event.link.trim()}")
    }.toString())

    event.venue?.let { venue ->
        values.put(Events.EVENT_LOCATION, mutableListOf<String>().apply {
            add(venue.name.trim())
            add(venue.address_1.trim())
            venue.address_2?.let { add(it.trim()) }
            venue.address_3?.let { add(it.trim()) }
            add(venue.city.trim())
            add(venue.localized_country_name.trim())
        }.joinToString(", "))
    }

    values.put(Events.ORGANIZER, event.group.name)

    // SELF_ATTENDEE_STATUS kann nur bei insert, nicht jedoch bei update gesetzt werden
    if (forInsert) {
        event.self.rsvp?.response?.let { rsvp ->
            values.put(Events.SELF_ATTENDEE_STATUS, rsvp.value)
        }
    }

    values.put(Events.HAS_ATTENDEE_DATA, true)
    values.put(Events.GUESTS_CAN_MODIFY, false)
    values.put(Events.GUESTS_CAN_INVITE_OTHERS, false)
    values.put(Events.GUESTS_CAN_SEE_GUESTS, false)

    return values
}

private fun Context.insertEvent(account: Account, values: ContentValues): Long =
    ContentUris.parseId(contentResolver.insert(contentUri(Events.CONTENT_URI, account), values))

private fun Context.updateEvent(account: Account, id: Long, values: ContentValues): Int {
    return contentResolver.update(contentUri(Events.CONTENT_URI, account),
                                  values,
                                  Events._ID + " = ?",
                                  arrayOf(id.toString()))
}

private fun Context.updateSelfAttendeeStatus(account: Account, id: Long, values: ContentValues) {
    contentResolver.update(contentUri(Attendees.CONTENT_URI, account),
                           values,
                           Attendees.EVENT_ID + " = ?",
                           arrayOf(id.toString()))
}


private fun contentUri(uri: Uri, account: Account): Uri =
    uri.buildUpon()
        .appendQueryParameter(CALLER_IS_SYNCADAPTER, "true")
        .appendQueryParameter(Calendars.ACCOUNT_NAME, account.name)
        .appendQueryParameter(Calendars.ACCOUNT_TYPE, account.type).build()
