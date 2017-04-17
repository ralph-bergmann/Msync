package eu.the4thfloor.msync.di

import com.google.gson.Gson
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import dagger.Module
import dagger.Provides
import eu.the4thfloor.msync.api.MeetupApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module(includes = arrayOf(GsonModule::class, OkHttpModule::class))
class NetworkModule {

    private val BASE_URL = "https://secure.meetup.com/oauth2/"

    @Provides
    @Singleton
    internal fun provideApi(gson: Gson, client: OkHttpClient): MeetupApi {

        val apiClient = client
            .newBuilder()
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .client(apiClient)
            .build()
            .create(MeetupApi::class.java)
    }
}
