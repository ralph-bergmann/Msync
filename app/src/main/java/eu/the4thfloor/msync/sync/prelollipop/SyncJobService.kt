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


import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import eu.the4thfloor.msync.MSyncApp
import eu.the4thfloor.msync.api.MeetupApi
import eu.the4thfloor.msync.api.SecureApi
import eu.the4thfloor.msync.sync.sync
import javax.inject.Inject

class SyncJobService : JobService() {

    @Inject lateinit var secureApi: SecureApi
    @Inject lateinit var meetupApi: MeetupApi

    override fun onCreate() {
        super.onCreate()
        MSyncApp.graph.inject(this)
    }

    override fun onStartJob(job: JobParameters): Boolean {
        sync(secureApi, meetupApi, applicationContext) {
            jobFinished(job, false)
        }
        return true
    }

    override fun onStopJob(job: JobParameters): Boolean = true
}
