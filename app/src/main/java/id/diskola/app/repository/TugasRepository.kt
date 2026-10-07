package id.diskola.app.repository

import id.diskola.app.apiservice.TugasApiService
import id.diskola.app.dataclass.ResponData.AssignmentData
import id.diskola.app.dataclass.ResponData.AssignmentDay
import id.diskola.app.dataclass.ResponData.AssignmentSchedule
import id.diskola.app.dataclass.ResponData.HomeworkAnswerFileTable
import id.diskola.app.dataclass.ResponData.HomeworkCollected
import id.diskola.app.dataclass.ResponData.HomeworkCreateResponse
import id.diskola.app.dataclass.ResponData.HomeworkTable
import id.diskola.app.dataclass.localDb.HomeworkDao
import id.diskola.app.utils.session.SessionStore
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import okhttp3.MultipartBody
import okhttp3.RequestBody

private const val PAGE_SIZE = 10

/**
 * Tugas's local-only 10/page tabs (doc `05-pembelajaran-materi-tugas.md` §5.5/§6) — same
 * `ensureFirstPage`/`loadMore`/`refresh` shape as `MateriRepository`/`SchoolRepository`. One
 * `homework` table serves student tabs (`type`-scoped) and the teacher's own list (Kelas/Mapel
 * AND-filtered), same denormalized-no-@Relation pattern as `MateriTable`.
 */
