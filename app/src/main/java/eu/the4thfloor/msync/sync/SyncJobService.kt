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

package eu.the4thfloor.msync.sync

import android.Manifest
import android.accounts.Account
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.support.v4.app.NotificationCompat
import eu.the4thfloor.msync.BuildConfig
import eu.the4thfloor.msync.R
import eu.the4thfloor.msync.api.MeetupApi
import eu.the4thfloor.msync.api.SecureApi
import eu.the4thfloor.msync.api.models.Event
import eu.the4thfloor.msync.api.models.Rsvp
import eu.the4thfloor.msync.ui.CheckLoginStatusActivity
import eu.the4thfloor.msync.ui.NotificationPermissionsActivity
import eu.the4thfloor.msync.utils.addEvent
import eu.the4thfloor.msync.utils.apply
import eu.the4thfloor.msync.utils.checkSelfPermission
import eu.the4thfloor.msync.utils.deleteEventsNotIn
import eu.the4thfloor.msync.utils.getAccount
import eu.the4thfloor.msync.utils.getCalendarID
import eu.the4thfloor.msync.utils.getRefreshToken
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.doFromSdk
import org.jetbrains.anko.notificationManager
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

private val FIELDS = "self,plain_text_description,link"
private val ONLY = "id,name,time,utc_offset,duration,updated,venue,group,link,self,plain_text_description"

private fun Context.saveEvents(account: Account, calendarId: Long, events: List<Event>) {

    val sync_yes = defaultSharedPreferences.getBoolean("pref_key_sync_event_yes", true)
    val sync_waitlist = defaultSharedPreferences.getBoolean("pref_key_sync_event_waitlist", true)
    val sync_unanswered = defaultSharedPreferences.getBoolean("pref_key_sync_event_unanswered", false)
    val sync_no = defaultSharedPreferences.getBoolean("pref_key_sync_event_no", false)
    val ids = mutableListOf<Long>()

    events.forEach { event ->
        when (event.self.rsvp?.response) {
            Rsvp.yes, Rsvp.yes_pending_payment -> if (sync_yes) addEvent(account, calendarId, event) else null
            Rsvp.no                            -> if (sync_no) addEvent(account, calendarId, event) else null
            Rsvp.waitlist                      -> if (sync_waitlist) addEvent(account, calendarId, event) else null
            else                               -> if (sync_unanswered) addEvent(account, calendarId, event) else null
        }?.let { ids.add(it) }
    }
    deleteEventsNotIn(ids)
}

private fun Context.showLoginNotification() {
    notificationManager
        .notify(0,
                NotificationCompat.Builder(this)
                    .apply {
                        setSmallIcon(R.drawable.ic_notification)
                        setContentTitle(getString(R.string.app_name))
                        setContentText(getString(R.string.please_log_in))
                        setContentIntent(PendingIntent.getActivity(mContext,
                                                                   0,
                                                                   Intent(mContext, CheckLoginStatusActivity::class.java)
                                                                       .apply {
                                                                           flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                                       },
                                                                   PendingIntent.FLAG_UPDATE_CURRENT)
                                        )
                        setAutoCancel(true)
                    }.build())
}

private fun Context.showPermissionsNotification() {
    notificationManager
        .notify(0,
                NotificationCompat.Builder(this)
                    .apply {
                        setSmallIcon(R.drawable.ic_notification)
                        setContentTitle(getString(R.string.app_name))
                        setContentText(getString(R.string.please_approve_permissions))
                        setContentIntent(PendingIntent.getActivity(mContext,
                                                                   0,
                                                                   Intent(mContext, NotificationPermissionsActivity::class.java)
                                                                       .apply {
                                                                           flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                                       },
                                                                   PendingIntent.FLAG_UPDATE_CURRENT)
                                        )
                        setAutoCancel(true)
                    }.build())
}

fun Context.sync(secureApi: SecureApi, meetupApi: MeetupApi, finish: () -> Unit) {

    val account = getAccount()
    val calendarId = getCalendarID()
    val refreshToken = getRefreshToken()
    if (account == null || calendarId == null || refreshToken == null) {
        showLoginNotification()
        return
    }

    doFromSdk(Build.VERSION_CODES.M, {
        if (checkSelfPermission(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            showPermissionsNotification()
            return
        }
    })

    defaultSharedPreferences.apply("pref_key_last_sync" to context.getString(R.string.syncing_now))

    fun done() {
        val sdf = SimpleDateFormat.getDateTimeInstance().format(Date())
        defaultSharedPreferences.apply("pref_key_last_sync" to getString(R.string.last_synced, sdf))
        finish()
    }

    loadToken(secureApi, refreshToken) { accessToken ->
        loadCalendar(meetupApi, accessToken) { events ->
            launch(CommonPool) {
                kotlin.run { saveEvents(account, calendarId, events) }
                launch(UI) { done() }
            }
        }
    }
}

private fun loadToken(secureApi: SecureApi, refreshToken: String, handler: (accessToken: String) -> Unit) {
    Timber.i("refreshToken: %s", refreshToken)
    launch(CommonPool) {
        val result = secureApi.access(mapOf("client_id" to BuildConfig.MEETUP_OAUTH_KEY,
                                            "client_secret" to BuildConfig.MEETUP_OAUTH_SECRET,
                                            "refresh_token" to refreshToken,
                                            "grant_type" to "refresh_token")).awaitResult()
        launch(UI) {
            when (result) {
                is Result.Ok        -> handler(result.value.access_token)
                is Result.Error     -> Timber.e(result.exception, "failed to load 'secureApi.access' - code: ${result.exception.code()}")
                is Result.Exception -> Timber.e(result.exception, "Something broken")
            }
        }
    }
}

private fun loadCalendar(meetupApi: MeetupApi, accessToken: String, handler: (events: List<Event>) -> Unit) {
    Timber.i("accessToken: %s", accessToken)
    launch(CommonPool) {
        val result = meetupApi.calendar("Bearer $accessToken", FIELDS, ONLY).awaitResult()
        launch(UI) {
            when (result) {
                is Result.Ok        -> handler(result.value)
                is Result.Error     -> Timber.e(result.exception, "failed to load 'meetupApi.calendar' - code: ${result.exception.code()}")
                is Result.Exception -> Timber.e(result.exception, "Something broken")
            }
        }
    }
}
