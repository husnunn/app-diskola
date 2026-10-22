package id.diskola.app.dataclass.ResponData

import com.squareup.moshi.JsonClass
import id.diskola.app.di.module.NullToEmptyString

/**
 * `GET payment/wallet` (doc `08-keuangan-pembayaran-klaspay-ppob.md` §5.5). Every field tolerates a missing key or `null`: the hub must
 * still render when the server omits one (legacy `KlaspayWalletData`, where [balance] is an Int — a Double here also reads `"150000"`).
 */
@JsonClass(generateAdapter = true)
data class KlaspayWalletResponse(
    @NullToEmptyString val status: String = "",
    @NullToEmptyString val message: String = "",
    val data: KlaspayWalletData? = null,
)

@JsonClass(generateAdapter = true)
data class KlaspayWalletData(
    @NullToEmptyString val wallet_id: String = "",
    /** The id shown to the user as "ID Wallet" and the key the notification summary is looked up by. */
    @NullToEmptyString val user_id: String = "",
    @NullToEmptyString val name: String = "",
    @NullToEmptyString val email: String = "",
    val school_id: Int? = null,
    val is_toppers: Boolean? = null,
    @NullToEmptyString val toppers_status: String = "",
    val balance: Double? = null,
    @NullToEmptyString val last_update: String = "",
)

/** `POST payment/reset-pin` (§4.4): asks the server to mail a reset link, or hands back a token when the e-mail is already verified. */
@JsonClass(generateAdapter = true)
data class ResetPinRequest(val email: String)

@JsonClass(generateAdapter = true)
data class ResetPinResponse(
    @NullToEmptyString val status: String = "",
    @NullToEmptyString val message: String = "",
    @NullToEmptyString val reset_token: String = "",
    val email_verified: Boolean? = null,
    val mail_verified: Boolean? = null,
)

/** `POST payment/reset-pin/setpin` (§4.2). */
@JsonClass(generateAdapter = true)
data class SetPinRequest(val pin: String, val reset_token: String)

/** `PUT mobile/app/accounts/user/{uuid}/change-profile` — the legacy dialogs send all three fields. */
@JsonClass(generateAdapter = true)
data class UpdateProfileRequest(val phone: String, val email: String, val username: String)

/** `PUT mobile/app/accounts/user/{uuid}/change-password`. */
@JsonClass(generateAdapter = true)
data class ChangePasswordRequest(val old_password: String, val new_password: String)
