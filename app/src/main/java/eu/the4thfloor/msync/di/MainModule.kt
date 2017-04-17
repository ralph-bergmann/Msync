package eu.the4thfloor.msync.di

import dagger.Module
import dagger.Provides
import eu.the4thfloor.msync.MSyncApp
import javax.inject.Singleton

@Module(includes = arrayOf(NetworkModule::class, FirebaseModule::class))
class MainModule(private val app: MSyncApp) {

    @Provides
    @Singleton
    internal fun provideApplication(): MSyncApp {
        return app
    }
}
