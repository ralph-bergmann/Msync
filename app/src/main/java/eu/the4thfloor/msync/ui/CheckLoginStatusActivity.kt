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
import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsCallback
import android.support.customtabs.CustomTabsClient
import android.support.customtabs.CustomTabsIntent
import android.support.customtabs.CustomTabsServiceConnection
import eu.the4thfloor.msync.BuildConfig
import eu.the4thfloor.msync.R
import eu.the4thfloor.msync.utils.CustomTabsHelper
import eu.the4thfloor.msync.utils.hasAccount
import org.jetbrains.anko.startActivity
import java.lang.ref.WeakReference

class CheckLoginStatusActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (hasAccount()) {
            startActivity<SettingsActivity>()
            finish()
        } else {
            loadCustomTab()
        }
    }

    private fun loadCustomTab() {
        val uri = Uri.Builder()
            .scheme("https")
            .authority("secure.meetup.com")
            .appendPath("oauth2")
            .appendPath("authorize")
            .appendQueryParameter("client_id", BuildConfig.MEETUP_OAUTH_KEY)
            .appendQueryParameter("redirect_uri", BuildConfig.MEETUP_OAUTH_REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("set_mobile", "on")
            .build()

        CustomTabsHelper.getPackageNameToUse(this, uri)?.let {
            CustomTabsClient.bindCustomTabsService(this, it, Connection(this, uri, it))
        }
    }

    private class Connection(activity: CheckLoginStatusActivity,
                             private val uri: Uri,
                             private val packageName: String,
                             private val ref: WeakReference<CheckLoginStatusActivity> = WeakReference(activity)) : CustomTabsServiceConnection() {

        override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
            ref.get()?.let {
                val session = client.newSession(Callback(it))
                CustomTabsIntent.Builder(session)
                    .apply {
                        setToolbarColor(it.resources.getColor(R.color.red))
                        enableUrlBarHiding()
                    }
                    .build()
                    .apply {
                        intent.`package` = packageName
                    }
                    .launchUrl(it, uri)
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {

        }
    }

    private class Callback(activity: CheckLoginStatusActivity,
                           private val ref: WeakReference<CheckLoginStatusActivity> = WeakReference(activity)) : CustomTabsCallback() {

        override fun onNavigationEvent(navigationEvent: Int, extras: Bundle?) {
            super.onNavigationEvent(navigationEvent, extras)
            when (navigationEvent) {
                TAB_HIDDEN -> {
                    ref.get()?.finish()
                }
            }
        }
    }
}
