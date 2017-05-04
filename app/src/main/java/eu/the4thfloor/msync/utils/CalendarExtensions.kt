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
import android.provider.CalendarContract
import android.provider.CalendarContract.Attendees.ATTENDEE_STATUS_INVITED
import android.provider.CalendarContract.Events.CALENDAR_ID
import android.provider.CalendarContract.Events.CONTENT_URI
import android.provider.CalendarContract.Events.DESCRIPTION
import android.provider.CalendarContract.Events.DTEND
import android.provider.CalendarContract.Events.DTSTART
import android.provider.CalendarContract.Events.EVENT_LOCATION
import android.provider.CalendarContract.Events.EVENT_TIMEZONE
import android.provider.CalendarContract.Events.GUESTS_CAN_INVITE_OTHERS
import android.provider.CalendarContract.Events.GUESTS_CAN_MODIFY
import android.provider.CalendarContract.Events.GUESTS_CAN_SEE_GUESTS
import android.provider.CalendarContract.Events.HAS_ATTENDEE_DATA
import android.provider.CalendarContract.Events.SELF_ATTENDEE_STATUS
import android.provider.CalendarContract.Events.SYNC_DATA1
import android.provider.CalendarContract.Events.TITLE
import android.provider.CalendarContract.Events._ID
import android.provider.CalendarContract.Events._SYNC_ID
import android.support.annotation.RequiresPermission
import android.text.format.Time
import eu.the4thfloor.msync.R
import eu.the4thfloor.msync.api.models.Event
import org.jetbrains.anko.defaultSharedPreferences
import timber.log.Timber
import java.util.concurrent.TimeUnit.HOURS

@RequiresPermission(allOf = arrayOf(Manifest.permission.READ_CALENDAR,
                                    Manifest.permission.WRITE_CALENDAR))
fun Context.getCalendarID(account: Account): Long? {

    val calendarCursor =
        contentResolver.query(CalendarContract.Calendars.CONTENT_URI,
                              arrayOf(CalendarContract.Calendars._ID),
                              "${CalendarContract.Calendars.ACCOUNT_NAME} = ? AND ${CalendarContract.Calendars.ACCOUNT_TYPE} = ?",
                              arrayOf(account.name, account.type),
                              null)

    if (calendarCursor.count <= 0) {
        calendarCursor.close()
        return null
    } else {
        calendarCursor.moveToFirst()
        val id = calendarCursor.getLong(calendarCursor.getColumnIndex(CalendarContract.Calendars._ID))
        calendarCursor.close()
        return id
    }
}

@RequiresPermission(allOf = arrayOf(Manifest.permission.READ_CALENDAR,
                                    Manifest.permission.WRITE_CALENDAR))
fun Context.createCalendar(account: Account): Long {

    val calendarName = defaultSharedPreferences.getString("pref_key_calendar_name",
                                                          getString(R.string.pref_default_calendar_name))
    val calendarColor = defaultSharedPreferences.getInt("pref_key_calendar_color",
                                                        resources.getColor(R.color.red))

    val values = ContentValues()
    values.put(CalendarContract.Calendars.NAME, calendarName)
    values.put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, calendarName)
    values.put(CalendarContract.Calendars.CALENDAR_COLOR, calendarColor)
    values.put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
    values.put(CalendarContract.Calendars.OWNER_ACCOUNT, account.name)
    values.put(CalendarContract.Calendars.ACCOUNT_NAME, account.name)
    values.put(CalendarContract.Calendars.ACCOUNT_TYPE, account.type)
    values.put(CalendarContract.Calendars.SYNC_EVENTS, 1)
    values.put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, Time.getCurrentTimezone())

    return ContentUris.parseId(contentResolver.insert(contentUri(CalendarContract.Calendars.CONTENT_URI,
                                                                 account), values))
}


/**
 * @return id of the entry in sqlite or null if something is broken
 */
