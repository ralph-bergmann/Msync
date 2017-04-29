package eu.the4thfloor.msync.account


import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.accounts.NetworkErrorException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import eu.the4thfloor.msync.ui.LoginActivity
import org.jetbrains.anko.bundleOf

/*
 * Implement AbstractAccountAuthenticator and stub out all
 * of its methods
 */
class Authenticator(private val context: Context) : AbstractAccountAuthenticator(context) {

    // Editing properties is not supported
    override fun editProperties(response: AccountAuthenticatorResponse,
                                accountType: String): Bundle? {

        throw UnsupportedOperationException()
    }

    @Throws(NetworkErrorException::class)
    override fun addAccount(response: AccountAuthenticatorResponse,
                            accountType: String,
                            authTokenType: String?,
                            requiredFeatures: Array<String>?,
                            options: Bundle?): Bundle? {

        val intent = Intent(context, LoginActivity::class.java)
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
        return bundleOf(AccountManager.KEY_INTENT to intent)
    }

    // Ignore attempts to confirm credentials
    @Throws(NetworkErrorException::class)
    override fun confirmCredentials(response: AccountAuthenticatorResponse,
                                    account: Account,
                                    options: Bundle?): Bundle? {

        return null
    }

    // Getting an authentication token is not supported
    @Throws(NetworkErrorException::class)
    override fun getAuthToken(response: AccountAuthenticatorResponse,
                              account: Account,
                              authTokenType: String,
                              options: Bundle?): Bundle? {

        throw UnsupportedOperationException()
    }

    // Getting a label for the auth token is not supported
    override fun getAuthTokenLabel(authTokenType: String): String? {

        throw UnsupportedOperationException()
    }

    // Updating user credentials is not supported
    @Throws(NetworkErrorException::class)
    override fun updateCredentials(response: AccountAuthenticatorResponse,
                                   account: Account,
                                   authTokenType: String?,
                                   options: Bundle?): Bundle? {

        throw UnsupportedOperationException()
    }

    // Checking features for the account is not supported
    @Throws(NetworkErrorException::class)
    override fun hasFeatures(response: AccountAuthenticatorResponse,
                             account: Account,
                             features: Array<String>): Bundle? {

        throw UnsupportedOperationException()
    }
}
