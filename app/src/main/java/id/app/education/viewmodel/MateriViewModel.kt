package id.app.education.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import id.app.education.apiservice.MateriApiService
import id.app.education.dataclass.ResponData.ClassRoomTable
import id.app.education.dataclass.ResponData.MajorItem
import id.app.education.dataclass.ResponData.MapelItem
import id.app.education.dataclass.ResponData.MateriItem
import id.app.education.dataclass.ResponData.UploadMateriResponse
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.MultipartBody
import okhttp3.RequestBody

@HiltViewModel
class MateriViewModel @Inject constructor(
    private val materiApiService: MateriApiService
) : BaseViewModel() {

    private val _materiList = MutableStateFlow<List<MateriItem>>(emptyList())
    val materiList: StateFlow<List<MateriItem>> = _materiList.asStateFlow()

    // khusus guru
    private val _teacherSubjects = MutableStateFlow<List<MapelItem>>(emptyList())
    val teacherSubjects: StateFlow<List<MapelItem>> = _teacherSubjects.asStateFlow()

    // khusus siswa
    private val _studentSubjects = MutableStateFlow<List<MapelItem>>(emptyList())
    val studentSubjects: StateFlow<List<MapelItem>> = _studentSubjects.asStateFlow()

    private val _classes = MutableStateFlow<List<ClassRoomTable>>(emptyList())
    val classes: StateFlow<List<ClassRoomTable>> = _classes.asStateFlow()

    private val _majors = MutableStateFlow<List<MajorItem>>(emptyList())
    val majors: StateFlow<List<MajorItem>> = _majors.asStateFlow()

    private val _uploadResult = MutableStateFlow<UploadMateriResponse?>(null)
    val uploadResult: StateFlow<UploadMateriResponse?> = _uploadResult.asStateFlow()

    private val _deleteSuccess = MutableStateFlow(false)
    val deleteSuccess: StateFlow<Boolean> = _deleteSuccess.asStateFlow()

    // simpan filter terakhir
    private var lastTeacherSubjectId: Int? = null
    private var lastTeacherClassId: Int? = null
    private var lastStudentSubjectId: Int? = null

    fun getMateriTeacher(subjectId: Int? = null, classId: Int? = null) {
        launchWithHandling {
            lastTeacherSubjectId = subjectId
            lastTeacherClassId = classId

            val response = materiApiService.teacherTheory(
                take = 1000,
                skip = 0,
                school_subject = subjectId,
                school_class = classId
            )
            _materiList.value = response.data.distinctBy { it.id }
        }
    }

    fun getMateriStudent(subjectId: Int) {
        launchWithHandling {
            lastStudentSubjectId = subjectId
            val response = materiApiService.studentTheories(subjectId = subjectId)
            _materiList.value = response.data.distinctBy { it.id }
        }
    }

    fun fetchTeacherRequirements() {
        launchWithHandling {
            val subjectsRes = materiApiService.teacherSubject(take = 1000, skip = 0)
            val classesRes = materiApiService.assignmentClass()
            val majorsRes = materiApiService.teacherMajor()

            _teacherSubjects.value = subjectsRes.data
            _classes.value = classesRes.data
            _majors.value = majorsRes.data
        }
    }

    fun fetchStudentSubjects() {
        launchWithHandling {
            val response = materiApiService.studentSubjects(take = 1000, skip = 0)
            _studentSubjects.value = response.data
        }
    }

    fun uploadMateri(
        data: Map<String, RequestBody>,
        file: MultipartBody.Part?
    ) {
        launchWithHandling {
            val response = materiApiService.createTheory(data, file)
            _uploadResult.value = response
        }
    }

    fun updateMateri(
        id: Int,
        data: Map<String, RequestBody>,
        file: MultipartBody.Part?
    ) {
        launchWithHandling {
            val response = materiApiService.updateTheory(id, data, file)
            _uploadResult.value = response
        }
    }

    fun deleteMateri(id: Long, isStudent: Boolean = false) {
        launchWithHandling {
            materiApiService.deleteTheory(id)
            _deleteSuccess.value = true

            if (isStudent) {
                lastStudentSubjectId?.let { getMateriStudent(it) }
            } else {
                getMateriTeacher(lastTeacherSubjectId, lastTeacherClassId)
            }
        }
    }

    fun refreshMateri(isStudent: Boolean) {
        if (isStudent) {
            lastStudentSubjectId?.let { getMateriStudent(it) }
        } else {
            getMateriTeacher(lastTeacherSubjectId, lastTeacherClassId)
        }
    }
}
