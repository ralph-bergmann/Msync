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
import java.util.concurrent.TimeUnit.HOURS


private val TAG = SyncJobService::class.java.simpleName

fun Context.createSyncJobs(init: Boolean) {
    FirebaseJobDispatcher(GooglePlayDriver(applicationContext)).let {
        if (init) {
            it.createAndScheduleInitJob()
        } else {
            it.createAndScheduleSyncJob()
        }
    }
}

private fun FirebaseJobDispatcher.createAndScheduleInitJob() {
    schedule(newJobBuilder()
                 .setService(SyncJobService::class.java)
                 .setTag(TAG + "-INIT")
                 .setTrigger(Trigger.executionWindow(0, 0))
                 .setConstraints(Constraint.ON_ANY_NETWORK)
                 .build())
}

private fun FirebaseJobDispatcher.createAndScheduleSyncJob() {
    val periodicity = HOURS.toSeconds(2).toInt()
    val toleranceInterval = HOURS.toSeconds(1).toInt()
    schedule(newJobBuilder()
                 .setService(SyncJobService::class.java)
                 .setTag(TAG + "-SYNC")
                 .setLifetime(Lifetime.FOREVER)
                 .setTrigger(Trigger.executionWindow(periodicity,
                                                     periodicity + toleranceInterval))
                 .setRecurring(true)
                 .setConstraints(Constraint.ON_ANY_NETWORK)
                 .build())
}
