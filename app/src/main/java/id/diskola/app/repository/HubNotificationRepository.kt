package id.diskola.app.repository

import id.diskola.app.apiservice.KlaspayApiService
import id.diskola.app.apiservice.NotifikasiApiService
import id.diskola.app.utils.session.SessionStore
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import timber.log.Timber

/**
 * The bell badge on the Pembelajaran hub (legacy `PembelajaranVm.fetchUnreadNotificationsCount`): only for accounts with an
 * active Klaspay wallet, `payment/wallet` first for the wallet's `user_id`, then `notification/summary?wallet_id=`.
 */
class HubNotificationRepository @Inject constructor(
    private val klaspay: KlaspayApiService,
    private val notifications: NotifikasiApiService,
    private val sessionStore: SessionStore,
) {
    /**
     * The unread count, or null when there is nothing to update — Klaspay inactive, wallet unreadable or without an id:
     * the badge then keeps what it showed (legacy). A failing summary call counts as 0.
     */
    suspend fun unreadCount(): Int? {
        if (!sessionStore.klaspayActive) return null
        val walletId = try {
            klaspay.getWallet().data?.user_id.orEmpty()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Gagal mengambil data wallet")
            return null
        }
        if (walletId.isBlank()) return null
        return try {
            notifications.getNotificationSummary(walletId).data?.count_unread ?: 0
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Gagal memuat jumlah notifikasi yang belum dibaca")
            0
        }
    }
}
