package eu.the4thfloor.msync.sync.lollipop


import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.support.annotation.RequiresApi
import timber.log.Timber
import java.util.concurrent.TimeUnit.HOURS

private val JOB_ID_INIT = 1
private val JOB_ID_SYNC = 2

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun Context.createSyncJobs(init: Boolean) {
    componentName.let { component ->
        jobScheduler.let { scheduler ->
            if (init) {
                scheduler.createAndScheduleInitJob(component)
            } else {
                scheduler.createAndScheduleSyncJob(component)
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
private fun JobScheduler.createAndScheduleSyncJob(componentName: ComponentName) {
    Timber.i("createAndScheduleSyncJob")
    val periodicity = HOURS.toMillis(2)
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
