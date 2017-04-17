package eu.the4thfloor.msync.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import dagger.Module
import dagger.Provides
import org.threeten.bp.LocalDate
import java.io.IOException
import javax.inject.Singleton

@Module
class GsonModule {

    @Provides
    @Singleton
    internal fun provideGson(): Gson {

        return GsonBuilder()
            .registerTypeAdapter(LocalDate::class.java, LocalDateTypeAdapter())
            .create()
    }

    private class LocalDateTypeAdapter internal constructor() : TypeAdapter<LocalDate>() {

        @Throws(IOException::class)
        override fun read(`in`: JsonReader): LocalDate {
            return LocalDate.parse(`in`.nextString())
        }

        @Throws(IOException::class)
        override fun write(out: JsonWriter,
                           value: LocalDate) {

        }
    }
}
