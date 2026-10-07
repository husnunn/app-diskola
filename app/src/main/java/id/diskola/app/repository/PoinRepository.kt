package id.diskola.app.repository

import id.diskola.app.apiservice.KonselingApiService
import id.diskola.app.dataclass.ResponData.PoinItemTable
import id.diskola.app.dataclass.ResponData.SearchPoinStudentItem
import id.diskola.app.dataclass.ResponData.StudentPoinItem
import id.diskola.app.dataclass.localDb.PoinDao
import id.diskola.app.utils.session.SessionStore
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

private const val PAGE_SIZE = 20

/** Result of picking a student in the teacher's search: either safe to continue with, or why not. */
sealed interface PoinTarget {
    /** [userId] is check-account's `data.id` — what `create/violation|achievement` take as `user_id`.
     * It is *not* the search item's `id` (that one is `score_student_id` for calling-student). */
    data class Ready(val userId: Int, val name: String) : PoinTarget
    data class Blocked(val message: String) : PoinTarget
}

data class PoinEvent(
    val key: String,
    val name: String,
    val score: Int,
    val image: String,
    val message: String,
    val date: String,
    val time: String,
    val creatorName: String,
)

data class PoinRecapRow(val name: String, val count: Int, val score: Int)
data class PoinRecap(val totalScore: Int, val rows: List<PoinRecapRow>)
data class PoinCall(val id: Int, val name: String, val at: String, val message: String)

data class PoinStudentSummary(
    val violations: List<PoinEvent>,
    val achievements: List<PoinEvent>,
    val violationRecap: PoinRecap,
    val achievementRecap: PoinRecap,
    val pendingCalls: List<PoinCall>,
)

/**
 * Poin (konseling) — doc `05-pembelajaran-materi-tugas.md` §9/§10. The three "jenis" master lists
 * are cached in Room with the usual ensureFirstPage/loadMore/refresh shape; the student's own
 * score is always network and never cached (the legacy `(id, poin_type)` cache collapsed events of
 * the same type and wiped both tabs on every fetch).
 */
