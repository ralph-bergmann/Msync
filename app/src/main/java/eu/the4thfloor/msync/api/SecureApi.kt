package eu.the4thfloor.msync.api

import eu.the4thfloor.msync.api.models.AccessResponse
import io.reactivex.Flowable
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Headers
import retrofit2.http.POST

interface SecureApi {


    @POST("access")
    @Headers("Accept: application/json")
    @FormUrlEncoded
    fun access(@FieldMap(encoded = true) params: Map<String, String>): Flowable<AccessResponse>
}
