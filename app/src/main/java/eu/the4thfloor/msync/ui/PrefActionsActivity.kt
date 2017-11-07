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

import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import eu.the4thfloor.msync.BuildConfig
import eu.the4thfloor.msync.BuildConfig.GITHUB_LINK
import eu.the4thfloor.msync.BuildConfig.PLAYSTORE_LINK
import eu.the4thfloor.msync.BuildConfig.PREF_ACTION_DEBUG
import eu.the4thfloor.msync.BuildConfig.PREF_ACTION_GITHUB
import eu.the4thfloor.msync.BuildConfig.PREF_ACTION_LOGOUT
import eu.the4thfloor.msync.BuildConfig.PREF_ACTION_SHARE
import eu.the4thfloor.msync.BuildConfig.PREF_ACTION_SYNC_NOW
import eu.the4thfloor.msync.MSyncApp
import eu.the4thfloor.msync.utils.createSyncJobs
import eu.the4thfloor.msync.utils.deleteAccount
import eu.the4thfloor.msync.utils.getRefreshToken
import org.jetbrains.anko.browse
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.email
import java.util.*


class PrefActionsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MSyncApp.graph.inject(this)

        when (intent?.dataString) {
            PREF_ACTION_SYNC_NOW -> createSyncJobs(true)
            PREF_ACTION_SHARE    -> startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND)
                                                                           .apply {
                                                                               type = "text/plain"
                                                                               putExtra(Intent.EXTRA_TEXT, PLAYSTORE_LINK)
                                                                           },
                                                                       null))
            PREF_ACTION_LOGOUT   -> {
                deleteAccount()
                finishAffinity()
            }
            PREF_ACTION_DEBUG    -> sendDebugInfos()
            PREF_ACTION_GITHUB   -> browse(GITHUB_LINK, true)
            else                 -> { }
        }

        finish()
    }

    private fun sendDebugInfos() =
        email("ralph@the4thfloor.eu",
              "Msync Debug Infos",
              "Android Version: ${Build.VERSION.RELEASE}\n" +
              "Android Api Level: ${Build.VERSION.SDK_INT}\n" +
              "Device: ${Build.MODEL}\n" +
              "Manufacturer: ${Build.MANUFACTURER}\n" +
              "System Language: ${Resources.getSystem().configuration.locale.language}\n" +
              "App Language: ${Locale.getDefault().language}\n" +
              "App Version: ${BuildConfig.VERSION_NAME}\n" +
              "Git commit: ${BuildConfig.GIT_SHA}\n" +
              "Sync yes: ${defaultSharedPreferences.getBoolean("pref_key_sync_event_yes", true)}\n" +
              "Sync waitlist: ${defaultSharedPreferences.getBoolean("pref_key_sync_event_waitlist", true)}\n" +
              "Sync unanswered: ${defaultSharedPreferences.getBoolean("pref_key_sync_event_unanswered", false)}\n" +
              "Sync no: ${defaultSharedPreferences.getBoolean("pref_key_sync_event_no", false)}\n" +
              "Sync frequency: ${defaultSharedPreferences.getString("pref_key_sync_frequency", "1440")}\n" +
              "Token: ${getRefreshToken()}")
}
