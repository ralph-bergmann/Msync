package eu.the4thfloor.msync.utils

import android.app.Application
import okhttp3.OkHttpClient

fun Application.enableStetho() { }

fun OkHttpClient.Builder.enableStetho() { }

fun OkHttpClient.Builder.enableLogging() { }
