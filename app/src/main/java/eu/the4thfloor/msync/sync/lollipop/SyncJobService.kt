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


import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build
import android.support.annotation.RequiresApi
import eu.the4thfloor.msync.MSyncApp
import eu.the4thfloor.msync.api.MeetupApi
import eu.the4thfloor.msync.api.SecureApi
import eu.the4thfloor.msync.sync.sync
import eu.the4thfloor.msync.utils.getRefreshToken
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject


@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class SyncJobService : JobService() {

    @Inject lateinit var secureApi: SecureApi
    @Inject lateinit var meetupApi: MeetupApi
    lateinit var disposables: CompositeDisposable

    override fun onCreate() {
        super.onCreate()
        MSyncApp.graph.inject(this)
        disposables = CompositeDisposable()
    }

    override fun onStartJob(params: JobParameters): Boolean {
        sync(secureApi, meetupApi, getRefreshToken(), disposables, applicationContext) {
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }
}
