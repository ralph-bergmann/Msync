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

package eu.the4thfloor.msync.sync.lollipop


import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import org.jetbrains.anko.defaultSharedPreferences
import timber.log.Timber
import java.util.concurrent.TimeUnit

private const val JOB_ID_INIT = 1
private const val JOB_ID_SYNC = 2

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun Context.createSyncJobs(init: Boolean) {
    componentName.let { component ->
        jobScheduler.let { scheduler ->
            if (init) {
                scheduler.createAndScheduleInitJob(component)
            } else {
                val syncFrequency = defaultSharedPreferences.getString("pref_key_sync_frequency", "1440")!!.toLong()
                scheduler.createAndScheduleSyncJob(component, syncFrequency)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
private fun JobScheduler.createAndScheduleInitJob(componentName: ComponentName) {
    Timber.i("createAndScheduleInitJob")
    schedule(JobInfo.Builder(JOB_ID_INIT, componentName)
                 .setPersisted(false)
                 .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                 .build())
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
private fun JobScheduler.createAndScheduleSyncJob(componentName: ComponentName, syncFrequency: Long) {
    Timber.i("createAndScheduleSyncJob")
    val periodicity = TimeUnit.MINUTES.toMillis(syncFrequency)
    schedule(JobInfo.Builder(JOB_ID_SYNC, componentName)
                 .setPersisted(true)
                 .setPeriodic(periodicity)
                 .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                 .build())
}

private val Context.componentName
    get() = ComponentName(this, SyncJobService::class.java)

private val Context.jobScheduler: JobScheduler
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    get() = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
