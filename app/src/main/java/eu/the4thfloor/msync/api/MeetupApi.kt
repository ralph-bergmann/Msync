/*
 * Copyright 2017 Ralph Bergmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
