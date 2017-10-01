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
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.preference.ListPreference
import android.preference.Preference
import android.preference.PreferenceActivity
import android.preference.PreferenceManager
import android.support.annotation.RequiresApi
import eu.the4thfloor.msync.BuildConfig.BUILD_DATE
import eu.the4thfloor.msync.BuildConfig.GIT_SHA
import eu.the4thfloor.msync.BuildConfig.VERSION_NAME
import eu.the4thfloor.msync.MSyncApp
import eu.the4thfloor.msync.R
import eu.the4thfloor.msync.utils.checkSelfPermission
import eu.the4thfloor.msync.utils.createSyncJobs
import eu.the4thfloor.msync.utils.getAccount
import eu.the4thfloor.msync.utils.updateCalendarColor
import eu.the4thfloor.msync.utils.updateCalendarName
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.doFromSdk

class SettingsActivity : PreferenceActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MSyncApp.graph.inject(this)

        addPreferencesFromResource(R.xml.preferences)

        bindPreferenceSummaryToValue(findPreference("pref_key_calendar_name"))
        bindPreferenceSummaryToValue(findPreference("pref_key_sync_frequency"))
        bindPreferenceSummaryToValue(findPreference("pref_key_last_sync"))

        findPreference("pref_key_account").summary = getAccount()?.name
        findPreference("pref_key_version").summary = "$VERSION_NAME\nBuild Date: $BUILD_DATE\nGit Commit: $GIT_SHA"

        doFromSdk(Build.VERSION_CODES.M, { checkCalendarPermissions() })
    }

    override fun onStart() {
        super.onStart()
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onStop() {
        super.onStop()
        defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            "pref_key_calendar_name" -> {
                updateCalendarName()
            }
            "pref_key_sync_frequency" -> {
                createSyncJobs(false)
            }
            "pref_key_calendar_color" -> {
                updateCalendarColor()
            }
            "pref_key_last_sync" -> {
                findPreference("pref_key_last_sync").summary = sharedPreferences.getString(key, "")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkCalendarPermissions() {
        if (checkSelfPermission(Manifest.permission.READ_CALENDAR,
                                Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_CALENDAR,
                                       Manifest.permission.WRITE_CALENDAR), 0)
        }
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.

     * @see .sBindPreferenceSummaryToValueListener
     */
    private fun bindPreferenceSummaryToValue(preference: Preference) {
        // Set the listener to watch for value changes.
        preference.onPreferenceChangeListener = sBindPreferenceSummaryToValueListener

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener
            .onPreferenceChange(preference,
                                PreferenceManager
                                    .getDefaultSharedPreferences(preference.context)
                                    .getString(preference.key, ""))
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private val sBindPreferenceSummaryToValueListener = Preference.OnPreferenceChangeListener { preference, value ->
        val stringValue = value.toString()

        if (preference is ListPreference) {
            // For list preferences, look up the correct display value in
            // the preference's 'entries' list.
            val listPreference = preference
            val index = listPreference.findIndexOfValue(stringValue)

            // Set the summary to reflect the new value.
            preference.setSummary(
                if (index >= 0)
                    listPreference.entries[index]
                else
                    null)

        } else {
            // For all other preferences, set the summary to the value's
            // simple string representation.
            preference.summary = stringValue
        }
        true
    }
}
