package eu.the4thfloor.msync.ui

import android.app.Activity
import android.os.Bundle
import eu.the4thfloor.msync.BuildConfig.PREF_ACTION_SYNC_NOW
import org.jetbrains.anko.longToast

class PrefActionsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent?.dataString?.let {
            if (PREF_ACTION_SYNC_NOW.equals(it, ignoreCase = true)) {
                applicationContext.longToast("sync now")
            }
        }

        finish()
    }
}