class PoinRepository @Inject constructor(
    private val api: KonselingApiService,
    private val dao: PoinDao,
    private val authRepository: AuthRepository,
    private val sessionStore: SessionStore,
) {
    // ---- Master lists (violation / achievement / handling) ----

    fun observeItems(type: Int, search: String): Flow<List<PoinItemTable>> = dao.observeItems(type, search)

    suspend fun ensureFirstPage(type: Int, search: String) {
        if (dao.countItems(type, search) == 0) fetchPage(type, search, 0)
    }

    suspend fun loadMore(type: Int, search: String, currentCount: Int): Boolean = fetchPage(type, search, currentCount)

    suspend fun refresh(type: Int, search: String): Boolean = fetchPage(type, search, 0)

    private suspend fun fetchPage(type: Int, search: String, skip: Int): Boolean {
        val rows = when (type) {
            PoinItemTable.TYPE_VIOLATION ->
                api.listViolations(PAGE_SIZE, skip, search).data.map { PoinItemTable.from(it) }
            PoinItemTable.TYPE_ACHIEVEMENT ->
                api.listAchievements(PAGE_SIZE, skip, search).data.map { PoinItemTable.from(it) }
            else ->
                api.listHandlings(PAGE_SIZE, skip, search).data.map { PoinItemTable.from(it) }
        }
        dao.insertAll(rows)
        return rows.size >= PAGE_SIZE
    }

    // ---- Teacher: access gate + student search ----

    /** True only when the teacher has roles and *all* of them are "Cooperative" (blocked). Any API
     * failure fails open, as the legacy app does. */
    suspend fun isCooperativeOnly(): Boolean = try {
        val user = sessionStore.user
        val nis = user.nisNik.ifBlank { user.nisnNik }
        val roles = authRepository.checkAccount(nis, sessionStore.school.uuid).data?.roles.orEmpty()
        roles.isNotEmpty() && roles.all { it.name.equals("Cooperative", ignoreCase = true) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        false
    }

    /** `name=query`, plus `nisn=query` when the query is all digits; merged and de-duplicated
     * (`id != 0` → id, else name|nisn|class, because some rows genuinely come back with id 0). */
    suspend fun searchStudents(query: String): List<SearchPoinStudentItem> = coroutineScope {
        val byName = async { api.searchStudentScore(name = query).data }
        val byNisn = if (query.all { it.isDigit() }) async { api.searchStudentScore(nisn = query).data } else null
        (byName.await() + (byNisn?.await().orEmpty()))
            .distinctBy { if (it.id != 0) "id:${it.id}" else "${it.name}|${it.nisn}|${it.student_class}" }
    }

    /** Resolves the picked student to the `user_id` the create endpoints need, and refuses targets
     * that are not students or have no NISN to look them up by (never writes the session). */
    suspend fun checkTarget(student: SearchPoinStudentItem): PoinTarget {
        if (student.nisn.isBlank()) {
            return PoinTarget.Blocked("NISN siswa tidak tersedia sehingga data tidak dapat diproses.")
        }
        val data = authRepository.checkAccount(student.nisn, sessionStore.school.uuid).data
            ?: return PoinTarget.Blocked("Data pengguna tidak ditemukan.")
        val roleNames = data.roles.orEmpty().mapNotNull { it.name }
        val isStudent = if (roleNames.isNotEmpty()) {
            roleNames.any { it.equals("Student", ignoreCase = true) }
        } else {
            student.role.equals("Student", ignoreCase = true)
        }
        if (!isStudent) return PoinTarget.Blocked("User bukan termasuk siswa.")
        return PoinTarget.Ready(userId = data.id, name = data.name.orEmpty().ifBlank { student.name })
    }

    // ---- Teacher: create (all three throw on failure — nothing is swallowed) ----

    suspend fun submitViolation(userId: Int, typeId: Int, message: String, photo: File?) {
        api.createViolation(
            data = mapOf(
                "user_id" to text(userId.toString()),
                "school_violation_id" to text(typeId.toString()),
                "violation_message" to text(message),
            ),
            file = photo?.let { imagePart("violation_image", it) },
        )
    }

    suspend fun submitAchievement(userId: Int, typeId: Int, message: String, photo: File?) {
        api.createAchievement(
            data = mapOf(
                "user_id" to text(userId.toString()),
                "school_achievement_id" to text(typeId.toString()),
                "achievement_message" to text(message),
            ),
            file = photo?.let { imagePart("achievement_image", it) },
        )
    }

    /** [scoreStudentId] is the search item's `id`; [callingAt] is `yyyy-MM-dd HH:mm:ss`. */
    suspend fun submitCalling(scoreStudentId: Int, handlingId: Int, callingAt: String, message: String) {
        api.createCallingStudent(
            mapOf(
                "score_student_id" to scoreStudentId,
                "school_handling_id" to handlingId,
                "calling_at" to callingAt,
                "calling_message" to message,
            ),
        )
    }

    // ---- Student: own score ----

    suspend fun getMyScore(): PoinStudentSummary {
        val item: StudentPoinItem? = api.myScore().data?.firstOrNull()

        val violationRows = item?.violation?.violation_detail.orEmpty()
            .map { PoinEvent("", it.violation_name, it.violation_score ?: 0, it.violation_image, it.violation_message, it.date, it.time, it.creator_name) }
        val achievementRows = item?.achievement?.achievement_detail.orEmpty()
            .map { PoinEvent("", it.achievement_name, it.achievement_score ?: 0, it.achievement_image, it.achievement_message, it.date, it.time, it.creator_name) }

        return PoinStudentSummary(
            violations = violationRows.sortedAndKeyed(),
            achievements = achievementRows.sortedAndKeyed(),
            violationRecap = PoinRecap(
                totalScore = item?.violation?.violation_recap?.violation_score ?: 0,
                rows = item?.violation?.violation_recap?.recap.orEmpty()
                    .map { PoinRecapRow(it.name_of_violation, it.number_of_violations ?: 0, it.number_of_scores ?: 0) },
            ),
            achievementRecap = PoinRecap(
                totalScore = item?.achievement?.achievement_recap?.achievement_score ?: 0,
                rows = item?.achievement?.achievement_recap?.recap.orEmpty()
                    .map { PoinRecapRow(it.name_of_achievement, it.number_of_achievements ?: 0, it.number_of_scores ?: 0) },
            ),
            pendingCalls = item?.notHandled.orEmpty()
                .filter { (it.handled ?: 0) == 0 }
                .map { PoinCall(it.id, it.calling_name, it.calling_at, it.calling_message) },
        )
    }

    /** Newest first by `date`+`time` (both sort lexicographically), keyed by position so two events
     * of the same type never collide. */
    private fun List<PoinEvent>.sortedAndKeyed(): List<PoinEvent> =
        sortedWith(compareByDescending<PoinEvent> { it.date }.thenByDescending { it.time })
            .mapIndexed { index, event -> event.copy(key = "${event.date}_${event.time}_$index") }

    private fun text(value: String): RequestBody = value.toRequestBody("multipart/form-data".toMediaTypeOrNull())

    private fun imagePart(partName: String, file: File): MultipartBody.Part =
        MultipartBody.Part.createFormData(partName, file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))
}
