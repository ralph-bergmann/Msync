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


open class Response

/**
 * {
 * ... "access_token":"ACCESS_TOKEN_TO_STORE",
 * ... "token_type":"bearer",
 * ... "expires_in":3600,
 * ... "refresh_token":"TOKEN_USED_TO_REFRESH_AUTHORIZATION"
 * }
 */
class AccessResponse : Response() {
    var access_token: String? = null
    var token_type: String? = null
    var expires_in: Long? = null
    var refresh_token: String? = null

    override fun toString(): String =
        "AccessResponse(access_token=$access_token, token_type=$token_type, expires_in=$expires_in, refresh_token=$refresh_token)"
}

/**
 * {
 * ... "id": 123,
 * ... "name": "Bobby Tables"
 * }
 */
class SelfResponse : Response() {
    var id: Long? = null
    var name: String? = null

    override fun toString(): String = "SelfResponse(id=$id, name=$name)"
}

class CalendarResponse(val events: List<Event>) : Response()

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
class Event {
    var id: String? = null
    var name: String? = null
    var plain_text_description: String? = null
    var link: String? = null
    var time: Long? = null
    var utc_offset: Int? = null
    var duration: Long? = null
    var updated: Long? = null
    var venue: EventVenue? = null
    var group: EventGroup? = null
    var self: EventSelf? = null

    override fun toString(): String = "Event(id=$id, name=$name, self=$self)"
}

class EventVenue {
    var id: String? = null
    var name: String? = null
    var address_1: String? = null
    var address_2: String? = null
    var address_3: String? = null
    var city: String? = null
    var localized_country_name: String? = null

    override fun toString(): String = "EventVenue(id=$id, name=$name)"
}

class EventGroup {
    var id: String? = null
    var name: String? = null

    override fun toString(): String = "EventGroup(id=$id, name=$name)"
}

class EventSelf {
    var rsvp: EventRsvp? = null

    override fun toString(): String = "EventSelf(rsvp=$rsvp)"
}

class EventRsvp {
    var response: Rsvp? = null

    override fun toString(): String = "EventRsvp(response=$response)"
}

enum class Rsvp(val value: Int) {
    yes(ATTENDEE_STATUS_ACCEPTED),
    yes_pending_payment(ATTENDEE_STATUS_ACCEPTED),
    no(ATTENDEE_STATUS_DECLINED),
    waitlist(ATTENDEE_STATUS_TENTATIVE),
    notanswered(ATTENDEE_STATUS_INVITED)
}

class CreateAccountResponse() : Response()

/**
 * {
 * ... "error_description": "Invalid code",
 * ... "error": "invalid_grant"
 * }
 */
class ErrorResponse {
    var error_description: String? = null
    var error: String? = null

    override fun toString(): String =
        "ErrorResponse(error_description=$error_description, error=$error)"
}