fun Context.addEvent(event: Event): Long? {

    val account = getAccount() ?: return null
    val calendarId = getCalendarID(account) ?: return null

    val cursor = contentResolver.query(contentUri(CONTENT_URI, account),
                                       arrayOf(_ID, SELF_ATTENDEE_STATUS, SYNC_DATA1),
                                       "$CALENDAR_ID = ? AND $_SYNC_ID = ?",
                                       arrayOf(calendarId.toString(), event.id),
                                       null)


    if (cursor.count == 0) {
        cursor.close()
        Timber.v("event %s not found -> insert", event.name)
        return insertEvent(account, contentValues(calendarId, event, true))
    }

    cursor.moveToFirst()
    val id = cursor.getLong(cursor.getColumnIndex(_ID))
    val updated_time = cursor.getLong(cursor.getColumnIndex(SYNC_DATA1))
    val rsvp_status = cursor.getInt(cursor.getColumnIndex(SELF_ATTENDEE_STATUS))
    cursor.close()


    if (updated_time != event.updated) {
        Timber.v("event %s changed -> update", event.name)
        updateEvent(account, id, contentValues(calendarId, event, false))
    }


    event.self?.rsvp?.response?.let { rsvp ->
        if (rsvp_status != rsvp.value) {
            Timber.v("event %s rsvp changed -> update to %d", event.name, rsvp.value)

            val values = ContentValues()
            values.put(CalendarContract.Attendees.ATTENDEE_STATUS, rsvp.value)

            updateSelfAttendeeStatus(account, id, values)
        }
    }


    return id
}

fun Context.deleteEventsNotIn(ids: List<Long>) {
    val account = getAccount() ?: return
    val where = "$_ID NOT IN (${ids.joinToString()})"
    Timber.v("delete removed events where \"%s\"", where)
    contentResolver.delete(contentUri(CONTENT_URI, account), where, null)
}

private fun contentValues(calendarId: Long, event: Event, forInsert: Boolean): ContentValues {

    val values = ContentValues()

    values.put(_SYNC_ID, event.id)
    values.put(CALENDAR_ID, calendarId)
    values.put(SYNC_DATA1, event.updated)

    values.put(DTSTART, event.time!!)
    values.put(DTEND, event.time!! + (event.duration ?: HOURS.toMillis(3)))

    event.utc_offset?.let {
        values.put(EVENT_TIMEZONE, Time.getCurrentTimezone())

        // TODO
        // values.put(EVENT_TIMEZONE, TimeZone.getAvailableIDs(it).firstOrNull() ?: Time.getCurrentTimezone())
    }

    values.put(TITLE, event.name)
    values.put(DESCRIPTION, event.plain_text_description)

    event.venue?.let { venue ->
        values.put(EVENT_LOCATION, mutableListOf<String>().apply {
            venue.name?.let { add(it) }
            venue.address_1?.let { add(it) }
            venue.city?.let { add(it) }
            venue.localized_country_name?.let { add(it) }
        }.joinToString(", "))
    }


    values.put(HAS_ATTENDEE_DATA, true)
    values.put(GUESTS_CAN_MODIFY, false)
    values.put(GUESTS_CAN_INVITE_OTHERS, false)
    values.put(GUESTS_CAN_SEE_GUESTS, false)

    // SELF_ATTENDEE_STATUS kann nur bei insert, nicht jedoch bei update gesetzt werden
    if (forInsert) {
        values.put(CalendarContract.Events.SELF_ATTENDEE_STATUS,
                   event.self?.rsvp?.response?.value ?: ATTENDEE_STATUS_INVITED)
    }

    return values
}

private fun Context.insertEvent(account: Account, values: ContentValues): Long {
    return ContentUris.parseId(contentResolver.insert(contentUri(CONTENT_URI, account), values))
}

private fun Context.updateEvent(account: Account, id: Long, values: ContentValues): Int {
    return contentResolver.update(contentUri(CONTENT_URI, account),
                                  values,
                                  _ID + " = ?",
                                  arrayOf(id.toString()))
}

private fun Context.updateSelfAttendeeStatus(account: Account, id: Long, values: ContentValues) {
    contentResolver.update(contentUri(CalendarContract.Attendees.CONTENT_URI, account),
                           values,
                           CalendarContract.Attendees.EVENT_ID + " = ?",
                           arrayOf(id.toString()))
}


private fun contentUri(uri: Uri, account: Account): Uri {
    return uri.buildUpon()
        .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
        .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, account.name)
        .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, account.type).build()
}
