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
import android.os.Bundle
import eu.the4thfloor.msync.BuildConfig
import eu.the4thfloor.msync.BuildConfig.PREF_ACTION_SHARE
import eu.the4thfloor.msync.BuildConfig.PREF_ACTION_SYNC_NOW
import eu.the4thfloor.msync.MSyncApp
import eu.the4thfloor.msync.utils.createSyncJobs


class PrefActionsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MSyncApp.graph.inject(this)

        intent?.dataString?.let {
            if (PREF_ACTION_SYNC_NOW.equals(it, ignoreCase = true)) {
                createSyncJobs(true)
            } else if (PREF_ACTION_SHARE.equals(it, ignoreCase = true)) {
                startActivity(
                    Intent()
                        .apply {
                            setType("text/plain")
                            putExtra(Intent.EXTRA_TEXT, BuildConfig.PLAYSTORE_LINK)
                        })
            }
        }

        finish()
    }
}
