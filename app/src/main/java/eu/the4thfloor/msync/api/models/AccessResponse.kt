package eu.the4thfloor.msync.api.models


/**
 * {
 * ... "access_token":"ACCESS_TOKEN_TO_STORE",
 * ... "token_type":"bearer",
 * ... "expires_in":3600,
 * ... "refresh_token":"TOKEN_USED_TO_REFRESH_AUTHORIZATION"
 * }
 */
class AccessResponse {

    var access_token: String? = null
    var token_type: String? = null
    var expires_in: Long? = null
    var refresh_token: String? = null
}
