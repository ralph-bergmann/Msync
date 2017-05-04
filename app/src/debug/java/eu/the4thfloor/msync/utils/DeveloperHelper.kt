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

package eu.the4thfloor.msync.utils

import android.app.Application
import com.facebook.stetho.Stetho
import com.facebook.stetho.okhttp3.StethoInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

fun Application.enableStetho() = Stetho.initializeWithDefaults(this)

fun OkHttpClient.Builder.enableStetho() = addNetworkInterceptor(StethoInterceptor())

fun OkHttpClient.Builder.enableLogging() {
    val logging = HttpLoggingInterceptor()
    logging.level = HttpLoggingInterceptor.Level.BASIC
    addNetworkInterceptor(logging)
}
