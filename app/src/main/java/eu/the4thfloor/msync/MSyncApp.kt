package eu.the4thfloor.msync

import android.app.Application

import com.facebook.stetho.Stetho
import com.jakewharton.threetenabp.AndroidThreeTen
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher

import eu.the4thfloor.msync.di.MainComponent
import eu.the4thfloor.msync.di.MainModule
import eu.the4thfloor.msync.utils.DebugTree
import timber.log.Timber


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

        graph = MainComponent.Initializer.init(MainModule(this))

        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree("msync"))
            Stetho.initializeWithDefaults(this)
        }
        AndroidThreeTen.init(this)
    }

    companion object {

        var instance: MSyncApp? = null
            private set

        var graph: MainComponent? = null
            private set
    }
}
