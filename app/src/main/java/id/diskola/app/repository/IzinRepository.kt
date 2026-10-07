package id.diskola.app.repository

import id.diskola.app.apiservice.ApiException
import id.diskola.app.apiservice.PresensiApiService
import id.diskola.app.dataclass.ResponData.LeaveRequestItem
import id.diskola.app.dataclass.ResponData.LeaveRequestTable
import id.diskola.app.dataclass.ResponData.toTable
import id.diskola.app.dataclass.localDb.PresensiDao
import id.diskola.app.utils.PresensiRules
import id.diskola.app.utils.session.SessionStore
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

data class IzinGate(val block: PresensiRules.LeaveBlock?, val rejectedToday: Boolean)

/** Izin/sakit (doc `07` §10). The list is served from Room; [refresh] is the only thing that talks to the server. */
class IzinRepository @Inject constructor(
    private val api: PresensiApiService,
    private val dao: PresensiDao,
    private val sessionStore: SessionStore,
    private val presensiRepository: PresensiRepository,
) {
    private val role: String get() = if (sessionStore.isStudent) "student" else "staff"

    /** [approvalStatus] = `pending`/`approved`/`rejected`, or null for all. */
    fun observe(approvalStatus: String?): Flow<List<LeaveRequestTable>> = dao.observeLeaves(approvalStatus)

    suspend fun get(uuid: String): LeaveRequestTable? = dao.getLeave(uuid)

    /** Approval status of today's request (blank/null when there is none). */
    suspend fun todayStatus(): String? = dao.getLeaveByDate(LocalDate.now().toString())?.approvalStatus

    /** May the user file an izin right now, from what is cached (see [refreshGate] to make it fresh). */
    suspend fun gate(): IzinGate {
        val today = LocalDate.now().toString()
        val status = dao.getLeaveByDate(today)?.approvalStatus
        return IzinGate(
            block = PresensiRules.leaveBlock(
                hasCheckedInToday = presensiRepository.day(today)?.attendAt?.isNotBlank() == true,
                todayApprovalStatus = status,
            ),
            rejectedToday = status.orEmpty().equals("rejected", ignoreCase = true),
        )
    }

    /**
     * Brings "checked in today" and (unless [includeToday] is false because [refresh] just did) today's izin up to date. The legacy form trusted whatever the Absensi
     * tab had cached, so it could allow an izin after the user had already checked in.
     */
    suspend fun refreshGate(includeToday: Boolean = true) {
        runCatchingKeepCancel { presensiRepository.fetchMonth(YearMonth.now()) }
        if (includeToday) {
            runCatchingKeepCancel { api.leaveToday(role).data }?.let { dao.upsertLeaves(listOf(it.toTable())) }
        }
    }

    /**
     * Today's request + first history page. The two are independent: a failure of one never skips the
     * other (legacy aborted the history when `today` failed). Returns whether another page exists.
     */
    suspend fun refresh(): Boolean {
        val today = runCatchingKeepCancel { api.leaveToday(role).data }
        var hasNext = false
        runCatchingKeepCancel { api.leaveList(role, 1) }?.let { page ->
            val rows = page.data.map { it.toTable() }.toMutableList()
            today?.let { item -> if (rows.none { it.uuid == item.uuid }) rows += item.toTable() }
            dao.replaceLeaves(rows)
            hasNext = isMorePages(page.meta?.current_page, page.meta?.last_page, page.data.size)
        } ?: today?.let { dao.upsertLeaves(listOf(it.toTable())) }
        return hasNext
    }

    /** Appends page [page] (≥ 2) to the cache; returns whether another page exists. */
    suspend fun loadPage(page: Int): Boolean {
        val response = api.leaveList(role, page)
        dao.upsertLeaves(response.data.map { it.toTable() })
        return isMorePages(response.meta?.current_page, response.meta?.last_page, response.data.size)
    }

    /** Uploads the proof and stores the created request. */
    suspend fun submit(status: String, note: String, file: File, mimeType: String): LeaveRequestItem {
        val part = MultipartBody.Part.createFormData("file", file.name, file.asRequestBody(mimeType.toMediaTypeOrNull()))
        val created = try {
            api.submitLeave(
                role = role,
                status = status.toRequestBody("text/plain".toMediaTypeOrNull()),
                note = note.trim().toRequestBody("text/plain".toMediaTypeOrNull()),
                file = part,
            ).data
        } catch (e: ApiException) {
            throw IllegalStateException(e.validationMessages.joinToString("\n").ifBlank { e.message.orEmpty() })
        } ?: throw IllegalStateException("Pengajuan gagal dikirim")
        dao.upsertLeaves(listOf(created.toTable()))
        return created
    }

    private fun isMorePages(current: Int?, last: Int?, size: Int): Boolean =
        size > 0 && (current ?: 0) < (last ?: 0)

    private suspend fun <T> runCatchingKeepCancel(block: suspend () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}
