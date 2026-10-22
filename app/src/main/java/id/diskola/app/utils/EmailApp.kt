package id.diskola.app.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast

/** Opens the user's mail app on its inbox, through the system chooser ("Buka email dengan"). */
object EmailApp {
    fun open(context: Context) {
        val inbox = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_EMAIL)
        val chooser = Intent.createChooser(inbox, "Buka email dengan").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(chooser)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Tidak ada aplikasi email di perangkat ini", Toast.LENGTH_SHORT).show()
        }
    }
}
