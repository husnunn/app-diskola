package id.diskola.app.database

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The migrations create the exam tables by hand; Room refuses to open the database if they differ from what it expects (and, with
 * `fallbackToDestructiveMigration` still on, a wrong migration would silently wipe unsent answers). Room exports that expectation to
 * `app/schemas`, so the two are compared here instead of finding out on a student's phone.
 */
class AkmMigrationTest {

    private fun schema(version: Int): Map<String, Any?> {
        val file = listOf("schemas", "app/schemas")
            .map { File(it, "id.diskola.app.database.LocalDatabase/$version.json") }
            .first { it.exists() }
        val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        return Moshi.Builder().build().adapter<Map<String, Any?>>(type).fromJson(file.readText())!!
    }

    private val v8 by lazy { schema(8) }
    private val v9 by lazy { schema(9) }
    private val v10 by lazy { schema(10) }

    @Suppress("UNCHECKED_CAST")
    private fun entity(schema: Map<String, Any?>, table: String): Map<String, Any?> {
        val database = schema["database"] as Map<String, Any?>
        val entities = database["entities"] as List<Map<String, Any?>>
        return entities.first { it["tableName"] == table }
    }

    private fun createSql(schema: Map<String, Any?>, table: String): String =
        (entity(schema, table)["createSql"] as String).replace("\${TABLE_NAME}", table)

    @Suppress("UNCHECKED_CAST")
    private fun indexSql(schema: Map<String, Any?>, table: String, index: String): String {
        val indices = entity(schema, table)["indices"] as List<Map<String, Any?>>
        return (indices.first { it["name"] == index }["createSql"] as String).replace("\${TABLE_NAME}", table)
    }

    // ---- v7 -> v8 ----

    @Test
    fun `answer table matches the schema Room exports`() = assertEquals(createSql(v8, "akm_answer"), AkmMigrationSql.ANSWER)

    @Test
    fun `submission table matches the schema Room exports`() = assertEquals(createSql(v8, "akm_submission"), AkmMigrationSql.SUBMISSION)

    @Test
    fun `schedule cache table matches the schema Room exports`() = assertEquals(createSql(v8, "akm_schedule_cache"), AkmMigrationSql.SCHEDULE_CACHE)

    @Test
    fun `the added columns appear in the exported exam table with the same definition`() {
        val sql = createSql(v8, "akm_synced_exam")
        assertTrue(sql.contains("`studentExamId` INTEGER NOT NULL DEFAULT 0"))
        assertTrue(sql.contains("`endAtMillis` INTEGER"))
        assertTrue(AkmMigrationSql.ADD_STUDENT_EXAM_ID.endsWith("studentExamId INTEGER NOT NULL DEFAULT 0"))
        assertTrue(AkmMigrationSql.ADD_END_AT.endsWith("endAtMillis INTEGER"))
    }

    @Test
    fun `the first migration spans exactly 7 to 8`() {
        assertEquals(7, MIGRATION_7_8.startVersion)
        assertEquals(8, MIGRATION_7_8.endVersion)
    }

    // ---- v8 -> v9 ----

    @Test
    fun `violation table and its index match the schema Room exports`() {
        assertEquals(createSql(v9, "akm_violation"), AkmMigrationSql.VIOLATION)
        assertEquals(indexSql(v9, "akm_violation", "index_akm_violation_examId"), AkmMigrationSql.VIOLATION_INDEX)
    }

    @Test
    fun `problem report table and its index match the schema Room exports`() {
        assertEquals(createSql(v9, "akm_problem_report"), AkmMigrationSql.PROBLEM_REPORT)
        assertEquals(indexSql(v9, "akm_problem_report", "index_akm_problem_report_examId"), AkmMigrationSql.PROBLEM_REPORT_INDEX)
    }

    @Test
    fun `version 9 keeps every table of version 8 unchanged`() {
        listOf("akm_answer", "akm_submission", "akm_schedule_cache", "akm_synced_exam").forEach { table ->
            assertEquals(table, createSql(v8, table), createSql(v9, table))
        }
    }

    @Test
    fun `the second migration spans exactly 8 to 9`() {
        assertEquals(8, MIGRATION_8_9.startVersion)
        assertEquals(9, MIGRATION_8_9.endVersion)
    }

    // ---- v9 -> v10 (teacher-task cache) ----

    @Test
    fun `the added homework column appears in the exported table with the same definition`() {
        assertTrue(createSql(v10, "homework").contains("`schedule_day` TEXT NOT NULL DEFAULT ''"))
        assertTrue(HomeworkMigrationSql.ADD_SCHEDULE_DAY.endsWith("schedule_day TEXT NOT NULL DEFAULT ''"))
        assertTrue(HomeworkMigrationSql.ADD_SCHEDULE_DAY.startsWith("ALTER TABLE homework ADD COLUMN"))
    }

    @Test
    fun `version 10 changes the homework table and nothing else — the exam tables are untouched`() {
        @Suppress("UNCHECKED_CAST")
        val tables = ((v9["database"] as Map<String, Any?>)["entities"] as List<Map<String, Any?>>).map { it["tableName"] as String }
        assertTrue(tables.contains("akm_violation") && tables.contains("akm_answer"))
        tables.filter { it != "homework" }.forEach { table ->
            assertEquals(table, createSql(v9, table), createSql(v10, table))
        }
    }

    @Test
    fun `the third migration spans exactly 9 to 10`() {
        assertEquals(9, MIGRATION_9_10.startVersion)
        assertEquals(10, MIGRATION_9_10.endVersion)
    }
}
