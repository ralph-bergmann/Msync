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

package eu.the4thfloor.msync

import android.app.Application
import android.os.StrictMode
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import eu.the4thfloor.msync.api.models.Event
import eu.the4thfloor.msync.api.models.Rsvp
import eu.the4thfloor.msync.di.MainComponent
import eu.the4thfloor.msync.di.MainModule
import eu.the4thfloor.msync.utils.DebugTree
import eu.the4thfloor.msync.utils.enableStetho
import okio.BufferedSource
import okio.Okio
import timber.log.Timber
import java.util.*


class MSyncApp : Application() {

    var refWatcher: RefWatcher? = null
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return
        }
        refWatcher = LeakCanary.install(this)

        if (BuildConfig.DEBUG) {

            // Timber

            Timber.plant(DebugTree("msync"))


            // StrictMode

            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectNetwork().detectCustomSlowCalls().penaltyLog().build())
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build())


            // stetho

            enableStetho()
        }

        test()
    }

    private fun test() {
        val input = assets.open("calendar.json")
        val buffer = Okio.buffer(Okio.source(input))
        val list = foo(buffer)
        list
            .filter { item -> item.self.rsvp?.response == Rsvp.yes }
            .forEach { item -> Timber.i("${item.name} - ${Date(item.time)}") }
    }

    private fun foo(buffer: BufferedSource): List<Event> {
        val listMyData = Types.newParameterizedType(List::class.java, Event::class.java)
        val adapter = Moshi.Builder().build().adapter<List<Event>>(listMyData)
        return adapter.fromJson(buffer)
    }

    companion object {

        var instance: MSyncApp? = null
            private set

        val graph: MainComponent by lazy {
            MainComponent.Initializer.init(MainModule(instance!!))
        }
    }
}
