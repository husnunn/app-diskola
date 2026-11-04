package id.diskola.app.dataclass

import id.diskola.app.dataclass.ResponData.MagangAttendanceResponse
import id.diskola.app.dataclass.ResponData.MagangScheduleResponse
import id.diskola.app.di.module.MoshiModule
import id.diskola.app.utils.MagangRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MagangModelsParseTest {

    private val moshi = MoshiModule.instance()

    private fun schedule(json: String) = moshi.adapter(MagangScheduleResponse::class.java).fromJson(json)!!
    private fun attendance(json: String) = moshi.adapter(MagangAttendanceResponse::class.java).fromJson(json)!!

    @Test
    fun schedule_readsACompleteDay() {
        val item = schedule(
            """
            { "data": [ {
              "id": 31,
              "company": { "id": 4, "name": "PT. Testing Magang", "address": "Jl. Uji 1", "latitude": "-7.29731", "longitude": "112.75836", "radius": "100" },
              "schedule": { "date": "2026-06-04", "start_time": "15:00:00", "end_time": "19:15:00" },
              "attendance": { "status": "Belum presensi", "can_attend": true, "can_leave": false, "can_submit_leave_request": true,
                              "leave_request": null, "radius_enabled": false }
            } ] }
            """,
        ).data!!.single()
        assertEquals(31L, item.id)
        assertEquals("PT. Testing Magang", item.company?.name)
        assertEquals(-7.29731, MagangRules.coordinate(item.company?.latitude), 0.0)
        assertEquals("100", item.company?.radius)
        assertEquals("2026-06-04", item.schedule?.date)
        assertEquals(true, item.attendance?.can_attend)
        assertEquals(false, item.attendance?.radius_enabled)
        assertNull(item.attendance?.leave_request)
    }

    @Test
    fun schedule_companyIdPastIntRangeAndRadiusAsNumberReadLikeTheDevServerSendsThem() {
        // Real row from dev (4 Nov 2026): the company id is above Int.MAX_VALUE and the radius is a JSON number.
        val item = schedule(
            """
            { "data": [
              { "id": 2026110425,
                "company": { "id": 2149267629, "name": "PT AMEERA MEKKAH", "address": "Jl. Raya Bratang Binangun No.85 B",
                             "latitude": "-7.2942595", "longitude": "112.7597888", "radius": 5 },
                "schedule": { "date": "2026-11-04", "start_time": "08:00:00", "end_time": "16:00:00" },
                "attendance": { "status": "Presensi Masuk", "can_attend": true, "can_leave": false,
                                "can_submit_leave_request": true, "leave_request": null, "radius_enabled": true } }
            ] }
            """,
        ).data!!.single()
        assertEquals(2149267629L, item.company?.id)
        assertEquals("5", item.company?.radius)
        assertEquals(true, item.attendance?.can_attend)
    }

    @Test
    fun schedule_numbersWhereStringsAreExpectedAndNullsDoNotFailTheList() {
        val items = schedule(
            """
            { "data": [
              { "id": 1, "company": { "name": null, "latitude": -7.5, "longitude": 112.7, "radius": 75 },
                "schedule": { "date": "2026-06-04", "start_time": null, "end_time": null },
                "attendance": { "status": null, "radius_enabled": null } },
              { "id": 2, "company": null, "schedule": null, "attendance": null }
            ] }
            """,
        ).data!!
        assertEquals("", items[0].company?.name)
        assertEquals("-7.5", items[0].company?.latitude)
        assertEquals("75", items[0].company?.radius)
        assertNull(items[0].attendance?.radius_enabled)
        assertNull(items[1].company)
    }

    @Test
    fun schedule_aMissingOrNullDataIsAnEmptyList() {
        assertNull(schedule("{}").data)
        assertNull(schedule("""{ "data": null }""").data)
    }

    @Test
    fun attendance_tellsReportsFromIzinAndKeepsTheLatitudeTypo() {
        val data = attendance(
            """
            { "data": [
              { "record_type": "attendance", "id": 12, "date": "2026-06-04", "check_in_at": "2026-06-04 14:50:00", "check_in_status": "on_time",
                "check_out_at": null, "file": null, "daily_report": null,
                "location": { "latitide": "-7.29731", "longitude": "112.75836" },
                "company": { "name": "PT. Testing Magang", "address": "Jl. Uji 1", "latitude": "-7.29731", "longitude": "112.75836", "radius": null } },
              { "record_type": "leave_request", "id": 40, "date": "2026-11-03", "status": "sakit", "note": "Demam", "file": "izin/surat.png", "can_delete": false,
                "company": { "name": "PT. Testing Magang" } }
            ] }
            """,
        ).data!!
        val report = data[0]
        assertFalse(MagangRules.isLeave(report))
        assertEquals("2026-06-04 14:50:00", report.check_in_at)
        assertEquals("", report.check_out_at)
        assertEquals("", report.daily_report)
        assertEquals(-7.29731, MagangRules.reportLatitude(report), 0.0)
        assertEquals("", report.company?.radius)

        val leave = data[1]
        assertTrue(MagangRules.isLeave(leave))
        assertEquals("sakit", leave.status)
        assertEquals("izin/surat.png", leave.file)
        assertEquals(1, MagangRules.leavesFromAttendance(data).size)
        assertEquals(1, MagangRules.reportsOf(data).size)
    }
}
