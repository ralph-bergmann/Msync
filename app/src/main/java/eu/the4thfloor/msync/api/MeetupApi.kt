package eu.the4thfloor.msync.api

import eu.the4thfloor.msync.api.models.Event
import eu.the4thfloor.msync.api.models.SelfResponse
import io.reactivex.Flowable
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Query

interface MeetupApi {


    @GET("2/member/self/")
    @Headers("Accept: application/json")
    fun self(@Header("Authorization") access_token: String): Flowable<SelfResponse>

    @GET("self/calendar")
    @Headers("Accept: application/json")
    fun calendar(@Header("Authorization") access_token: String,
               @Query("fields") fields: String,
               @Query("only") only: String): Flowable<List<Event>>
}
