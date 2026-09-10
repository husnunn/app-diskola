package id.app.education.utils

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject

class DateUtil @Inject constructor() {

    fun formatUTCToHourMinute(input: String): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val odt = OffsetDateTime.parse(input)
                val zoned = odt.atZoneSameInstant(ZoneId.systemDefault()) // use device timezone
                val hour = zoned.hour.toString().padStart(2, '0')
                val minute = zoned.minute.toString().padStart(2, '0')
                "$hour:$minute"
            } else {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale("id"))
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val date = sdf.parse(input)

                val outputFormat = SimpleDateFormat("HH:mm", Locale("id"))
                outputFormat.timeZone = TimeZone.getDefault() // use device timezone
                outputFormat.format(date ?: Date())
            }
        } catch (e: Exception) {
            ""
        }
    }

    fun formatUTCToDate(input: String): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val odt = OffsetDateTime.parse(input)
                val zoned = odt.atZoneSameInstant(ZoneId.systemDefault())
                val formatter = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", Locale.ENGLISH)
                zoned.format(formatter)
            } else {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val date = sdf.parse(input)

                val outputFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.ENGLISH)
                outputFormat.timeZone = TimeZone.getDefault()
                outputFormat.format(date ?: Date())
            }
        } catch (e: Exception) {
            ""
        }
    }

    fun getCurrentFormattedDate(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val currentDate = OffsetDateTime.now(ZoneId.systemDefault())
                val formatter = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", Locale.ENGLISH)
                currentDate.format(formatter)
            } else {
                val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.ENGLISH)
                sdf.timeZone = TimeZone.getDefault()
                sdf.format(Date())
            }
        } catch (e: Exception) {
            ""
        }
    }


    fun formatUTCToDateTime(input: String): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val odt = OffsetDateTime.parse(input)
                val zoned = odt.atZoneSameInstant(ZoneId.systemDefault())
                val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy • HH:mm", Locale.ENGLISH)
                zoned.format(formatter)
            } else {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val date = sdf.parse(input)

                val outputFormat = SimpleDateFormat("EEEE, d MMMM yyyy • HH:mm", Locale.ENGLISH)
                outputFormat.timeZone = TimeZone.getDefault()
                outputFormat.format(date ?: Date())
            }
        } catch (e: Exception) {
            ""
        }
    }



    fun isTimeAutomatic(c: Context): Boolean {
        return Settings.System.getInt(c.contentResolver, Settings.System.AUTO_TIME, 0) == 1
    }

    fun isTimeZoneAutomatic(c: Context): Boolean {
        return Settings.System.getInt(c.contentResolver, Settings.System.AUTO_TIME_ZONE, 0) == 1
    }

    fun formatDate(input: String): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                OffsetDateTime.parse(input).toEpochSecond()
            } catch (e: Exception) {
                0
            }
        } else {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale("id")).parse(input)?.time ?: 0
        }

    fun formatString(timeInMilis: Long): String {
        val now = Calendar.getInstance()
        val dateNow = now.get(Calendar.DAY_OF_YEAR)
        val cal = Calendar.getInstance().apply { timeInMillis = timeInMilis }
        val date = cal.get(Calendar.DAY_OF_YEAR)
        val dayFormat =
            if (date == dateNow) "Hari ini" else if (date - dateNow == 1) "Kemarin" else ""
        val dateFormat =
            SimpleDateFormat(
                "${if (dayFormat.isNotEmpty()) "" else "dd MMMM"} ${
                    if (cal.get(
                            Calendar.YEAR
                        ) != now.get(Calendar.YEAR)
                    ) "yyyy" else ""
                }, HH:mm", Locale("id")
            )
        return "${if (dayFormat.isNotEmpty()) dayFormat else ""}${dateFormat.format(date)}"
    }

    fun getDateTime(s: Long): String? {
        try {
            val locale = Locale("id", "ID")
            val sdf = SimpleDateFormat("dd MMMM yyyy",locale)
            val netDate = Date(s.toLong() * 1000)
            return sdf.format(netDate)
        } catch (e: Exception) {
            return e.toString()
        }
    }
    fun getDateTime2(s: Long): String {
        try {
            val locale = Locale("id", "ID")
            val sdf = SimpleDateFormat("dd MMMM yyyy HH:mm",locale)
            val netDate = Date(s.toLong() * 1000)
            return sdf.format(netDate)
        } catch (e: Exception) {
            return e.toString()
        }
    }
    fun getDateTime3(s: Long): String {
        try {
            val locale = Locale("id", "ID")
            val sdf = SimpleDateFormat("yyyy-MM-dd",locale)
            val netDate = Date(s.toLong() * 1000)
            return sdf.format(netDate)
        } catch (e: Exception) {
            return e.toString()
        }
    }
    fun getDateTimeTomorrow(s: Long): String {
        try {
            val locale = Locale("id", "ID")
            val sdf = SimpleDateFormat("dd MMMM yyyy HH:mm",locale)
            val netDate = Date((s.toLong() * 1000) + (1000 * 60 * 60 * 24))
            return sdf.format(netDate)
        } catch (e: Exception) {
            return e.toString()
        }
    }

    fun formatUTCToReadable(utcString: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val date = sdf.parse(utcString)

            val outputFormat = SimpleDateFormat("EEEE, dd MMM yyyy HH:mm", Locale("id"))
            outputFormat.timeZone = TimeZone.getDefault()
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            "-"
        }
    }


}