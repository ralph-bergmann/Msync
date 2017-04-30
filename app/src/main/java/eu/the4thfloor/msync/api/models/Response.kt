package eu.the4thfloor.msync.api.models

import android.accounts.Account


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

    override fun toString(): String {
        return "AccessResponse(access_token=$access_token, token_type=$token_type, expires_in=$expires_in, refresh_token=$refresh_token)"
    }
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

    override fun toString(): String {
        return "SelfResponse(id=$id, name=$name)"
    }
}

class CreateAccountResponse(val account: Account) : Response()

/**
 * {
 * ... "error_description": "Invalid code",
 * ... "error": "invalid_grant"
 * }
 */
class ErrorResponse {
    var error_description: String? = null
    var error: String? = null

    override fun toString(): String {
        return "ErrorResponse(error_description=$error_description, error=$error)"
    }
}
