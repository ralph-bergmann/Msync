package eu.the4thfloor.msync.di


import android.Manifest.permission.WAKE_LOCK
import android.support.annotation.RequiresPermission
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.Module
import dagger.Provides
import eu.the4thfloor.msync.MSyncApp
import javax.inject.Singleton

@Module
class FirebaseModule {

    @Provides
    @Singleton
    @RequiresPermission(anyOf = arrayOf(WAKE_LOCK))
    internal fun provideFirebaseAnalytics(app: MSyncApp): FirebaseAnalytics {
        return FirebaseAnalytics.getInstance(app)
    }
}
