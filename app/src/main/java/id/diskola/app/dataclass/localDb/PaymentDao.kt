package id.diskola.app.dataclass.localDb

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.diskola.app.dataclass.ResponData.PaymentGuideTable
import id.diskola.app.dataclass.ResponData.PaymentInvoiceTable

/** Invoices to pay outside the wallet and the cached payment guides (doc `08` §2.5, §2.6, §14.1). */
@Dao
interface PaymentDao {

    @Query("SELECT * FROM payment_invoice WHERE trx_id = :trxId LIMIT 1")
    suspend fun invoice(trxId: String): PaymentInvoiceTable?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInvoice(invoice: PaymentInvoiceTable)

    @Query("DELETE FROM payment_invoice WHERE trx_id = :trxId")
    suspend fun deleteInvoice(trxId: String)

    @Query("SELECT * FROM payment_guide WHERE channel = :channel LIMIT 1")
    suspend fun guide(channel: String): PaymentGuideTable?

    /** One row per channel: a refreshed guide replaces the old one instead of being added to it (decision Q4). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGuide(guide: PaymentGuideTable)
}
