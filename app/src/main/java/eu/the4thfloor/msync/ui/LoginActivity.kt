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
import eu.the4thfloor.msync.BuildConfig.MEETUP_OAUTH_KEY
import eu.the4thfloor.msync.BuildConfig.MEETUP_OAUTH_REDIRECT_URI
import eu.the4thfloor.msync.BuildConfig.MEETUP_OAUTH_SECRET
import eu.the4thfloor.msync.MSyncApp
import eu.the4thfloor.msync.R
import eu.the4thfloor.msync.api.MeetupApi
import eu.the4thfloor.msync.api.SecureApi
import eu.the4thfloor.msync.utils.checkSelfPermission
import eu.the4thfloor.msync.utils.createAccount
import eu.the4thfloor.msync.utils.createCalendar
import eu.the4thfloor.msync.utils.createSyncJobs
import eu.the4thfloor.msync.utils.doFromSdk
import eu.the4thfloor.msync.utils.getCalendarID
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.startActivity
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult
import timber.log.Timber
import javax.inject.Inject

class LoginActivity : AccountAuthenticatorActivity() {

    @Inject lateinit var secureApi: SecureApi
    @Inject lateinit var meetupApi: MeetupApi

    public override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        MSyncApp.graph.inject(this)
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
                }
            }
        }
    }

    private fun loadToken(code: String) {
        Timber.i("code: %s", code)
        launch(CommonPool) {
            val result = secureApi.access(mapOf("client_id" to MEETUP_OAUTH_KEY,
                                                "client_secret" to MEETUP_OAUTH_SECRET,
                                                "redirect_uri" to MEETUP_OAUTH_REDIRECT_URI,
                                                "code" to code,
                                                "grant_type" to "authorization_code")).awaitResult()
            launch(UI) {
                when (result) {
                    is Result.Ok        -> loadSelf(result.value.access_token, result.value.refresh_token)
                    is Result.Error     -> Timber.e(result.exception, "failed to load 'secureApi.access' - code: ${result.exception.code()}")
                    is Result.Exception -> Timber.e(result.exception, "Something broken")
                }
            }
        }
    }

    private fun loadSelf(accessToken: String, refreshToken: String) {
        Timber.i("accessToken: %s", accessToken)
        launch(CommonPool) {
            val result = meetupApi.self("Bearer $accessToken").awaitResult()
            launch(UI) {
                when (result) {
                    is Result.Ok        -> createAccountCalendarAndSyncJob(result.value.name, refreshToken)
                    is Result.Error     -> Timber.e(result.exception, "failed to load 'meetupApi.self' - code: ${result.exception.code()}")
                    is Result.Exception -> Timber.e(result.exception, "Something broken")
                }
            }
        }
    }

    private fun createAccountCalendarAndSyncJob(name: String, accessToken: String) {
        Timber.i("name: %s - accessToken: %s", name, accessToken)
        createAccount(name, accessToken)
        doFromSdk(Build.VERSION_CODES.M,
                  {
                      if (checkSelfPermission(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                          requestPermissions(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR), 0)
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
        createSyncJobs(false)
    }

    private fun goNext() {
        startActivity<SettingsActivity>()
        finish()
    }
}
