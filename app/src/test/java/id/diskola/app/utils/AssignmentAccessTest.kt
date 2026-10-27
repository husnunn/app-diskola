package id.diskola.app.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssignmentAccessTest {

    @Test
    fun `a staff account with a teacher record may manage assignments`() {
        assertTrue(AssignmentAccess.canManage(isTeacher = true, isStudent = false, teacherId = 10803))
    }

    @Test
    fun `an account without teacher data may not, whatever the backend calls its type`() {
        assertFalse(AssignmentAccess.canManage(isTeacher = true, isStudent = false, teacherId = 0))
        assertFalse(AssignmentAccess.canManage(isTeacher = true, isStudent = false, teacherId = -1))
    }

    @Test
    fun `a student never may, even with an id`() {
        assertFalse(AssignmentAccess.canManage(isTeacher = false, isStudent = true, teacherId = 5))
        assertFalse(AssignmentAccess.canManage(isTeacher = true, isStudent = true, teacherId = 5))
    }

    @Test
    fun `an account that is not a teacher may not`() {
        assertFalse(AssignmentAccess.canManage(isTeacher = false, isStudent = false, teacherId = 5))
    }

    @Test
    fun `only a positive id counts as teacher data`() {
        assertTrue(AssignmentAccess.hasTeacherData(1))
        assertFalse(AssignmentAccess.hasTeacherData(0))
        assertFalse(AssignmentAccess.hasTeacherData(-3))
    }

    @Test
    fun `the texts shown to a blocked account are not empty`() {
        assertTrue(AssignmentAccess.NO_TEACHER_DATA_TITLE.isNotBlank())
        assertTrue(AssignmentAccess.NO_TEACHER_DATA_BODY.isNotBlank())
        assertTrue(AssignmentAccess.NO_TEACHER_DATA_TOAST.isNotBlank())
    }
}
