package id.diskola.app.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceInfoRulesTest {

    @Test
    fun `device name joins maker and model with a capital`() {
        assertEquals("Samsung SM-A515F", DeviceInfoRules.deviceName("samsung", "SM-A515F"))
        assertEquals("Xiaomi Redmi Note 11", DeviceInfoRules.deviceName("Xiaomi", "Redmi Note 11"))
    }

    @Test
    fun `device name does not repeat a maker the model already starts with`() {
        assertEquals("Xiaomi 2201117TG", DeviceInfoRules.deviceName("Xiaomi", "Xiaomi 2201117TG"))
        assertEquals("oppo CPH2269", DeviceInfoRules.deviceName("OPPO", "oppo CPH2269"))
    }

    @Test
    fun `device name survives a missing half`() {
        assertEquals("SM-A515F", DeviceInfoRules.deviceName("", "SM-A515F"))
        assertEquals("Samsung", DeviceInfoRules.deviceName("samsung", " "))
        assertEquals(DeviceInfoRules.NOT_AVAILABLE, DeviceInfoRules.deviceName("", ""))
    }

    @Test
    fun `bytes read in gigabytes from one gigabyte up, megabytes below`() {
        assertEquals("3.7 GB", DeviceInfoRules.formatBytes((3.7 * 1024 * 1024 * 1024).toLong()))
        assertEquals("1.0 GB", DeviceInfoRules.formatBytes(1024L * 1024 * 1024))
        assertEquals("512 MB", DeviceInfoRules.formatBytes(512L * 1024 * 1024))
        assertEquals("0 MB", DeviceInfoRules.formatBytes(0))
    }

    @Test
    fun `an unreadable size says so`() {
        assertEquals(DeviceInfoRules.NOT_AVAILABLE, DeviceInfoRules.formatBytes(-1))
    }

    @Test
    fun `a blank value is spelled out and others are trimmed`() {
        assertEquals(DeviceInfoRules.NOT_AVAILABLE, DeviceInfoRules.row("Patch keamanan", "  ").value)
        assertEquals(DeviceInfoRules.NOT_AVAILABLE, DeviceInfoRules.row("Arsitektur", null).value)
        assertEquals("14", DeviceInfoRules.row("Versi Android", " 14 ").value)
    }

    @Test
    fun `plain text lists every group and row under the capture time`() {
        val groups = listOf(
            DeviceInfoGroup("Aplikasi", listOf(DeviceInfoRules.row("Versi aplikasi", "2.2.84"), DeviceInfoRules.row("Nomor build", "83"))),
            DeviceInfoGroup("Perangkat", listOf(DeviceInfoRules.row("API Level", "34"))),
        )
        val expected = """
            Diskola - Informasi Perangkat
            Dicatat: 2026-10-28 14:03:11 (Asia/Jakarta)

            [Aplikasi]
            Versi aplikasi: 2.2.84
            Nomor build: 83

            [Perangkat]
            API Level: 34
        """.trimIndent()
        assertEquals(expected, DeviceInfoRules.toPlainText(groups, "2026-10-28 14:03:11 (Asia/Jakarta)"))
    }
}
