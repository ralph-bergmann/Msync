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

import android.content.Context
import com.google.firebase.crash.FirebaseCrash
import com.jakewharton.retrofit2.adapter.rxjava2.HttpException
import com.squareup.moshi.Moshi
import eu.the4thfloor.msync.BuildConfig
import eu.the4thfloor.msync.api.MeetupApi
import eu.the4thfloor.msync.api.SecureApi
import eu.the4thfloor.msync.api.models.AccessResponse
import eu.the4thfloor.msync.api.models.CalendarResponse
import eu.the4thfloor.msync.api.models.ErrorResponse
import eu.the4thfloor.msync.api.models.Response
import eu.the4thfloor.msync.ui.Request
import eu.the4thfloor.msync.ui.ResponseModel
import eu.the4thfloor.msync.utils.addEvent
import eu.the4thfloor.msync.utils.deleteEventsNotIn
import io.reactivex.Flowable
import io.reactivex.FlowableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

private val FIELDS = "self,plain_text_description,link"
private val ONLY = "id,name,time,utc_offset,duration,updated,venue,link,self,plain_text_description"

fun sync(secureApi: SecureApi,
         meetupApi: MeetupApi,
         refreshToken: String,
         disposables: CompositeDisposable,
         context: Context,
         finish: () -> Unit) {

    val moshi = Moshi.Builder().build().adapter(ErrorResponse::class.java)
    val request = PublishProcessor.create<Request>()

    fun handleResponse(response: AccessResponse) {
        request.onNext(Request.Calendar(response.access_token!!))
    }

    fun handleResponse(response: CalendarResponse) {

        val ids = mutableListOf<Long>()
        response.events
            .forEach { event ->
                val id = context.addEvent(event)
                id?.let { ids.add(it) }
            }
        context.deleteEventsNotIn(ids)

        finish()
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
                                moshi.fromJson(t.response().errorBody().string())
                            } else {
                                null
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
                                moshi.fromJson(t.response().errorBody().string())
                            } else {
                                null
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
                                        FirebaseCrash.report(Exception(error?.error_description))
                                        finish()
                                    }
                                }
                            },
                            { error ->
                                Timber.e(error, "failed to access api")
                                FirebaseCrash.report(error)
                                finish()
                            }))

    // start signal
    request.onNext(Request.Access(refreshToken))
}
