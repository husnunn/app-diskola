package id.diskola.app.dataclass

import id.diskola.app.dataclass.ResponData.PaymentChannelResponse
import id.diskola.app.dataclass.ResponData.PaymentGuideResponse
import id.diskola.app.dataclass.ResponData.PaymentHistoryDetailResponse
import id.diskola.app.dataclass.ResponData.PaymentTrxResponse
import id.diskola.app.dataclass.ResponData.SppHistoryResponse
import id.diskola.app.dataclass.ResponData.SppPayRequest
import id.diskola.app.di.module.MoshiModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SppPaymentModelsParseTest {

    private val moshi = MoshiModule.instance()

    @Test
    fun channels_keepServerOrderAndTolerateNulls() {
        val json = """{"data":{"saldo":[{"channel_method_id":1,"channel_method_name":"SALDO","channel_id":1,"image_url":null}],
            "virtual account":[{"channel_method_id":"12","channel_method_name":"BANK BRI","channel_id":5,"is_active":true}]}}"""
        val data = moshi.adapter(PaymentChannelResponse::class.java).fromJson(json)!!.data!!
        assertEquals(listOf("saldo", "virtual account"), data.keys.toList())
        assertEquals("", data["saldo"]!![0].image_url)
        assertEquals(12, data["virtual account"]!![0].channel_method_id)
    }

    @Test
    fun sppHistory_readsPriceAsNumberOrStringAndMissingFields() {
        val json = """{"data":{"transaction_invoice":[
            {"transaction_id":"A","price":150000,"note":"SPP Juli 2026","created_date":"2026-07-01","paid_date":null,"is_inquiry_channel":false},
            {"transaction_id":"B","price":"175000.00","note":"SPP Agustus 2026"}]}}"""
        val items = moshi.adapter(SppHistoryResponse::class.java).fromJson(json)!!.data!!.transaction_invoice!!
        assertEquals(150000.0, items[0].price!!, 0.0)
        assertEquals("", items[0].paid_date)
        assertEquals(175000.0, items[1].price!!, 0.0)
        assertNull(items[1].is_inquiry_channel)
    }

    @Test
    fun sppHistory_emptyData() {
        assertNull(moshi.adapter(SppHistoryResponse::class.java).fromJson("""{"data":null}""")!!.data)
        assertTrue(moshi.adapter(SppHistoryResponse::class.java).fromJson("""{"data":{"transaction_invoice":[]}}""")!!.data!!.transaction_invoice!!.isEmpty())
    }

    @Test
    fun payment_invoiceFieldsAndNullStrings() {
        val json = """{"status":"success","message":null,"data":{"transaction_id":"T1","bank_name":"BANK BRI","virtual_account":"8888",
            "admin_fee":4000,"amount":150000,"total_amount":154000,"expired":"27/10/2026 08:00","paid_at":null,"status":null}}"""
        val response = moshi.adapter(PaymentTrxResponse::class.java).fromJson(json)!!
        assertEquals("", response.message)
        assertEquals("8888", response.data!!.virtual_account)
        assertEquals(154000.0, response.data!!.total_amount!!, 0.0)
        assertEquals("", response.data!!.paid_at)
    }

    @Test
    fun guide_keepsSectionOrder() {
        val json = """{"status":"success","data":{"ATM":["Masukkan kartu","Pilih menu"],"Mobile Banking":["Buka aplikasi"]}}"""
        val data = moshi.adapter(PaymentGuideResponse::class.java).fromJson(json)!!.data!!
        assertEquals(listOf("ATM", "Mobile Banking"), data.keys.toList())
        assertEquals(2, data["ATM"]!!.size)
    }

    @Test
    fun historyDetail_readsTheShownFields() {
        val json = """{"status":"success","data":{"transaction_detail":{"transaction_id":"T1","amount":150000,"total_amount":154000,
            "fee_admin":null,"type":"SPP","transaction_status":"pending","created_at":"2026-10-27T01:00:00.000Z","payment_method":"BANK BRI"}}}"""
        val detail = moshi.adapter(PaymentHistoryDetailResponse::class.java).fromJson(json)!!.data!!.transaction_detail!!
        assertEquals("BANK BRI", detail.payment_method)
        assertNull(detail.fee_admin)
        assertEquals("", detail.paid_date)
    }

    @Test
    fun payRequest_isTheLegacyBody() {
        val json = moshi.adapter(SppPayRequest::class.java).toJson(SppPayRequest("A,B", "123456", 1, 9))
        assertEquals("""{"transaction_id":"A,B","pin":"123456","channel":1,"channel_method_id":9}""", json)
    }
}
