package eu.the4thfloor.msync.api

import eu.the4thfloor.msync.api.models.AccessResponse
import io.reactivex.Observable
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface MeetupApi {

    @POST("access")
    @FormUrlEncoded
    fun access(@FieldMap(encoded = true) params: Map<String, String>): Observable<AccessResponse>
}
