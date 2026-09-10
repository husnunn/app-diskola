package id.app.education.utils

import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject

class PreferenceClass @Inject constructor(private val sharedPreference: SharedPreferences) :
    SharedPreferences {

    fun putString(key: String, value: String) =
        sharedPreference.edit { putString(key, value) }

    fun putInt(key: String, value: Int) = sharedPreference.edit { putInt(key, value) }

    fun putLong(key: String, value: Long) = sharedPreference.edit { putLong(key, value) }

    fun putBoolean(key: String, value: Boolean) =
        sharedPreference.edit { putBoolean(key, value) }

    fun remove(key: String) = sharedPreference.edit { remove(key) }

    fun clear() = sharedPreference.edit { clear() }

    override fun getString(key: String?, defValue: String?): String = try {
        key?.let { sharedPreference.getString(it, defValue) } ?: defValue.orEmpty()
    } catch (e: Exception) {
        defValue.orEmpty()
    }

    fun getString(key: String) = getString(key, "")

    override fun getBoolean(key: String?, defValue: Boolean): Boolean = try {
        key?.let { sharedPreference.getBoolean(it, defValue) } ?: defValue
    } catch (e: Exception) {
        defValue
    }

    fun getBoolean(key: String) = getBoolean(key, false)

    override fun getInt(key: String?, defValue: Int): Int = try {
        key?.let { sharedPreference.getInt(it, defValue) } ?: defValue
    } catch (e: Exception) {
        defValue
    }

    fun getInt(key: String) = getInt(key, 0)

    override fun getLong(key: String?, defValue: Long): Long = try {
        key?.let { sharedPreference.getLong(it, defValue) } ?: defValue
    } catch (e: Exception) {
        defValue
    }

    fun getLong(key: String) = getLong(key, 0L)

    override fun getFloat(key: String?, defValue: Float): Float = try {
        key?.let { sharedPreference.getFloat(it, defValue) } ?: defValue
    } catch (e: Exception) {
        defValue
    }

    override fun contains(key: String?): Boolean =
        key?.let { sharedPreference.contains(it) } ?: false

    override fun getAll(): MutableMap<String, *> = sharedPreference.all

    override fun edit(): SharedPreferences.Editor = sharedPreference.edit()

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String> =
        sharedPreference.getStringSet(key, defValues) ?: mutableSetOf()

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) = sharedPreference.unregisterOnSharedPreferenceChangeListener(listener)

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) = sharedPreference.registerOnSharedPreferenceChangeListener(listener)
}
