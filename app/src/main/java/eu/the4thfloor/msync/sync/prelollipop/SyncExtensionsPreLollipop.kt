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

package eu.the4thfloor.msync.sync.prelollipop


import android.content.Context
import com.firebase.jobdispatcher.Constraint
import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.firebase.jobdispatcher.GooglePlayDriver
import com.firebase.jobdispatcher.Lifetime
import com.firebase.jobdispatcher.Trigger
import org.jetbrains.anko.defaultSharedPreferences
import java.util.concurrent.TimeUnit.MINUTES


private val TAG = SyncJobService::class.java.simpleName

fun Context.createSyncJobs(init: Boolean) {
    FirebaseJobDispatcher(GooglePlayDriver(applicationContext)).let {
        if (init) {
            it.createAndScheduleInitJob()
        } else {
            val syncFrequency = defaultSharedPreferences.getString("pref_key_sync_frequency", "1440").toLong()
            it.createAndScheduleSyncJob(syncFrequency)
        }
    }
}

private fun FirebaseJobDispatcher.createAndScheduleInitJob() {
    schedule(newJobBuilder()
                 .setService(SyncJobService::class.java)
                 .setTag(TAG + "-INIT")
                 .setLifetime(Lifetime.UNTIL_NEXT_BOOT)
                 .setTrigger(Trigger.executionWindow(0, 0))
                 .setConstraints(Constraint.ON_ANY_NETWORK)
                 .build())
}

private fun FirebaseJobDispatcher.createAndScheduleSyncJob(syncFrequency: Long) {
    val periodicity = MINUTES.toSeconds(syncFrequency).toInt()
    val toleranceInterval = MINUTES.toSeconds(syncFrequency / 2).toInt()
    schedule(newJobBuilder()
                 .setService(SyncJobService::class.java)
                 .setTag(TAG + "-SYNC")
                 .setLifetime(Lifetime.FOREVER)
                 .setTrigger(Trigger.executionWindow(periodicity,
                                                     periodicity + toleranceInterval))
                 .setRecurring(true)
                 .setConstraints(Constraint.ON_ANY_NETWORK)
                 .setReplaceCurrent(true)
                 .build())
}
