package id.app.education.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import id.app.education.network.NotificationResponse
import timber.log.Timber
import java.lang.reflect.Type
import java.util.*


class DbConverter {

    @TypeConverter
    fun fromTimestamp(value: Long?): Date {
        return value?.let { Date(it) } ?: Date()
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long {
        return date?.time ?: 0
    }

    @TypeConverter
//    fun fromStringList(value: String?): List<String> {
//        val listType = object : TypeToken<List<String>>() {}.type
//        return Gson().fromJson(value, listType) ?: emptyList()
//    }

    fun fromStringList(value: String?): List<String> {
        Timber.d("Data JSON sebelum diparsing: $value")
        val type: Type = TypeToken.getParameterized(List::class.java, String::class.java).type
        return Gson().fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun toStringList(list: List<String>?): String {
        return Gson().toJson(list)
    }


    @TypeConverter
    fun fromPage(value: NotificationResponse?): String? {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toPage(value: String?): NotificationResponse? {
        return value?.let { Gson().fromJson(it, NotificationResponse::class.java) }
    }
}