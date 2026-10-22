package id.diskola.app.dataclass

import id.diskola.app.dataclass.ResponData.CheckAccountApiResponse
import id.diskola.app.dataclass.ResponData.CurrentUserResponse
import id.diskola.app.dataclass.ResponData.KlaspayWalletResponse
import id.diskola.app.dataclass.ResponData.NotificationSummaryResponse
import id.diskola.app.dataclass.ResponData.TodaySettingResponse
import id.diskola.app.di.module.MoshiModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The hub's DTOs must survive nulls, missing keys and very long texts: one bad field may not fail the whole response. */
class HubModelsParseTest {

    private val moshi = MoshiModule.instance()

    private inline fun <reified T> parse(json: String): T = moshi.adapter(T::class.java).fromJson(json)!!

    private fun checkAccount(extra: String) = """
        {"data":{"id":1,"uuid":"u","name":"Siswa","school":{"id":9,"uuid":"s","name":"SMK Demo","image":null}},
         "rule":{"is_student":true,"is_teacher":false},"is_active":true $extra}
    """.trimIndent()

    @Test
    fun `check-account carries current_subject and userProgress`() {
        val response = parse<CheckAccountApiResponse>(
            checkAccount(
                """, "current_subject":{"id":12,"attendance_id":77,"user_id":3,"plot_start_at":"07:00","plot_end_at":"08:30",
                   "teacher_name":"Bu Guru","subject_name":"Matematika","subject_icon_image":"https://x/i.png","status":""},
                   "userProgress":{"requested_user_id":3,"status":"in_review","message":"Menunggu persetujuan admin"}""",
            ),
        )
        val subject = response.current_subject!!
        assertEquals(12, subject.id)
        assertEquals(77, subject.attendance_id)
        assertEquals("07:00", subject.plot_start_at)
        assertEquals("Matematika", subject.subject_name)
        assertEquals("", subject.status)
        assertEquals("in_review", response.userProgress!!.status)
        assertEquals("Menunggu persetujuan admin", response.userProgress!!.message)
    }

    @Test
    fun `null or missing current_subject and userProgress stay null`() {
        assertNull(parse<CheckAccountApiResponse>(checkAccount("")).current_subject)
        val explicit = parse<CheckAccountApiResponse>(checkAccount(""","current_subject":null,"userProgress":null"""))
        assertNull(explicit.current_subject)
        assertNull(explicit.userProgress)
    }

    @Test
    fun `current_subject tolerates nulls in every field`() {
        val subject = parse<CheckAccountApiResponse>(
            checkAccount(
                ""","current_subject":{"id":null,"attendance_id":null,"plot_start_at":null,"plot_end_at":null,
                   "subject_name":null,"subject_icon_image":null,"status":null}""",
            ),
        ).current_subject!!
        assertNull(subject.id)
        assertNull(subject.attendance_id)
        assertEquals("", subject.subject_name)
        assertEquals("", subject.plot_start_at)
        assertNull(subject.status)
    }

    @Test
    fun `very long texts are kept whole`() {
        val long = "Pengajuan verifikasi data Anda ditolak karena ".repeat(200)
        val response = parse<CheckAccountApiResponse>(checkAccount(""","userProgress":{"status":"rejected","message":"$long"}"""))
        assertEquals(long, response.userProgress!!.message)
        assertTrue(response.userProgress!!.message.length > 8_000)
    }

    @Test
    fun `school city_name is optional`() {
        val response = parse<CheckAccountApiResponse>(
            """{"data":{"id":1,"uuid":"u","school":{"id":9,"uuid":"s","city_name":"Surabaya"}}}""",
        )
        assertEquals("Surabaya", response.data!!.school!!.city_name)
        assertNull(parse<CheckAccountApiResponse>(checkAccount("")).data!!.school!!.city_name)
    }

    @Test
    fun `current-user keeps a null device id as null`() {
        val withDevice = parse<CurrentUserResponse>("""{"id":5,"uuid":"u","device_id":"abc","single_device_login_enabled":true}""")
        assertEquals("abc", withDevice.device_id)
        assertTrue(withDevice.single_device_login_enabled)
        val legacyToken = parse<CurrentUserResponse>("""{"id":5,"uuid":"u","device_id":null}""")
        assertNull(legacyToken.device_id)
        assertFalse(legacyToken.single_device_login_enabled)
    }

    @Test
    fun `wallet user_id may arrive as a string or a number`() {
        assertEquals("5001", parse<KlaspayWalletResponse>("""{"status":"success","data":{"wallet_id":"WL-1","user_id":"5001","balance":1}}""").data!!.user_id)
        assertEquals("5001", parse<KlaspayWalletResponse>("""{"data":{"user_id":5001}}""").data!!.user_id)
        assertEquals("", parse<KlaspayWalletResponse>("""{"data":{"user_id":null}}""").data!!.user_id)
        assertNull(parse<KlaspayWalletResponse>("""{}""").data)
    }

    @Test
    fun `notification summary count`() {
        assertEquals(3, parse<NotificationSummaryResponse>("""{"data":{"count_unread":3}}""").data!!.count_unread)
        assertNull(parse<NotificationSummaryResponse>("""{"data":{}}""").data!!.count_unread)
        assertNull(parse<NotificationSummaryResponse>("""{}""").data)
    }

    @Test
    fun `today setting parses events and tolerates missing parts`() {
        val data = parse<TodaySettingResponse>(
            """{"data":{"date":"2026-10-07","day":"Rabu","status":"hari_kerja","jam_masuk":"07:00",
               "events":[{"name":"Upacara","start_time":"07:00","end_time":"07:30","day_off":false},{"name":null,"day_off":null}],
               "teaching_today":[{"subject_schedule_id":1}],"roles":["guru"]}}""",
        ).data!!
        assertEquals("hari_kerja", data.status)
        assertEquals("07:00", data.jam_masuk)
        assertEquals(2, data.events!!.size)
        assertEquals("Upacara", data.events!![0].name)
        assertNull(data.events!![1].name)
        assertNull(data.events!![1].day_off)
        assertNotNull(parse<TodaySettingResponse>("""{"data":{"status":"holiday"}}""").data)
        assertNull(parse<TodaySettingResponse>("""{"data":{"status":"holiday"}}""").data!!.events)
    }
}
