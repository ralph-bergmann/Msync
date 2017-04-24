package eu.the4thfloor.msync.ui

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import com.google.android.gms.appinvite.AppInviteInvitation
import com.google.android.gms.plus.PlusShare
import eu.the4thfloor.msync.BuildConfig
import eu.the4thfloor.msync.BuildConfig.PREF_ACTION_SHARE_FIREBASE
import eu.the4thfloor.msync.BuildConfig.PREF_ACTION_SHARE_GOOGLE_PLUS
import eu.the4thfloor.msync.BuildConfig.PREF_ACTION_SYNC_NOW
import eu.the4thfloor.msync.R
import org.jetbrains.anko.longToast


class PrefActionsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent?.dataString?.let {
            if (PREF_ACTION_SYNC_NOW.equals(it, ignoreCase = true)) {
                applicationContext.longToast("sync now")
            } else if (PREF_ACTION_SHARE_GOOGLE_PLUS.equals(it, ignoreCase = true)) {
                val shareIntent = PlusShare.Builder(this)
                    .setType("text/plain")
                    .setText(getString(R.string.google_plus_share_message))
                    .setContentUrl(Uri.parse("https://expa8.app.goo.gl/?apn=" + BuildConfig.APPLICATION_ID))
                    .intent
                startActivity(shareIntent)
            } else if (PREF_ACTION_SHARE_FIREBASE.equals(it, ignoreCase = true)) {
                val intent = AppInviteInvitation.IntentBuilder(getString(R.string.firebase_app_invite_title))
                    .setMessage(getString(R.string.firebase_app_invite_message))
                    .setDeepLink(Uri.parse("https://expa8.app.goo.gl/?apn=" + BuildConfig.APPLICATION_ID))
                    .setEmailSubject(getString(R.string.firebase_app_invite_email_subject))
                    .setEmailHtmlContent(getString(R.string.firebase_app_invite_email_html))
                    .build()
                startActivityForResult(intent, 0)
            }
        }

        finish()
    }
}
