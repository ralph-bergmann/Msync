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
