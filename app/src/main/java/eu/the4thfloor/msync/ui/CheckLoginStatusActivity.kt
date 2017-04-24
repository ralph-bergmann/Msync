package eu.the4thfloor.msync.ui

import android.app.Activity
import android.os.Bundle
import eu.the4thfloor.msync.utils.PREF_ACCESS_TOKEN
import eu.the4thfloor.msync.utils.PREF_REFRESH_TOKEN
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.startActivity

class CheckLoginStatusActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (defaultSharedPreferences.contains(PREF_ACCESS_TOKEN) &&
            defaultSharedPreferences.contains(PREF_REFRESH_TOKEN)) {
            startActivity<SettingsActivity>()
        } else {
            startActivity<LoginActivity>()
        }
        finish()
    }
}
