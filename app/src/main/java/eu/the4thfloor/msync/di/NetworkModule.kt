package eu.the4thfloor.msync.di

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import dagger.Module
import dagger.Provides
import eu.the4thfloor.msync.api.MeetupApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module(includes = arrayOf(OkHttpModule::class))
class NetworkModule {

    private val BASE_URL = "https://secure.meetup.com/oauth2/"

    @Provides
    @Singleton
    internal fun provideRetrofit(client: OkHttpClient): Retrofit {

        val apiClient = client
            .newBuilder()
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .client(apiClient)
            .build()
    }

    @Provides
    @Singleton
    internal fun provideApi(retrofit: Retrofit): MeetupApi {
        return retrofit.create(MeetupApi::class.java)
    }
}
