package eu.the4thfloor.msync.api

import eu.the4thfloor.msync.api.models.SelfResponse
import io.reactivex.Flowable
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers

interface MeetupApi {


    @GET("2/member/self/")
    @Headers("Accept: application/json")
    fun self(@Header("Authorization") access_token: String): Flowable<SelfResponse>
}
