package eu.the4thfloor.msync.di

import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.firebase.jobdispatcher.GooglePlayDriver
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.Module
import dagger.Provides
import eu.the4thfloor.msync.MSyncApp
import javax.inject.Singleton

@Module
class FirebaseModule {

    @Provides
    @Singleton
    internal fun provideFirebaseAnalytics(app: MSyncApp): FirebaseAnalytics {
        return FirebaseAnalytics.getInstance(app)
    }

    @Provides
    @Singleton
    internal fun provideFirebaseJobDispatcher(app: MSyncApp): FirebaseJobDispatcher {
        return FirebaseJobDispatcher(GooglePlayDriver(app))
    }
}
