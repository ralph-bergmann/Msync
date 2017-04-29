package eu.the4thfloor.msync.ui

import android.app.Activity
import android.os.Bundle
import eu.the4thfloor.msync.utils.hasAccount
import org.jetbrains.anko.startActivity

class CheckLoginStatusActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (hasAccount()) {
            startActivity<SettingsActivity>()
        } else {
            startActivity<LoginActivity>()
        }
        finish()
    }
}
