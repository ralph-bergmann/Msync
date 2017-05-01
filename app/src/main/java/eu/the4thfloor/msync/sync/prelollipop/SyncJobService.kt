package eu.the4thfloor.msync.sync.prelollipop


import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import eu.the4thfloor.msync.MSyncApp
import eu.the4thfloor.msync.api.MeetupApi
import eu.the4thfloor.msync.api.SecureApi
import eu.the4thfloor.msync.sync.sync
import eu.the4thfloor.msync.utils.getRefreshToken
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

class SyncJobService : JobService() {

    @Inject lateinit var secureApi: SecureApi
    @Inject lateinit var meetupApi: MeetupApi
    lateinit var disposables: CompositeDisposable

    override fun onCreate() {
        super.onCreate()
        MSyncApp.graph.inject(this)
        disposables = CompositeDisposable()
    }

    override fun onStartJob(job: JobParameters): Boolean {
        sync(secureApi, meetupApi, getRefreshToken(), disposables, applicationContext) {
            jobFinished(job, false)
        }
        return true
    }

    override fun onStopJob(job: JobParameters): Boolean {
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }
}
