package eu.the4thfloor.msync.di

import dagger.Component
import eu.the4thfloor.msync.ui.LoginActivity
import eu.the4thfloor.msync.ui.PrefActionsActivity
import javax.inject.Singleton
import eu.the4thfloor.msync.sync.lollipop.SyncJobService as SyncJobServiceLollipop
import eu.the4thfloor.msync.sync.prelollipop.SyncJobService as SyncJobServicePreLollipop

@Singleton
@Component(modules = arrayOf(MainModule::class))
interface MainComponent {

    fun inject(loginActivity: LoginActivity)
    fun inject(prefActionsActivity: PrefActionsActivity)
    fun inject(syncJobService: SyncJobServiceLollipop)
    fun inject(syncJobService: SyncJobServicePreLollipop)

    object Initializer {

        fun init(mainModule: MainModule): MainComponent {
            return DaggerMainComponent.builder().mainModule(mainModule).build()
        }
    }
}
