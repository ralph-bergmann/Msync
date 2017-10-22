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

import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import eu.the4thfloor.msync.api.MeetupApi
import eu.the4thfloor.msync.api.SecureApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module(includes = [(OkHttpModule::class)])
class NetworkModule {

    private val SECURE_BASE_URL = "https://secure.meetup.com/oauth2/"
    private val API_BASE_URL = "https://api.meetup.com/"

    @Provides
    @Singleton
    internal fun provideMoshi(): Moshi = Moshi.Builder().build()

    @Provides
    @Singleton
    internal fun provideSecureApi(client: OkHttpClient, moshi: Moshi): SecureApi {

        return Retrofit.Builder()
            .baseUrl(SECURE_BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(client)
            .build()
            .create(SecureApi::class.java)
    }

    @Provides
    @Singleton
    internal fun provideMeetupApi(client: OkHttpClient, moshi: Moshi): MeetupApi {

        return Retrofit.Builder()
            .baseUrl(API_BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(client)
            .build()
            .create(MeetupApi::class.java)
    }
}
