package id.diskola.app.repository

import id.diskola.app.apiservice.MateriApiService
import id.diskola.app.dataclass.ResponData.ClassRoomTable
import id.diskola.app.dataclass.ResponData.GradeTable
import id.diskola.app.dataclass.ResponData.MajorItem
import id.diskola.app.dataclass.ResponData.MapelTable
import id.diskola.app.dataclass.ResponData.MateriTable
import id.diskola.app.dataclass.localDb.MapelDao
import id.diskola.app.dataclass.localDb.MateriDao
import id.diskola.app.dataclass.ResponData.UploadMateriResponse
import id.diskola.app.dataclass.localDb.TeacherReferenceDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import okhttp3.MultipartBody
import okhttp3.RequestBody

private const val PAGE_SIZE = 20

/**
 * Materi's local-only search + 20/page infinite scroll (doc `05-pembelajaran-materi-tugas.md`
 * §2.4/§3.4) — the same `ensureFirstPage`/`loadNextPage`/`refresh` shape as `SchoolRepository`
 * (auth phase). One `mapel`/`materi` Room table serves both roles; which endpoint fills it depends
 * on which `fetch*`/`ensure*` function the caller (student vs teacher screen) uses.
 */
class MateriRepository @Inject constructor(
    private val materiApiService: MateriApiService,
    private val mapelDao: MapelDao,
    private val materiDao: MateriDao,
    private val teacherReferenceDao: TeacherReferenceDao,
) {
    // ---- Subject list (student `TheoryPage` / teacher's own "Mata Pelajaran") ----

    fun observeSubjects(query: String): Flow<List<MapelTable>> = mapelDao.observe(query.trim())

    suspend fun ensureStudentSubjectsFirstPage() {
        if (mapelDao.count() == 0) fetchStudentSubjectsPage(0)
    }

    suspend fun loadMoreStudentSubjects(currentCount: Int): Boolean = fetchStudentSubjectsPage(currentCount)

    suspend fun refreshStudentSubjects(): Boolean {
        mapelDao.clear()
        return fetchStudentSubjectsPage(0)
    }

    private suspend fun fetchStudentSubjectsPage(skip: Int): Boolean {
        val response = materiApiService.studentSubjects(take = PAGE_SIZE, skip = skip)
        mapelDao.insertAll(response.data.map { MapelTable.fromMapelItem(it) })
        return response.data.size >= PAGE_SIZE
    }

    suspend fun ensureTeacherSubjectsFirstPage() {
        if (mapelDao.count() == 0) fetchTeacherSubjectsPage(0)
    }

    suspend fun loadMoreTeacherSubjects(currentCount: Int): Boolean = fetchTeacherSubjectsPage(currentCount)

    suspend fun refreshTeacherSubjects(): Boolean {
        mapelDao.clear()
        return fetchTeacherSubjectsPage(0)
    }

    private suspend fun fetchTeacherSubjectsPage(skip: Int): Boolean {
        val response = materiApiService.teacherSubject(take = PAGE_SIZE, skip = skip)
        mapelDao.insertAll(response.data.map { MapelTable.fromMapelItem(it) })
        return response.data.size >= PAGE_SIZE
    }

    // ---- Materi within one subject (`MateriPage`, both roles) ----

    fun observeMateriBySubject(subjectId: Int): Flow<List<MateriTable>> = materiDao.observeBySubject(subjectId)

    suspend fun ensureSubjectMateriFirstPage(subjectId: Int, isTeacher: Boolean) {
        if (materiDao.countBySubject(subjectId) == 0) fetchSubjectMateriPage(subjectId, isTeacher, 0)
    }

    suspend fun loadMoreSubjectMateri(subjectId: Int, isTeacher: Boolean, currentCount: Int): Boolean =
        fetchSubjectMateriPage(subjectId, isTeacher, currentCount)

    suspend fun refreshSubjectMateri(subjectId: Int, isTeacher: Boolean): Boolean {
        materiDao.clearBySubject(subjectId)
        return fetchSubjectMateriPage(subjectId, isTeacher, 0)
    }

    private suspend fun fetchSubjectMateriPage(subjectId: Int, isTeacher: Boolean, skip: Int): Boolean {
        val response = if (isTeacher) {
            materiApiService.teacherTheoryBySubject(subjectId, take = PAGE_SIZE, skip = skip)
        } else {
            materiApiService.studentTheories(subjectId, take = PAGE_SIZE, skip = skip)
        }
        materiDao.insertAll(response.data.map { MateriTable.fromMateriItem(it) })
        return response.data.size >= PAGE_SIZE
    }

    // ---- Teacher's own "Materi Saya" — Kelas/Mapel filters combined with AND (decision: fix the
    // legacy exclusive-filter behaviour rather than replicate it). ----

    fun observeTeacherOwnMateri(teacherId: Int, subjectId: Int?, classId: Int?): Flow<List<MateriTable>> =
        materiDao.observeByTeacher(teacherId, subjectId, classId)

    suspend fun ensureTeacherOwnFirstPage(teacherId: Int, subjectId: Int?, classId: Int?) {
        if (materiDao.countByTeacher(teacherId, subjectId, classId) == 0) fetchTeacherOwnPage(subjectId, classId, 0)
    }

    suspend fun loadMoreTeacherOwn(subjectId: Int?, classId: Int?, currentCount: Int): Boolean =
        fetchTeacherOwnPage(subjectId, classId, currentCount)

    /** Also used right after create/edit/delete succeeds — fixes the legacy bug where a newly
     * created/edited materi never showed up without a manual pull-to-refresh (doc §3.8). */
    suspend fun refreshTeacherOwn(teacherId: Int, subjectId: Int?, classId: Int?): Boolean {
        materiDao.clearByTeacher(teacherId)
        return fetchTeacherOwnPage(subjectId, classId, 0)
    }

    private suspend fun fetchTeacherOwnPage(subjectId: Int?, classId: Int?, skip: Int): Boolean {
        val response = materiApiService.teacherTheory(take = PAGE_SIZE, skip = skip, school_subject = subjectId, school_class = classId)
        materiDao.insertAll(response.data.map { MateriTable.fromMateriItem(it) })
        return response.data.size >= PAGE_SIZE
    }

    // ---- Detail (`MateriDetailPage`) — Room first, API only if not cached; never re-fetched once
    // cached, matching doc §2.4 exactly. ----

    suspend fun getDetail(materiId: Int, subjectId: Int, isTeacher: Boolean): MateriTable? {
        materiDao.getById(materiId)?.let { return it }
        if (materiId <= 0 || subjectId <= 0) return null
        return try {
            val response = if (isTeacher) {
                materiApiService.teacherTheoryDetail(subjectId, materiId)
            } else {
                materiApiService.studentTheoryDetail(subjectId, materiId)
            }
            val table = MateriTable.fromMateriItem(response.data)
            materiDao.insertAll(listOf(table))
            table
        } catch (e: Exception) {
            null
        }
    }

    /** Local-cache-only lookup for edit-mode prefill — the row is already cached from whichever
     * list screen the user tapped "Edit" from, so no network round trip is needed here. */
    suspend fun getCachedMateri(id: Int): MateriTable? = materiDao.getById(id)

    suspend fun deleteMateri(id: Int) {
        materiApiService.deleteTheory(id.toLong())
        materiDao.deleteById(id)
    }

    suspend fun createMateri(data: Map<String, RequestBody>, file: MultipartBody.Part?): UploadMateriResponse =
        materiApiService.createTheory(data, file)

    suspend fun updateMateri(id: Int, data: Map<String, RequestBody>, file: MultipartBody.Part?): UploadMateriResponse =
        materiApiService.updateTheory(id, data, file)

    // ---- Teacher reference lists (upload form + "Materi Saya" filter dropdowns) — small,
    // unpaged, always replaced on open (doc §3.4: `assignmentClass` "selalu dipanggil saat buka"). ----

    fun observeClasses(): Flow<List<ClassRoomTable>> = teacherReferenceDao.observeClasses()
    fun observeMajors(): Flow<List<MajorItem>> = teacherReferenceDao.observeMajors()
    fun observeGrades(): Flow<List<GradeTable>> = teacherReferenceDao.observeGrades()

    /** "Materi Saya" Kelas filter — doc §3.4: `mobile/app/learning/assignment/teachers/class`. */
    suspend fun refreshClasses() {
        val response = materiApiService.assignmentClass()
        teacherReferenceDao.clearClasses()
        teacherReferenceDao.insertClasses(response.data)
    }

    /** Upload form "Ditampilkan ke → Kelas" dropdown — doc §3.4 last paragraph: a *different*
     * endpoint (`mobile/teacher/school-class-room`) than the filter above, kept distinct rather
     * than reusing `assignmentClass()` like the current app did. Shares the same `classroom` cache
     * table since both return the same shape. */
    suspend fun refreshFormClasses() {
        val response = materiApiService.teacherSchoolClassRoom()
        teacherReferenceDao.clearClasses()
        teacherReferenceDao.insertClasses(response.data)
    }

    suspend fun refreshMajors() {
        val response = materiApiService.teacherMajor()
        teacherReferenceDao.clearMajors()
        teacherReferenceDao.insertMajors(response.data)
    }

    suspend fun refreshGrades() {
        val response = materiApiService.teacherSchoolGrade()
        teacherReferenceDao.clearGrades()
        teacherReferenceDao.insertGrades(response.data)
    }
}