class TugasRepository @Inject constructor(
    private val tugasApiService: TugasApiService,
    private val homeworkDao: HomeworkDao,
    private val sessionStore: SessionStore,
) {
    // ---- Student tabs (Belum/Sudah/Nilai) ----

    fun observeBacklog(): Flow<List<HomeworkTable>> = homeworkDao.observeByType(HomeworkTable.TYPE_BACKLOG)
    fun observeDone(): Flow<List<HomeworkTable>> = homeworkDao.observeByType(HomeworkTable.TYPE_DONE)
    fun observeScored(): Flow<List<HomeworkTable>> = homeworkDao.observeByType(HomeworkTable.TYPE_SCORED)

    suspend fun ensureBacklogFirstPage() {
        if (homeworkDao.countByType(HomeworkTable.TYPE_BACKLOG) == 0) fetchBacklogPage(0)
    }
    suspend fun loadMoreBacklog(currentCount: Int): Boolean = fetchBacklogPage(currentCount)
    suspend fun refreshBacklog(): Boolean = fetchBacklogPage(0)

    private suspend fun fetchBacklogPage(skip: Int): Boolean {
        val response = tugasApiService.studentAssignmentBacklog(take = PAGE_SIZE, skip = skip)
        insertForSchool(response.data, HomeworkTable.TYPE_BACKLOG)
        return response.data.size >= PAGE_SIZE
    }

    suspend fun ensureDoneFirstPage() {
        if (homeworkDao.countByType(HomeworkTable.TYPE_DONE) == 0) fetchDonePage(0)
    }
    suspend fun loadMoreDone(currentCount: Int): Boolean = fetchDonePage(currentCount)
    suspend fun refreshDone(): Boolean = fetchDonePage(0)

    private suspend fun fetchDonePage(skip: Int): Boolean {
        val response = tugasApiService.studentAssignmentDone(take = PAGE_SIZE, skip = skip)
        insertForSchool(response.data, HomeworkTable.TYPE_DONE)
        return response.data.size >= PAGE_SIZE
    }

    suspend fun ensureScoredFirstPage() {
        if (homeworkDao.countByType(HomeworkTable.TYPE_SCORED) == 0) fetchScoredPage(0)
    }
    suspend fun loadMoreScored(currentCount: Int): Boolean = fetchScoredPage(currentCount)
    suspend fun refreshScored(): Boolean = fetchScoredPage(0)

    private suspend fun fetchScoredPage(skip: Int): Boolean {
        val response = tugasApiService.studentAssignmentScored(take = PAGE_SIZE, skip = skip)
        insertForSchool(response.data, HomeworkTable.TYPE_SCORED)
        return response.data.size >= PAGE_SIZE
    }

    /** Doc §5.4: only rows matching the current session's school are kept. */
    private suspend fun insertForSchool(items: List<id.diskola.app.dataclass.ResponData.HomeworkItem>, type: Int) {
        val schoolUuid = sessionStore.school.uuid
        val filtered = items.filter { it.school?.uuid.isNullOrBlank() || it.school?.uuid == schoolUuid }
        homeworkDao.insertAll(filtered.map { HomeworkTable.fromHomeworkItem(it, type).copy(school_uuid = schoolUuid) })
    }

    // ---- Detail (student + teacher read-only "Lihat Tugas") ----

    suspend fun getCachedTugas(id: Int): HomeworkTable? = homeworkDao.getById(id)

    fun observeAnswerFiles(tugasId: Int): Flow<List<HomeworkAnswerFileTable>> = homeworkDao.observeAnswerFiles(tugasId)

    suspend fun ensureAnswerFiles(tugasId: Int, subjectAssignmentId: Int, studentAssignmentId: Int) {
        if (studentAssignmentId <= 0) return
        if (homeworkDao.countAnswerFiles(tugasId) > 0) return
        val response = tugasApiService.getAssignmentAnswerDetail(subjectAssignmentId, studentAssignmentId)
        homeworkDao.insertAnswerFiles(response.data.files.map { HomeworkAnswerFileTable.from(tugasId, it) })
    }

    /** Called when a student clears every answer file/link — keeps the "upload" requirement
     * reactive instead of a one-way latch (decision: fix, don't replicate the legacy bug). */
    suspend fun clearAnswerFiles(tugasId: Int) = homeworkDao.clearAnswerFiles(tugasId)

    // ---- Submit ("Kirim Tugas") ----

    suspend fun collectAssignment(
        id: Int,
        data: Map<String, RequestBody>,
        files: List<MultipartBody.Part>,
    ) {
        tugasApiService.collectAssignment(id, data, files)
        homeworkDao.deleteById(id) // doc §5.5: sukses -> hilang dari tab Belum
    }

    // ---- "List Tugas" guru ----

    fun observeTeacherOwn(teacherId: Int, subjectId: Int?, classId: Int?): Flow<List<HomeworkTable>> =
        homeworkDao.observeByTeacher(teacherId, subjectId, classId)

    suspend fun ensureTeacherOwnFirstPage(teacherId: Int, subjectId: Int?, classId: Int?) {
        if (homeworkDao.countByTeacher(teacherId, subjectId, classId) == 0) {
            fetchTeacherOwnPage(teacherId, subjectId, classId, 0)
        }
    }
    suspend fun loadMoreTeacherOwn(teacherId: Int, subjectId: Int?, classId: Int?, currentCount: Int): Boolean =
        fetchTeacherOwnPage(teacherId, subjectId, classId, currentCount)
    suspend fun refreshTeacherOwn(teacherId: Int, subjectId: Int?, classId: Int?): Boolean {
        homeworkDao.clearByTeacher(teacherId)
        return fetchTeacherOwnPage(teacherId, subjectId, classId, 0)
    }

    /** Filter kirim literal `0` untuk "Semua" (bukan dihilangkan) — tiru app lama persis, keputusan
     * user eksplisit (bukan bug yang diperbaiki di sini). */
    private suspend fun fetchTeacherOwnPage(teacherId: Int, subjectId: Int?, classId: Int?, skip: Int): Boolean {
        val filter = buildMap {
            put("take", PAGE_SIZE.toString())
            put("skip", skip.toString())
            put("filter[0][0]", "teacher_id")
            put("filter[0][1]", "=")
            put("filter[0][2]", teacherId.toString())
            put("filter[1]", "and")
            put("filter[2][0]", "school_classes_id")
            put("filter[2][1]", "=")
            put("filter[2][2]", (classId ?: 0).toString())
            put("filter[3]", "and")
            put("filter[4][0]", "school_subject_id")
            put("filter[4][1]", "=")
            put("filter[4][2]", (subjectId ?: 0).toString())
        }
        val response = tugasApiService.teacherAssignmentBacklog(filter)
        homeworkDao.insertAll(response.data.map { HomeworkTable.fromHomeworkItem(it, HomeworkTable.TYPE_BACKLOG).copy(school_uuid = sessionStore.school.uuid) })
        return response.data.size >= PAGE_SIZE
    }

    // ---- Form buat/edit ----

    suspend fun fetchScheduleDays(): List<AssignmentDay> = tugasApiService.teacherAssignmentScheduleDay().data

    suspend fun fetchSchedule(classId: Int, day: String): List<AssignmentSchedule> =
        tugasApiService.teacherAssignmentSchedule(classId, day).data

    suspend fun createTugas(data: Map<String, RequestBody>, file: MultipartBody.Part?): HomeworkCreateResponse =
        tugasApiService.createAssignment(data, file)

    suspend fun updateTugas(id: Int, data: Map<String, RequestBody>, file: MultipartBody.Part?): HomeworkCreateResponse =
        tugasApiService.updateAssignment(id, data, file)

    suspend fun deleteTugas(id: Int) {
        tugasApiService.deleteAssignment(id)
        homeworkDao.deleteById(id)
    }

    // ---- Penilaian & Tugas Terkumpul & Scoring ----

    fun observeScoredGroups(): Flow<List<HomeworkCollected>> = homeworkDao.observeCollected()

    suspend fun ensureScoredGroupsFirstPage() {
        if (homeworkDao.countCollected() == 0) fetchScoredGroupsPage(0)
    }
    suspend fun loadMoreScoredGroups(currentCount: Int): Boolean = fetchScoredGroupsPage(currentCount)
    suspend fun refreshScoredGroups(): Boolean {
        homeworkDao.clearCollected()
        return fetchScoredGroupsPage(0)
    }

    private suspend fun fetchScoredGroupsPage(skip: Int): Boolean {
        val response = tugasApiService.teacherAssignmentScoredGroups(take = PAGE_SIZE, skip = skip)
        homeworkDao.insertCollected(response.data)
        return response.data.size >= PAGE_SIZE
    }

    /** Selalu network, tanpa cache Room — detail per-nilai ini berubah tiap kali guru memberi
     * nilai, tidak ada manfaat menyimpannya lokal (beda dengan list Tugas yang memang di-cache). */
    suspend fun getScoredDetail(id: Int): AssignmentData = tugasApiService.teacherAssignmentScoredDetail(id).data

    suspend fun saveScore(collectedId: Int, assignmentId: Int, score: Int) {
        tugasApiService.saveAssignmentScore(collectedId, assignmentId, mapOf("score" to score))
    }
}
