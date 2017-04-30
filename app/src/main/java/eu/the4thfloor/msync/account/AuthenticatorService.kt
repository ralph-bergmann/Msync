package eu.the4thfloor.msync.account


import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * A bound Service that instantiates the authenticator
 * when started.
 */
class AuthenticatorService : Service() {

    private val mAuthenticator: Authenticator by lazy { Authenticator(applicationContext) }

    /**
     * When the system binds to this Service to make the RPC call
     * return the authenticator's IBinder.
     */
    override fun onBind(intent: Intent): IBinder? {

        return mAuthenticator.iBinder
    }
}
