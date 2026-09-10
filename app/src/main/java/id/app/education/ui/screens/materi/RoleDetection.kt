package id.app.education.ui.screens.materi

import android.content.Context
import id.app.education.utils.PreferenceClass

/** Ported verbatim from `MateriFragment.isTeacherRole()` / `detectStudentRole()`. */
fun isTeacherRole(context: Context, pref: PreferenceClass): Boolean {
    if (pref.getBoolean("is_teacher")) return true
    val prefRoles = pref.getString("roles").lowercase()
    if (prefRoles.contains("guru") || prefRoles.contains("teacher")) return true

    val sp = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
    if (sp.getBoolean("is_teacher", false)) return true
    val ruleLabel = sp.getString("rule_label", "").orEmpty().lowercase()
    return ruleLabel.contains("guru") || ruleLabel.contains("teacher")
}

fun isStudentRole(context: Context, pref: PreferenceClass): Boolean = !isTeacherRole(context, pref)
