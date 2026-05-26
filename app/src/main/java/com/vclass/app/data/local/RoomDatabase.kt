package com.vclass.app.data.local

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ========== ENTITIES ==========
@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey val id: Int,
    val shortname: String,
    val fullname: String,
    val displayname: String?,
    val progress: Double?,
    val completed: Boolean?,
    val startdate: Long?,
    val enddate: Long?,
    val lastaccess: Long?,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "assignments")
data class AssignmentEntity(
    @PrimaryKey val id: Int,
    val cmid: Int,
    val courseId: Int,
    val name: String,
    val duedate: Long?,
    val allowsubmissionsfromdate: Long?,
    val grade: Int?,
    val intro: String?,
    val submissionStatus: String?,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "forums")
data class ForumEntity(
    @PrimaryKey val id: Int,
    val courseId: Int,
    val type: String,
    val name: String,
    val intro: String?,
    val duedate: Long?,
    val numdiscussions: Int?,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String?,
    val courseId: Int?,
    val courseName: String?,
    val timestart: Long,
    val timeduration: Long?,
    val eventtype: String?,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "grades")
data class GradeEntity(
    @PrimaryKey val id: String,
    val courseId: Int,
    val courseName: String,
    val itemName: String,
    val grade: String?,
    val weight: String?,
    val percentage: String?,
    val feedback: String?,
    val cachedAt: Long = System.currentTimeMillis()
)

// ========== DAO ==========
@Dao
abstract class VClassDao {

    // Courses
    @Query("SELECT * FROM courses ORDER BY lastaccess DESC")
    abstract fun getAllCourses(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE id = :courseId")
    abstract suspend fun getCourseById(courseId: Int): CourseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertCourses(courses: List<CourseEntity>)

    @Query("DELETE FROM courses")
    abstract suspend fun clearCourses()

    // Assignments
    @Query("SELECT * FROM assignments ORDER BY duedate ASC")
    abstract fun getAllAssignments(): Flow<List<AssignmentEntity>>

    @Query("SELECT * FROM assignments WHERE courseId = :courseId ORDER BY duedate ASC")
    abstract fun getAssignmentsByCourse(courseId: Int): Flow<List<AssignmentEntity>>

    @Query("SELECT * FROM assignments WHERE submissionStatus != 'submitted' ORDER BY duedate ASC")
    abstract fun getUnsubmittedAssignments(): Flow<List<AssignmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAssignments(assignments: List<AssignmentEntity>)

    @Query("DELETE FROM assignments")
    abstract suspend fun clearAssignments()

    @Query("UPDATE assignments SET submissionStatus = :status WHERE id = :assignmentId")
    abstract suspend fun updateSubmissionStatus(assignmentId: Int, status: String)

    // Forums
    @Query("SELECT * FROM forums ORDER BY name ASC")
    abstract fun getAllForums(): Flow<List<ForumEntity>>

    @Query("SELECT * FROM forums WHERE courseId = :courseId")
    abstract fun getForumsByCourse(courseId: Int): Flow<List<ForumEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertForums(forums: List<ForumEntity>)

    @Query("DELETE FROM forums")
    abstract suspend fun clearForums()

    // Calendar Events
    @Query("SELECT * FROM calendar_events WHERE timestart > :now ORDER BY timestart ASC")
    abstract fun getUpcomingEvents(now: Long = System.currentTimeMillis() / 1000): Flow<List<CalendarEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertEvents(events: List<CalendarEventEntity>)

    @Query("DELETE FROM calendar_events")
    abstract suspend fun clearEvents()

    // Grades
    @Query("SELECT * FROM grades ORDER BY courseId, itemName ASC")
    abstract fun getAllGrades(): Flow<List<GradeEntity>>

    @Query("SELECT * FROM grades WHERE courseId = :courseId ORDER BY itemName ASC")
    abstract fun getGradesByCourse(courseId: Int): Flow<List<GradeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertGrades(grades: List<GradeEntity>)

    @Query("DELETE FROM grades")
    abstract suspend fun clearGrades()

    // Search assignments
    @Query("SELECT * FROM assignments WHERE name LIKE '%' || :query || '%' ORDER BY duedate ASC")
    abstract fun searchAssignments(query: String): Flow<List<AssignmentEntity>>

    // Clear all
    @Transaction
    open suspend fun clearAll() {
        clearCourses()
        clearAssignments()
        clearForums()
        clearEvents()
        clearGrades()
    }
}

// ========== DATABASE ==========
@Database(
    entities = [CourseEntity::class, AssignmentEntity::class, ForumEntity::class, CalendarEventEntity::class, GradeEntity::class],
    version = 2,
    exportSchema = false
)
abstract class VClassDatabase : RoomDatabase() {
    abstract fun dao(): VClassDao

    companion object {
        @Volatile
        private var INSTANCE: VClassDatabase? = null

        fun getInstance(context: Context): VClassDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    VClassDatabase::class.java,
                    "vclass_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
