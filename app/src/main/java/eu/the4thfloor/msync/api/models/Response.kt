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

package eu.the4thfloor.msync.api.models

import android.provider.CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED
import android.provider.CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED
import android.provider.CalendarContract.Attendees.ATTENDEE_STATUS_INVITED
import android.provider.CalendarContract.Attendees.ATTENDEE_STATUS_TENTATIVE


/**
 * {
 * ... "access_token":"ACCESS_TOKEN_TO_STORE",
 * ... "token_type":"bearer",
 * ... "expires_in":3600,
 * ... "refresh_token":"TOKEN_USED_TO_REFRESH_AUTHORIZATION"
 * }
 */
data class AccessResponse(val access_token: String,
                          val token_type: String,
                          val expires_in: Long,
                          val refresh_token: String)

/**
 * {
 * ... "id": 123,
 * ... "name": "Bobby Tables"
 * }
 */
data class SelfResponse(val id: Long,
                        val name: String)

data class CalendarResponse(val events: List<Event>)

/**
 * [
 * ... {
 * ...... "plain_text_description": "Talk 1: Conve ... PhD in Applied Mathematics.",
 * ...... "self": {
 * ......... "actions": [
 * ............ "upload_photo",
 * ............ "comment",
 * ............ "rsvp"
 * ......... ],
 * ......... "rsvp": {
 * ............ "response": "yes",
 * ............ "guests": 0
 * ......... }
 * ...... },
 * ...... "link": "https://www.meetup.com/berlin-deep-learning/events/239325485/",
 * ...... "venue": {
 * ......... "id": 24434712,
 * ......... "name": "SAP",
 * ......... "lat": 52.52587127685547,
 * ......... "lon": 13.403037071228027,
 * ......... "repinned": false,
 * ......... "address_1": "Rosenthaler Str. 30, 10178 Berlin",
 * ......... "city": "Berlin",
 * ......... "country": "de",
 * ......... "localized_country_name": "Germany"
 * ...... },
 * ...... "group": {
 * ......... "created": 1461339963000,
 * ......... "name": "Berlin Outdoor Events",
 * ......... "id": 19870711,
 * ......... "join_mode": "open",
 * ......... "lat": 52.52000045776367,
 * ......... "lon": 13.380000114440918,
 * ......... "urlname": "Berlin-Outdoor-Events",
 * ......... "who": "Members"
 * ...... },
 * ...... "updated": 1492617561000,
 * ...... "duration": 10800000,
 * ...... "time": 1493830800000,
 * ...... "utc_offset": 7200000,
 * ...... "name": "Natural Language Understanding",
 * ...... "id": "239325485"
 * ... }
 * ]
 */
data class Event(val id: String,
                 val name: String,
                 val plain_text_description: String?,
                 val link: String,
                 val time: Long,
                 val utc_offset: Int,
                 val duration: Long?,
                 val updated: Long,
                 val venue: EventVenue?,
                 val group: EventGroup,
                 val self: EventSelf)

data class EventVenue(val id: String,
                      val name: String,
                      val address_1: String,
                      val address_2: String?,
                      val address_3: String?,
                      val city: String,
                      val localized_country_name: String)

data class EventGroup(val id: String,
                      val name: String)

data class EventSelf(var rsvp: EventRsvp?)

data class EventRsvp(var response: Rsvp)

enum class Rsvp(val value: Int) {
    yes(ATTENDEE_STATUS_ACCEPTED),
    yes_pending_payment(ATTENDEE_STATUS_ACCEPTED),
    no(ATTENDEE_STATUS_DECLINED),
    waitlist(ATTENDEE_STATUS_TENTATIVE),
    notanswered(ATTENDEE_STATUS_INVITED)
}
