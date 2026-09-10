package id.app.education.dataclass.ResponData

import androidx.room.PrimaryKey
import id.app.education.di.module.NullToEmptyString

data class UserTable(
    @PrimaryKey var id: Int = 0,
    @NullToEmptyString var uuid: String = "",
    @NullToEmptyString var name: String = "",
    @NullToEmptyString var email: String = "",
    @NullToEmptyString var nisn_nik: String = "",
    @NullToEmptyString var nis_nik: String = "",
    @NullToEmptyString var phone: String = "",
    @NullToEmptyString var user_avatar_image: String = "",
    @NullToEmptyString var username: String = "",
    var school_id: Int = 0,
    val is_verified: Boolean = false,
    var wallet_id: String = "",
    var class_name: String = "",
    var majors: String = ""
) {

    constructor(item: FeedUser) : this(
        item.id,
        item.uuid,
        item.name,
        item.email,
        item.nisn_nik,
        item.nis_nik,
        item.phone,
        item.user_avatar_image,
        item.user_username,
        is_verified = item.is_verified
    )

    constructor(item: ContactItem) : this(
        item.user_id,
        item.user_uuid,
        item.name,
        user_avatar_image = item.foto,
        wallet_id = item.wallet_id,
        class_name = item.kelas,
        majors = item.jurusan
    )
}