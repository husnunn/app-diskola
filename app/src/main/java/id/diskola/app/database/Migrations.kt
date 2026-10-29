package id.diskola.app.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The exam tables hold answers a student has not sent yet, so — unlike the rest of this database, which is a re-fetchable cache and
 * may be dropped (`fallbackToDestructiveMigration`) — they get an explicit migration: v7 -> v8 only **adds** tables/columns and keeps
 * every downloaded exam. The `CREATE TABLE` strings equal the DDL Room exports to `app/schemas/.../8.json` and `9.json` (checked by
 * `AkmMigrationTest`). v8 -> v9 adds the two tables of the exam-safety phase: recorded violations and filed problem reports.
 */
object AkmMigrationSql {
    const val ANSWER =
        "CREATE TABLE IF NOT EXISTS `akm_answer` (`examId` INTEGER NOT NULL, `questionId` INTEGER NOT NULL, `instructionId` INTEGER NOT NULL, " +
            "`typeLabel` TEXT NOT NULL, `answerJson` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`examId`, `questionId`))"

    const val SUBMISSION =
        "CREATE TABLE IF NOT EXISTS `akm_submission` (`examId` INTEGER NOT NULL, `state` TEXT NOT NULL, `manual` INTEGER NOT NULL, " +
            "`attempts` INTEGER NOT NULL, `queuedAt` INTEGER NOT NULL, `lastAttemptAt` INTEGER NOT NULL, `sentAt` INTEGER NOT NULL, " +
            "`errorMessage` TEXT NOT NULL, PRIMARY KEY(`examId`))"

    const val SCHEDULE_CACHE =
        "CREATE TABLE IF NOT EXISTS `akm_schedule_cache` (`id` INTEGER NOT NULL, `kind` TEXT NOT NULL, `scheduleJson` TEXT NOT NULL, " +
            "`savedAt` INTEGER NOT NULL, PRIMARY KEY(`id`, `kind`))"

    const val VIOLATION =
        "CREATE TABLE IF NOT EXISTS `akm_violation` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `examId` INTEGER NOT NULL, " +
            "`studentExamId` INTEGER NOT NULL, `type` TEXT NOT NULL, `detectedAtMillis` INTEGER NOT NULL, `metadataJson` TEXT, " +
            "`createdAtElapsed` INTEGER NOT NULL)"

    const val VIOLATION_INDEX = "CREATE INDEX IF NOT EXISTS `index_akm_violation_examId` ON `akm_violation` (`examId`)"

    const val PROBLEM_REPORT =
        "CREATE TABLE IF NOT EXISTS `akm_problem_report` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `examId` INTEGER NOT NULL, " +
            "`studentExamId` INTEGER NOT NULL, `message` TEXT NOT NULL, `violationsJson` TEXT NOT NULL, `isSystemFaultConfirmed` INTEGER NOT NULL, " +
            "`appVersion` TEXT NOT NULL, `androidVersion` TEXT NOT NULL, `deviceName` TEXT NOT NULL, `logcat` TEXT NOT NULL, " +
            "`createdAt` INTEGER NOT NULL, `isSent` INTEGER NOT NULL)"

    const val PROBLEM_REPORT_INDEX = "CREATE INDEX IF NOT EXISTS `index_akm_problem_report_examId` ON `akm_problem_report` (`examId`)"

    const val ADD_STUDENT_EXAM_ID = "ALTER TABLE akm_synced_exam ADD COLUMN studentExamId INTEGER NOT NULL DEFAULT 0"
    const val ADD_END_AT = "ALTER TABLE akm_synced_exam ADD COLUMN endAtMillis INTEGER"

    /** A previous build marked a submitted exam by `downloadStatus = 'SUBMITTED'`; that fact moves to `akm_submission`. */
    const val MOVE_SUBMITTED =
        "INSERT OR IGNORE INTO akm_submission (examId, state, manual, attempts, queuedAt, lastAttemptAt, sentAt, errorMessage) " +
            "SELECT id, 'SENT', 0, 0, 0, 0, 0, '' FROM akm_synced_exam WHERE downloadStatus = 'SUBMITTED'"
    const val RESET_SUBMITTED = "UPDATE akm_synced_exam SET downloadStatus = 'DOWNLOADED' WHERE downloadStatus = 'SUBMITTED'"
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(AkmMigrationSql.ANSWER)
        db.execSQL(AkmMigrationSql.SUBMISSION)
        db.execSQL(AkmMigrationSql.SCHEDULE_CACHE)
        db.execSQL(AkmMigrationSql.ADD_STUDENT_EXAM_ID)
        db.execSQL(AkmMigrationSql.ADD_END_AT)
        db.execSQL(AkmMigrationSql.MOVE_SUBMITTED)
        db.execSQL(AkmMigrationSql.RESET_SUBMITTED)
    }
}

/**
 * v9 -> v10 adds one column to the teacher-task cache so that editing a task made for a schedule can load that schedule's list again
 * (`schedule.day`). It is an explicit migration for a reason that is easy to miss: `fallbackToDestructiveMigration` is still on, and a
 * database version without a migration is **wiped whole** — including the exam answers and violations that have not been sent yet.
 */
object HomeworkMigrationSql {
    const val ADD_SCHEDULE_DAY = "ALTER TABLE homework ADD COLUMN schedule_day TEXT NOT NULL DEFAULT ''"
}

/** v10 -> v11: the two payment tables of Bayar SPP (P2). Only new tables, so nothing already on the phone is touched. */
object PaymentMigrationSql {
    const val INVOICE =
        "CREATE TABLE IF NOT EXISTS `payment_invoice` (`trx_id` TEXT NOT NULL, `type` TEXT NOT NULL, `product_name` TEXT NOT NULL, `channel_method_name` TEXT NOT NULL, `image_url` TEXT NOT NULL, `payment_code` TEXT NOT NULL, `partner_reff2` TEXT NOT NULL, `admin_fee` INTEGER NOT NULL, `amount` INTEGER NOT NULL, `total_amount` INTEGER NOT NULL, `note` TEXT NOT NULL, `created_at` TEXT NOT NULL, `expired_at` TEXT NOT NULL, `paid_at` TEXT NOT NULL, `is_paid` INTEGER NOT NULL, `cancelable` INTEGER NOT NULL, `status` TEXT NOT NULL, PRIMARY KEY(`trx_id`))"
    const val GUIDE =
        "CREATE TABLE IF NOT EXISTS `payment_guide` (`channel` TEXT NOT NULL, `sectionsJson` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`channel`))"
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(PaymentMigrationSql.INVOICE)
        db.execSQL(PaymentMigrationSql.GUIDE)
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(HomeworkMigrationSql.ADD_SCHEDULE_DAY)
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(AkmMigrationSql.VIOLATION)
        db.execSQL(AkmMigrationSql.VIOLATION_INDEX)
        db.execSQL(AkmMigrationSql.PROBLEM_REPORT)
        db.execSQL(AkmMigrationSql.PROBLEM_REPORT_INDEX)
    }
}
