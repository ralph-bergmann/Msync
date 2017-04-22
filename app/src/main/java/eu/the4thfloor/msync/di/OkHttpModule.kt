package eu.the4thfloor.msync.di

import dagger.Module
import dagger.Provides
import eu.the4thfloor.msync.BuildConfig
import eu.the4thfloor.msync.MSyncApp
import eu.the4thfloor.msync.utils.enableLogging
import eu.the4thfloor.msync.utils.enableStetho
import okhttp3.Cache
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Singleton

@Module
class OkHttpModule {

    private val OKHTTP_CLIENT_CACHE_SIZE = 100L * 1024L * 1024L // 100 MiB for images

    @Provides
    @Singleton
    internal fun provideOkHttpClient(app: MSyncApp): OkHttpClient {

        val cacheDirectory = File(app.cacheDir.absoluteFile, "http")
        val cache = Cache(cacheDirectory, OKHTTP_CLIENT_CACHE_SIZE)
        val builder = OkHttpClient.Builder()
        builder.cache(cache)

        if (BuildConfig.DEBUG) {
            builder.enableLogging()
            builder.enableStetho()
        }

        val certificatePinner = CertificatePinner.Builder()
            .add("secure.meetup.com", "sha256/dvShW4Mh3UYqIPjIrA7pExa9WkTXG3+g2Bws6R8NYu0=") // CN=f2.shared.global.fastly.net,O=Fastly\, Inc.,L=San Francisco,ST=California,C=US
            .add("secure.meetup.com", "sha256/+VZJxHgrOOiVyUxgMRbfoo+GIWrMKd4aellBBHtBcKg=") // CN=GlobalSign CloudSSL CA - SHA256 - G3,O=GlobalSign nv-sa,C=BE
            .add("secure.meetup.com", "sha256/K87oWBWM9UZfyddvDfoxL+8lpNyoUB2ptGtn0fv6G2Q=") // CN=GlobalSign Root CA,OU=Root CA,O=GlobalSign nv-sa,C=BE
            .build()
        builder.certificatePinner(certificatePinner)

        return builder.build()
    }
}
