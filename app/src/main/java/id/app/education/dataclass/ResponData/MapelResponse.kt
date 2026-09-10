package id.app.education.dataclass.ResponData

import androidx.annotation.Keep
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.squareup.moshi.JsonClass
import id.app.education.di.module.NullToEmptyString
import timber.log.Timber

@Keep
@JsonClass(generateAdapter = true)
data class MapelResponse(val data: List<MapelItem> = emptyList())

@Keep
@JsonClass(generateAdapter = true)
data class MapelItem(
    val id: Int = 0,
    val name: String = "",
    @NullToEmptyString val icon_image: String = "",
    val message_label: String = "",
    val teacher: TeacherItem? = null
)

@Keep
@JsonClass(generateAdapter = true)
data class MajorResponse(val data: List<MajorItem> = emptyList())

@Entity(tableName = "major")
@Keep
@JsonClass(generateAdapter = true)
data class MajorItem(@PrimaryKey val id: Int = 0, @NullToEmptyString val name: String = "")

@Keep
@JsonClass(generateAdapter = true)
data class TeacherItem(
    val id: Int = 0,
    val name: String = "",
    val nip: String = "",
    val nik: String = "",
    val address: String = "",
    val user: FeedUser? = null
)

@Keep
@JsonClass(generateAdapter = true)
data class MateriResponse(val data: List<MateriItem> = emptyList())

@Keep
@JsonClass(generateAdapter = true)
data class UploadMateriResponse(val data: MateriItem = MateriItem())

@Keep
@JsonClass(generateAdapter = true)
data class DetailMateriResponse(val data: MateriItem = MateriItem())

@Keep
@JsonClass(generateAdapter = true)
data class MateriItem(
    val id: Int = 0,
    val name: String = "",
    @NullToEmptyString val description: String = "",
    val message_label: String = "",
    val uri: MateriLink? = null,
    @NullToEmptyString val file_path: String = "",
    @NullToEmptyString val file_name: String = "",
    @NullToEmptyString val file_format: String = "",
    @NullToEmptyString val file_type: String = "",
    @NullToEmptyString val file_size: String = "",
    val created_at_label: String = "",
    val subject: MapelItem? = null,
    val teacher: TeacherItem? = null,
    val school: PembelajaranSekolahItem? = null,
    val grade: Int? = 0,
    val school_class: Any? = null,
    val school_major: Any? = null,
    @NullToEmptyString val file_edited: String = ""
)

@Keep
@JsonClass(generateAdapter = true)
data class MateriLink(val link: List<String> = emptyList())

@Keep
@JsonClass(generateAdapter = true)
@Entity(tableName = "grade", indices = [Index("id", unique = true)])
data class GradeTable(@PrimaryKey val id: Int = 0, val name: String = "")

@Keep
@JsonClass(generateAdapter = true)
data class GradeResponse(val data: List<GradeTable> = emptyList())

@Entity(tableName = "mapel", indices = [Index(name = "mapel_idx", value = ["id"], unique = true)])
data class MapelTable(
    @PrimaryKey val id: Long = 0L,
    val name: String = "",
    val image: String = "",
    val label: String = "",
    val teacher_id: Int = 0,
    val user_id: Int = 0
) {
    companion object {
        fun fromMapelItem(item: MapelItem): MapelTable = MapelTable(
            item.id.toLong(),
            item.name,
            item.icon_image,
            item.message_label,
            item.teacher?.id ?: 0,
            item.teacher?.user?.id ?: 0
        )
    }
}

@Entity(
    tableName = "mapel_teacher", primaryKeys = ["mapelId", "teacherId"], indices = [
        Index(name = "mapelteacherIdx", value = ["mapelId"], unique = false),
        Index(name = "teachermapelIdx", value = ["teacherId"], unique = false)
    ]
)
data class MapelTeacherCrossRef(val mapelId: Long = 0L, val teacherId: Long = 0L)

data class MapelWithTeacher(
    @Embedded val crossRef: MapelTeacherCrossRef,
    @Relation(parentColumn = "mapelId", entityColumn = "id") val mapel: MapelTable
)

@Entity(tableName = "materi", indices = [Index(name = "materi_idx", value = ["id"], unique = true)])
data class MateriTable(
    @PrimaryKey val id: Long = 0L,
    val name: String = "",
    val description: String = "",
    val message_label: String = "",
    val file_path: String = "",
    val file_name: String = "",
    val file_format: String = "",
    val file_type: String = "",
    val file_size: String = "",
    val created_at_label: String = "",
    val subject_id: Int = 0,
    val teacher_id: Int = 0,
    val user_id: Int = 0,
    val grade: Int = 0,
    val class_id: Int = 0,
    val major_id: Int = 0,
    val explanation_file_path: String = "",
) {
    companion object {
        fun fromMateriItem(it: MateriItem): MateriTable = MateriTable(
            it.id.toLong(),
            it.name,
            it.description,
            it.message_label,
            it.file_path,
            it.file_name,
            it.file_format,
            it.file_type,
            it.file_size,
            it.created_at_label,
            it.subject?.id ?: 0,
            it.teacher?.id ?: 0,
            it.teacher?.user?.id ?: 0,
            it.grade ?: 0,
            (it.school_class as? Map<*, *>)?.also { Timber.e("class: $it") }?.get("id")?.toString()
                ?.toDouble()?.toInt() ?: 0,
            (it.school_major as? Map<*, *>)?.also { Timber.e("major: $it") }?.get("id")?.toString()
                ?.toDouble()?.toInt() ?: 0,
            it.file_edited
        )
    }
}

@Entity(
    tableName = "materi_link",
    indices = [Index(value = ["materi_id", "link"], unique = true)]
)
data class MateriLinkTable(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val materi_id: Long = 0,
    val link: String = ""
)

data class MateriWithLink(
    @Embedded val materi: MateriTable,
    @Relation(parentColumn = "id", entityColumn = "materi_id")
    val link: List<MateriLinkTable> = emptyList()
)

@Entity(
    tableName = "teacher",
    indices = [Index(name = "teacher_idx", value = ["id"], unique = true)]
)
data class TeacherTable(
    @PrimaryKey val id: Long = 0L,
    val name: String = "",
    val nip: String = "",
    val address: String = "",
    val sosmed_user_id: Int = 0
) {
    companion object {
        fun fromTeacherItem(item: TeacherItem, name: String? = null): TeacherTable = TeacherTable(
            item.id.toLong(),
            if (name.isNullOrEmpty()) item.name else name,
            item.nip,
            item.address,
            item.user?.id ?: 0
        )
    }
}

data class MapelTeacher(
    @Embedded val mapel: MapelTable,
    @Relation(parentColumn = "teacher_id", entityColumn = "id")
    val teacher: TeacherTable?
)

data class MateriMapelTeacher(
    @Embedded val materi: MateriTable,
    @Relation(parentColumn = "subject_id", entityColumn = "id")
    val mapel: MapelTable,
    @Relation(parentColumn = "teacher_id", entityColumn = "id")
    val teacher: TeacherTable?,
    @Relation(parentColumn = "user_id", entityColumn = "id")
    val userTable: UserTable?
)