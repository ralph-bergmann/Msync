package eu.the4thfloor.msync

import android.app.Application
import android.os.StrictMode
import com.jakewharton.threetenabp.AndroidThreeTen
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import eu.the4thfloor.msync.di.MainComponent
import eu.the4thfloor.msync.di.MainModule
import eu.the4thfloor.msync.utils.DebugTree
import eu.the4thfloor.msync.utils.enableStetho
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

        if (BuildConfig.DEBUG) {

            // Timber

            Timber.plant(DebugTree("msync"))


            // StrictMode

            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectNetwork().detectCustomSlowCalls().penaltyLog().build())
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build())


            // stetho

            enableStetho()
        }
        AndroidThreeTen.init(this)
    }

    companion object {

        var instance: MSyncApp? = null
            private set

        val graph: MainComponent by lazy {
            MainComponent.Initializer.init(MainModule(instance!!))
        }
    }
}
