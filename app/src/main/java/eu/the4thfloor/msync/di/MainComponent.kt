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

package eu.the4thfloor.msync.di

import dagger.Component
import eu.the4thfloor.msync.ui.LoginActivity
import eu.the4thfloor.msync.ui.PrefActionsActivity
import eu.the4thfloor.msync.ui.SettingsActivity
import javax.inject.Singleton
import eu.the4thfloor.msync.sync.lollipop.SyncJobService as SyncJobServiceLollipop
import eu.the4thfloor.msync.sync.prelollipop.SyncJobService as SyncJobServicePreLollipop

@Singleton
@Component(modules = [(MainModule::class)])
interface MainComponent {

    fun inject(loginActivity: LoginActivity)
    fun inject(settingsActivity: SettingsActivity)
    fun inject(prefActionsActivity: PrefActionsActivity)
    fun inject(syncJobService: SyncJobServiceLollipop)
    fun inject(syncJobService: SyncJobServicePreLollipop)

    object Initializer {

        fun init(mainModule: MainModule): MainComponent =
            DaggerMainComponent.builder().mainModule(mainModule).build()
    }
}
