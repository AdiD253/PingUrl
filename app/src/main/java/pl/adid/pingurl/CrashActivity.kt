package pl.adid.pingurl

import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import cat.ereza.customactivityoncrash.CustomActivityOnCrash
import kotlinx.android.synthetic.main.activity_crash.*
import timber.log.Timber

class CrashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crash)


        message.text = CustomActivityOnCrash.getStackTraceFromIntent(intent) ?: "null"

        AlertDialog.Builder(this)
                .setTitle("Błąd")
                .setMessage("Aplikacja została zatrzymana")
                .setPositiveButton("pokaż błąd", {dialog, _ ->
                    CustomActivityOnCrash.getConfigFromIntent(intent)?.let {
                        CustomActivityOnCrash.restartApplication(this@CrashActivity, it)
                    }

                })
                .show()

        Timber.e(CustomActivityOnCrash.getActivityLogFromIntent(intent))
        Timber.e(CustomActivityOnCrash.getAllErrorDetailsFromIntent(this@CrashActivity, intent))
        Timber.e(CustomActivityOnCrash.getStackTraceFromIntent(intent))
    }
}