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
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.support.v4.app.NotificationCompat
import com.jakewharton.retrofit2.adapter.rxjava2.HttpException
import com.squareup.moshi.Moshi
import eu.the4thfloor.msync.BuildConfig
import eu.the4thfloor.msync.R
import eu.the4thfloor.msync.api.MeetupApi
import eu.the4thfloor.msync.api.SecureApi
import eu.the4thfloor.msync.api.models.AccessResponse
import eu.the4thfloor.msync.api.models.CalendarResponse
import eu.the4thfloor.msync.api.models.ErrorResponse
import eu.the4thfloor.msync.api.models.Response
import eu.the4thfloor.msync.api.models.Rsvp
import eu.the4thfloor.msync.ui.CheckLoginStatusActivity
import eu.the4thfloor.msync.ui.NotificationPermissionsActivity
import eu.the4thfloor.msync.ui.Request
import eu.the4thfloor.msync.ui.ResponseModel
import eu.the4thfloor.msync.utils.addEvent
import eu.the4thfloor.msync.utils.apply
import eu.the4thfloor.msync.utils.checkSelfPermission
import eu.the4thfloor.msync.utils.deleteEventsNotIn
import eu.the4thfloor.msync.utils.getRefreshToken
import io.reactivex.Flowable
import io.reactivex.FlowableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.doFromSdk
import org.jetbrains.anko.notificationManager
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

private val FIELDS = "self,plain_text_description,link"
private val ONLY = "id,name,time,utc_offset,duration,updated,venue,group,link,self,plain_text_description"

private fun Context.saveEvents(response: CalendarResponse) {

    val sync_yes = defaultSharedPreferences.getBoolean("pref_key_sync_event_yes", true)
    val sync_waitlist = defaultSharedPreferences.getBoolean("pref_key_sync_event_waitlist", true)
    val sync_unanswered = defaultSharedPreferences.getBoolean("pref_key_sync_event_unanswered", false)
    val sync_no = defaultSharedPreferences.getBoolean("pref_key_sync_event_no", false)
    val ids = mutableListOf<Long>()

    response.events
        .forEach { event ->
            when (event.self?.rsvp?.response ?: Rsvp.notanswered) {
                Rsvp.yes -> {
                    if (sync_yes) addEvent(event) else null
                }
                Rsvp.yes_pending_payment -> {
                    if (sync_yes) addEvent(event) else null
                }
                Rsvp.no -> {
                    if (sync_no) addEvent(event) else null
                }
                Rsvp.waitlist -> {
                    if (sync_waitlist) addEvent(event) else null
                }
                Rsvp.notanswered -> {
                    if (sync_unanswered) addEvent(event) else null
                }
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

fun sync(secureApi: SecureApi,
         meetupApi: MeetupApi,
         disposables: CompositeDisposable,
         context: Context,
         finish: () -> Unit) {

    val refreshToken = context.getRefreshToken()
    if (refreshToken == null) {
        context.showLoginNotification()
        return
    }

    doFromSdk(Build.VERSION_CODES.M, {
        if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR,
                                        Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            context.showPermissionsNotification()
            return
        }
    })

    context.defaultSharedPreferences.apply("pref_key_last_sync" to context.getString(R.string.syncing_now))

    val moshi = Moshi.Builder().build().adapter(ErrorResponse::class.java)
    val request = PublishProcessor.create<Request>()

    fun done() {

        val sdf = SimpleDateFormat.getDateTimeInstance().format(Date())
        context.defaultSharedPreferences.apply("pref_key_last_sync" to context.getString(R.string.last_synced, sdf))
        finish()
    }

    fun handleResponse(response: AccessResponse) {
        request.onNext(Request.Calendar(response.access_token!!))
    }

    fun handleResponse(response: CalendarResponse) {
        launch(CommonPool) {
            kotlin.run { context.saveEvents(response) }
            launch(UI) { done() }
        }
    }

    val accessTransformer =
        { flowable: Flowable<Request.Access> ->
            flowable
                .flatMap { access ->
                    secureApi
                        .access(mapOf("client_id" to BuildConfig.MEETUP_OAUTH_KEY,
                                      "client_secret" to BuildConfig.MEETUP_OAUTH_SECRET,
                                      "refresh_token" to access.code,
                                      "grant_type" to "refresh_token"))
                        .map { response -> ResponseModel.success(response) }
                        .onErrorReturn { t ->
                            ResponseModel.failure(if (t is HttpException) {
                                                      t.response().errorBody()?.let {
                                                          moshi.fromJson(it.string())
                                                      } ?: ErrorResponse().apply {
                                                          error = t.message
                                                      }
                                                  } else {
                                                      ErrorResponse().apply {
                                                          error = t.message
                                                      }
                                                  })
                        }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .startWith(ResponseModel.inProgress())
                }
        }

    val eventsTransformer =
        { flowable: Flowable<Request.Calendar> ->
            flowable
                .flatMap { event ->
                    meetupApi.calendar("Bearer ${event.accessToken}", FIELDS, ONLY)
                        .map { response ->
                            ResponseModel.success(CalendarResponse(response))
                        }
                        .onErrorReturn { t ->
                            ResponseModel.failure(if (t is HttpException) {
                                                      t.response().errorBody()?.let {
                                                          moshi.fromJson(it.string())
                                                      }?: ErrorResponse().apply {
                                                          error = t.message
                                                      }
                                                  } else {
                                                      ErrorResponse().apply {
                                                          error = t.message
                                                      }
                                                  })
                        }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .startWith(ResponseModel.inProgress())
                }
        }

    val requestTransformer = FlowableTransformer<Request, ResponseModel<Response>> { flowable ->
        flowable
            .publish({ shared ->
                         Flowable.merge(
                             shared.ofType(Request.Calendar::class.java).compose(eventsTransformer),
                             shared.ofType(Request.Access::class.java).compose(accessTransformer))
                     })
    }

    disposables
        .add(request
                 .compose(requestTransformer)
                 .subscribe({ (inProgress, success, response, error) ->
                                if (!inProgress) {
                                    if (success && response != null) {
                                        when (response) {
                                            is AccessResponse   -> {
                                                Timber.i("success %s", response.access_token)
                                                handleResponse(response)
                                            }
                                            is CalendarResponse -> {
                                                Timber.i("events %s", response.events)
                                                handleResponse(response)
                                            }
                                        }
                                    } else {
                                        Timber.e("!success %s", error)
                                        done()
                                    }
                                }
                            },
                            { error ->
                                Timber.e(error, "failed to access api")
                                done()
                            }))

    // start signal
    request.onNext(Request.Access(refreshToken))
}
