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

package eu.the4thfloor.msync.ui

import android.Manifest
import android.accounts.AccountAuthenticatorActivity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresPermission
import android.view.Window
import com.jakewharton.retrofit2.adapter.rxjava2.HttpException
import com.squareup.moshi.Moshi
import eu.the4thfloor.msync.BuildConfig.MEETUP_OAUTH_KEY
import eu.the4thfloor.msync.BuildConfig.MEETUP_OAUTH_REDIRECT_URI
import eu.the4thfloor.msync.BuildConfig.MEETUP_OAUTH_SECRET
import eu.the4thfloor.msync.MSyncApp
import eu.the4thfloor.msync.R
import eu.the4thfloor.msync.api.MeetupApi
import eu.the4thfloor.msync.api.SecureApi
import eu.the4thfloor.msync.api.models.AccessResponse
import eu.the4thfloor.msync.api.models.CreateAccountResponse
import eu.the4thfloor.msync.api.models.ErrorResponse
import eu.the4thfloor.msync.api.models.Response
import eu.the4thfloor.msync.api.models.SelfResponse
import eu.the4thfloor.msync.utils.checkSelfPermission
import eu.the4thfloor.msync.utils.createAccount
import eu.the4thfloor.msync.utils.createCalendar
import eu.the4thfloor.msync.utils.createSyncJobs
import eu.the4thfloor.msync.utils.doFromSdk
import eu.the4thfloor.msync.utils.getCalendarID
import io.reactivex.Flowable
import io.reactivex.FlowableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.startActivity
import timber.log.Timber
import javax.inject.Inject

class LoginActivity : AccountAuthenticatorActivity() {

    @Inject lateinit var secureApi: SecureApi
    @Inject lateinit var meetupApi: MeetupApi

    private val disposables = CompositeDisposable()
    private val request = PublishProcessor.create<Request>()

    private var refreshToken: String? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        MSyncApp.graph.inject(this)
        bind()

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        intent.action?.let {
            if (it.equals(Intent.ACTION_VIEW, true)) {
                intent.data.getQueryParameter("code")?.let {
                    loadToken(it)
                    true
                }
            }
        }
    }

    private fun bind() {

        val moshi = Moshi.Builder().build().adapter(ErrorResponse::class.java)
        val accessTransformer =
            { flowable: Flowable<Request.Access> ->
                flowable
                    .flatMap { access ->
                        secureApi.access(mapOf("client_id" to MEETUP_OAUTH_KEY,
                                               "client_secret" to MEETUP_OAUTH_SECRET,
                                               "redirect_uri" to MEETUP_OAUTH_REDIRECT_URI,
                                               "code" to access.code,
                                               "grant_type" to "authorization_code"))
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

        val selfTransformer =
            { flowable: Flowable<Request.Self> ->
                flowable
                    .flatMap { self ->
                        meetupApi.self("Bearer ${self.accessToken}")
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

        val createAccountTransformer =
            { flowable: Flowable<Request.CreateAccount> ->
                flowable
                    .flatMap { createAccount ->
                        createAccount(createAccount.name, createAccount.refreshToken)
                            .map { _ -> ResponseModel.success(CreateAccountResponse()) }
                            .onErrorReturn { t ->
                                val error = ErrorResponse()
                                error.error = t.message
                                ResponseModel.failure(error)
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
                                 shared.ofType(Request.Access::class.java).compose(accessTransformer),
                                 shared.ofType(Request.Self::class.java).compose(selfTransformer),
                                 shared.ofType(Request.CreateAccount::class.java).compose(createAccountTransformer))
                         })
        }

        disposables
            .add(request
                     .compose(requestTransformer)
                     .subscribe({ (inProgress, success, response, error) ->
                                    if (!inProgress) {
                                        if (success && response != null) {
                                            when (response) {
                                                is AccessResponse -> {
                                                    Timber.i("success %s", response.access_token)
                                                    handleResponse(response)
                                                }
                                                is SelfResponse -> {
                                                    Timber.i("self %s", response)
                                                    handleResponse(response)
                                                }
                                                is CreateAccountResponse -> {
                                                    Timber.i("account %s", response)
                                                    handleResponse(response)
                                                }
                                            }
                                        } else {
                                            Timber.e("!success %s", error)
                                            showError("failed to execute: $response")
                                        }
                                    }
                                },
                                { error ->
                                    Timber.e(error, "failed to access api")
                                    showError(error.message)
                                }))
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }

    private fun loadToken(code: String) {
        request.onNext(Request.Access(code))
    }

    private fun handleResponse(response: AccessResponse) {
        refreshToken = response.refresh_token
        request.onNext(Request.Self(response.access_token!!))
    }

    private fun handleResponse(response: SelfResponse) {
        request.onNext(Request.CreateAccount(refreshToken!!, response.name!!))
    }

    private fun handleResponse(response: CreateAccountResponse) {
        doFromSdk(Build.VERSION_CODES.M,
                  {
                      if (checkSelfPermission(Manifest.permission.READ_CALENDAR,
                                              Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                          requestPermissions(arrayOf(Manifest.permission.READ_CALENDAR,
                                                     Manifest.permission.WRITE_CALENDAR), 0)
                      } else {
                          createCalendarIfNeeded()
                          createSyncJobs()
                          goNext()
                      }
                  },
                  {
                      createCalendarIfNeeded()
                      createSyncJobs()
                      goNext()
                  })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            0 -> {
                if (grantResults.size == 2
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                    createCalendarIfNeeded()
                    createSyncJobs()
                    goNext()
                }
            }
        }
    }

    @RequiresPermission(allOf = arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))
    private fun createCalendarIfNeeded() {
        if (getCalendarID() == null) {
            createCalendar()
        }
    }

    private fun createSyncJobs() {
        val syncFrequency = defaultSharedPreferences.getString("pref_key_sync_frequency", "1440")
        createSyncJobs(false)
    }

    private fun goNext() {
        startActivity<SettingsActivity>()
        finish()
    }

    private fun showError(message: String?) {
        // TODO
    }
}

sealed class Request {
    class Access(val code: String) : Request()
    class Self(val accessToken: String) : Request()
    class CreateAccount(val refreshToken: String, val name: String) : Request()
    class Calendar(val accessToken: String) : Request()
}

data class ResponseModel<out T>(val inProgress: Boolean = false,
                                val success: Boolean = false,
                                val response: T? = null,
                                val error: ErrorResponse? = null) {

    companion object {
        fun <T> inProgress() = ResponseModel<T>(inProgress = true)
        fun <T> success(response: T) = ResponseModel(success = true, response = response)
        fun <T> failure(error: ErrorResponse?) = ResponseModel<T>(error = error)
    }
}
