package id.diskola.app.dataclass

import id.diskola.app.dataclass.ResponData.KlaspayWalletResponse
import id.diskola.app.dataclass.ResponData.ResetPinResponse
import id.diskola.app.di.module.MoshiModule
import id.diskola.app.utils.PaymentRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** One odd field in a payment response may not fail the whole screen. */
class KlaspayModelsParseTest {

    private val moshi = MoshiModule.instance()

    private inline fun <reified T> parse(json: String): T = moshi.adapter(T::class.java).fromJson(json)!!

    @Test
    fun `a full wallet response is read`() {
        val data = parse<KlaspayWalletResponse>(
            """{"status":"success","data":{"wallet_id":"WL-1","user_id":"US7788","name":"RIZKY","email":"r@x.id","school_id":9,
               "is_toppers":true,"toppers_status":"pending","balance":1250000,"last_update":"2026-10-14T01:00:00.000Z"}}""",
        ).data!!
        assertEquals("WL-1", data.wallet_id)
        assertEquals("US7788", data.user_id)
        assertEquals("RIZKY", data.name)
        assertEquals(true, data.is_toppers)
        assertEquals("pending", data.toppers_status)
        assertEquals(1_250_000L, PaymentRules.balanceOf(data.balance))
    }

    @Test
    fun `balance may be an int, a decimal, a numeric string or missing`() {
        fun balance(json: String) = PaymentRules.balanceOf(parse<KlaspayWalletResponse>("""{"data":{"balance":$json}}""").data!!.balance)
        assertEquals(150_000L, balance("150000"))
        assertEquals(150_000L, balance("150000.00"))
        assertEquals(150_000L, balance("\"150000\""))
        assertEquals(0L, balance("null"))
        assertEquals(0L, PaymentRules.balanceOf(parse<KlaspayWalletResponse>("""{"data":{}}""").data!!.balance))
    }

    @Test
    fun `null and absent text fields become empty strings`() {
        val data = parse<KlaspayWalletResponse>("""{"data":{"wallet_id":null,"name":null,"toppers_status":null}}""").data!!
        assertEquals("", data.wallet_id)
        assertEquals("", data.name)
        assertEquals("", data.toppers_status)
        assertNull(data.is_toppers)
    }

    @Test
    fun `user_id may arrive as a number`() {
        assertEquals("5001", parse<KlaspayWalletResponse>("""{"data":{"user_id":5001}}""").data!!.user_id)
    }

    @Test
    fun `very long names survive`() {
        val name = "N".repeat(5_000)
        assertEquals(5_000, parse<KlaspayWalletResponse>("""{"data":{"name":"$name"}}""").data!!.name.length)
    }

    @Test
    fun `an empty body has no wallet`() {
        assertNull(parse<KlaspayWalletResponse>("{}").data)
    }

    @Test
    fun `reset pin response with a verified e-mail carries the token`() {
        val response = parse<ResetPinResponse>("""{"status":"success","reset_token":"tok-1","email_verified":true}""")
        assertTrue(response.email_verified == true)
        assertEquals("tok-1", response.reset_token)
    }

    @Test
    fun `reset pin response without the flags is not verified`() {
        val response = parse<ResetPinResponse>("""{"status":"success","message":"Email terkirim"}""")
        assertFalse(response.email_verified == true)
        assertEquals("", response.reset_token)
        assertEquals("Email terkirim", response.message)
    }
}
