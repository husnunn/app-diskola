package id.diskola.app.ui.screens.materi

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import id.diskola.app.utils.PreferenceClass

/** Ported verbatim from `MateriFragment.isTeacherRole()` / `detectStudentRole()` — reads the main
 * prefs' `is_teacher`/`roles` (written by [id.diskola.app.utils.session.SessionStore] for every
 * login path), not the retired `user_session` file. */
fun isTeacherRole(context: Context, pref: PreferenceClass): Boolean {
    if (pref.getBoolean("is_teacher")) return true
    val prefRoles = pref.getString("roles").lowercase()
    return prefRoles.contains("guru") || prefRoles.contains("teacher")
}

fun isStudentRole(context: Context, pref: PreferenceClass): Boolean = !isTeacherRole(context, pref)

/**
 * Role of the signed-in user, for the screens that branch on it. Fase 3 makes Materi role-aware
 * and gates Asesmen to students, so this is read in several places.
 */
@Composable
fun rememberIsTeacher(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val pref = PreferenceClass(context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE))
        isTeacherRole(context, pref)
    }
}
