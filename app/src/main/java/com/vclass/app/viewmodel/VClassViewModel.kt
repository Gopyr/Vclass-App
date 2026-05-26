package com.vclass.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vclass.app.data.local.VClassDatabase
import com.vclass.app.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import com.vclass.app.data.repository.MoodleRepository

// ========== UI STATE ==========
data class DashboardUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val courses: List<Course> = emptyList(),
    val assignments: List<Assignment> = emptyList(),
    val calendarEvents: List<CalendarEvent> = emptyList(),
    val submissionStatuses: Map<Int, SubmissionStatusResponse> = emptyMap(),
    val announcements: List<ForumDiscussion> = emptyList(),
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val isOffline: Boolean = false,
    val unreadCount: Int = 0
)

data class CourseDetailUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val course: Course? = null,
    val sections: List<CourseSection> = emptyList(),
    val assignments: List<Assignment> = emptyList(),
    val forums: List<Forum> = emptyList(),
    val submissionStatuses: Map<Int, SubmissionStatusResponse> = emptyMap(),
    val errorMessage: String? = null
)

data class ForumDetailUiState(
    val isLoading: Boolean = true,
    val discussions: List<ForumDiscussion> = emptyList(),
    val errorMessage: String? = null
)

data class GradesUiState(
    val isLoading: Boolean = true,
    val grades: List<GradeItem> = emptyList(),
    val errorMessage: String? = null
)

data class QuizDetailUiState(
    val isLoading: Boolean = true,
    val attempts: List<QuizAttemptInfo> = emptyList(),
    val accessInfo: QuizAccessResponse? = null,
    val errorMessage: String? = null
)

data class ProfileUiState(
    val isLoading: Boolean = true,
    val siteInfo: SiteInfo? = null,
    val errorMessage: String? = null
)

data class GradeItem(
    val courseName: String,
    val itemName: String,
    val grade: String?,
    val weight: String?,
    val percentage: String?,
    val feedback: String?
)

// ========== VIEWMODEL ==========
class VClassViewModel(application: Application) : AndroidViewModel(application) {

    val repository = MoodleRepository()
    private val db = VClassDatabase.getInstance(application)
    private val dao = db.dao()

    // Dashboard state
    private val _dashboardState = MutableStateFlow(DashboardUiState())
    val dashboardState: StateFlow<DashboardUiState> = _dashboardState.asStateFlow()

    // Course detail state
    private val _courseDetailState = MutableStateFlow(CourseDetailUiState())
    val courseDetailState: StateFlow<CourseDetailUiState> = _courseDetailState.asStateFlow()

    // Selected assignment for detail view
    private val _selectedAssignment = MutableStateFlow<Assignment?>(null)
    val selectedAssignment: StateFlow<Assignment?> = _selectedAssignment.asStateFlow()

    // Counter to force re-fetch even when same assignment is re-selected
    private val _selectionCounter = MutableStateFlow(0)
    val selectionCounter: StateFlow<Int> = _selectionCounter.asStateFlow()

    private val _selectedSubmissionStatus = MutableStateFlow<SubmissionStatusResponse?>(null)
    val selectedSubmissionStatus: StateFlow<SubmissionStatusResponse?> = _selectedSubmissionStatus.asStateFlow()

    // Forum detail state
    private val _forumDetailState = MutableStateFlow(ForumDetailUiState())
    val forumDetailState: StateFlow<ForumDetailUiState> = _forumDetailState.asStateFlow()

    // Grades state
    private val _gradesState = MutableStateFlow(GradesUiState())
    val gradesState: StateFlow<GradesUiState> = _gradesState.asStateFlow()

    // Quiz detail state
    private val _quizDetailState = MutableStateFlow(QuizDetailUiState())
    val quizDetailState: StateFlow<QuizDetailUiState> = _quizDetailState.asStateFlow()

    // Profile state
    private val _profileState = MutableStateFlow(ProfileUiState())
    val profileState: StateFlow<ProfileUiState> = _profileState.asStateFlow()

    // Dark mode
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // Job for periodic submission status refresh
    private var submissionRefreshJob: Job? = null

    init {
        loadDashboard()
        // Start periodic submission status refresh for selected assignment
        startSubmissionStatusRefresh()
    }

    override fun onCleared() {
        super.onCleared()
        // Stop the refresh job when ViewModel is cleared
        submissionRefreshJob?.cancel()
    }

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
    }

    // ========== DASHBOARD ==========
    fun loadDashboard(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _dashboardState.value = _dashboardState.value.copy(isRefreshing = true)
            } else {
                _dashboardState.value = DashboardUiState(isLoading = true)
            }

            // Step 1: Get courses
            val coursesResult = repository.getUserCourses()
            if (coursesResult.isFailure) {
                val cachedCourses = dao.getAllCourses()
                _dashboardState.value = _dashboardState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isOffline = true,
                    errorMessage = coursesResult.exceptionOrNull()?.message
                )
                return@launch
            }
            val courses = coursesResult.getOrDefault(emptyList())
            val courseIds = courses.map { it -> it.id }

            // Cache courses
            dao.insertCourses(courses.map {
                com.vclass.app.data.local.CourseEntity(
                    id = it.id,
                    shortname = it.shortname,
                    fullname = it.fullname,
                    displayname = it.displayname,
                    progress = it.progress,
                    completed = it.completed,
                    startdate = it.startdate,
                    enddate = it.enddate,
                    lastaccess = it.lastaccess
                )
            })

            // Step 2: Get assignments for all courses
            val assignmentsResult = repository.getAssignments(courseIds)
            val allAssignments = assignmentsResult.getOrDefault(AssignmentsResponse())
                .courses?.flatMap { it.assignments ?: emptyList() } ?: emptyList()

            // Cache assignments
            dao.insertAssignments(allAssignments.map {
                com.vclass.app.data.local.AssignmentEntity(
                    id = it.id,
                    cmid = it.cmid,
                    courseId = it.course,
                    name = it.name,
                    duedate = it.duedate,
                    allowsubmissionsfromdate = it.allowsubmissionsfromdate,
                    grade = it.grade,
                    intro = it.intro,
                    submissionStatus = null
                )
            })

            // Step 3: Get calendar events
            val calendarResult = repository.getCalendarEvents()
            val events = calendarResult.getOrDefault(CalendarEventsResponse()).events ?: emptyList()

            // Step 4: Get submission status for each assignment
            val statuses = mutableMapOf<Int, SubmissionStatusResponse>()
            var unreadCount = 0
            for (assignment in allAssignments) {
                val statusResult = repository.getSubmissionStatus(assignment.id)
                if (statusResult.isSuccess) {
                    val status = statusResult.getOrDefault(SubmissionStatusResponse())
                    statuses[assignment.id] = status
                    // Count unsubmitted assignments
                    if (status.lastattempt?.submission?.status != "submitted") {
                        unreadCount++
                    }
                    dao.updateSubmissionStatus(
                        assignmentId = assignment.id,
                        status = status.lastattempt?.submission?.status ?: "none"
                    )
                }
            }

            // Step 5: Get announcements (news forums)
            val announcements = mutableListOf<ForumDiscussion>()
            try {
                val forumsResult = repository.getForums(courseIds)
                if (forumsResult.isSuccess) {
                    val newsForums = forumsResult.getOrDefault(emptyList())
                        .filter { it -> it.type == "news" }
                    for (forum in newsForums.take(3)) {
                        val discResult = repository.getForumDiscussions(forum.id)
                        if (discResult.isSuccess) {
                            val discs = discResult.getOrDefault(ForumDiscussionsResponse())
                                .discussions ?: emptyList()
                            announcements.addAll(discs.take(2))
                        }
                    }
                }
            } catch (_: Exception) { }

            _dashboardState.value = DashboardUiState(
                isLoading = false,
                isRefreshing = false,
                courses = courses,
                assignments = allAssignments,
                calendarEvents = events,
                submissionStatuses = statuses,
                announcements = announcements,
                isOffline = false,
                unreadCount = unreadCount
            )
        }
    }

    // ========== SEARCH ==========
    fun searchAssignments(query: String) {
        _dashboardState.value = _dashboardState.value.copy(searchQuery = query)
    }

    fun getFilteredAssignments(): List<Assignment> {
        val state = _dashboardState.value
        val query = state.searchQuery.lowercase()
        return if (query.isBlank()) {
            state.assignments
        } else {
            state.assignments.filter {
                it.name.lowercase().contains(query)
            }
        }
    }

    // ========== COURSE DETAIL ==========
    fun loadCourseDetail(course: Course, isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _courseDetailState.value = _courseDetailState.value.copy(isRefreshing = true)
            } else {
                _courseDetailState.value = CourseDetailUiState(isLoading = true, course = course)
            }

            val courseId = course.id

            val contentsResult = repository.getCourseContents(courseId)
            val sections = contentsResult.getOrDefault(emptyList())

            val assignmentsResult = repository.getAssignments(listOf(courseId))
            val assignments = assignmentsResult.getOrDefault(AssignmentsResponse())
                .courses?.flatMap { it.assignments ?: emptyList() } ?: emptyList()

            val forumsResult = repository.getForums(listOf(courseId))
            val forums = forumsResult.getOrDefault(emptyList())

            dao.insertForums(forums.map {
                com.vclass.app.data.local.ForumEntity(
                    id = it.id,
                    courseId = it.course,
                    type = it.type,
                    name = it.name,
                    intro = it.intro,
                    duedate = it.duedate,
                    numdiscussions = it.numdiscussions
                )
            })

            val statuses = mutableMapOf<Int, SubmissionStatusResponse>()
            for (assignment in assignments) {
                val statusResult = repository.getSubmissionStatus(assignment.id)
                if (statusResult.isSuccess) {
                    statuses[assignment.id] = statusResult.getOrDefault(SubmissionStatusResponse())
                }
            }

            _courseDetailState.value = CourseDetailUiState(
                isLoading = false,
                isRefreshing = false,
                course = course,
                sections = sections,
                assignments = assignments,
                forums = forums,
                submissionStatuses = statuses
            )
        }
    }

    // ========== ASSIGNMENT DETAIL ==========
    fun selectAssignment(assignment: Assignment) {
        _selectedAssignment.value = assignment
        _selectionCounter.value = _selectionCounter.value + 1
        // Always fetch fresh status from server when selecting an assignment
        fetchAndSyncSubmissionStatus(assignment.id)
        // Restart refresh job for the newly selected assignment
        restartSubmissionStatusRefresh()
    }

    fun clearSelectedAssignment() {
        _selectedAssignment.value = null
        _selectedSubmissionStatus.value = null
        // Stop refresh when no assignment is selected
        submissionRefreshJob?.cancel()
    }

    // ========== SUBMISSION STATUS REFRESH ==========
    private fun startSubmissionStatusRefresh() {
        // Cancel any existing job
        submissionRefreshJob?.cancel()
        
        // Start new job for periodic refresh (every 10 seconds for timely updates)
        submissionRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(10000) // 10 seconds
                val currentAssignment = _selectedAssignment.value
                if (currentAssignment != null) {
                    refreshSubmissionStatus(currentAssignment.id)
                }
            }
        }
    }

    private fun restartSubmissionStatusRefresh() {
        startSubmissionStatusRefresh()
    }

    private fun refreshSubmissionStatus(assignmentId: Int) {
        viewModelScope.launch {
            val result = repository.getSubmissionStatus(assignmentId)
            if (result.isSuccess) {
                val newStatus = result.getOrNull() ?: return@launch
                _selectedSubmissionStatus.value = newStatus

                // Sync to dashboard state
                val currentDashboard = _dashboardState.value
                val updatedDashboardStatuses = currentDashboard.submissionStatuses.toMutableMap()
                updatedDashboardStatuses[assignmentId] = newStatus
                // Recalculate unread count
                var unreadCount = 0
                for (a in currentDashboard.assignments) {
                    val s = updatedDashboardStatuses[a.id]
                    if (s?.lastattempt?.submission?.status != "submitted") {
                        unreadCount++
                    }
                }
                _dashboardState.value = currentDashboard.copy(
                    submissionStatuses = updatedDashboardStatuses,
                    unreadCount = unreadCount
                )

                // Sync to course detail state
                val currentCourseDetail = _courseDetailState.value
                if (currentCourseDetail.assignments.any { it.id == assignmentId }) {
                    val updatedCourseStatuses = currentCourseDetail.submissionStatuses.toMutableMap()
                    updatedCourseStatuses[assignmentId] = newStatus
                    _courseDetailState.value = currentCourseDetail.copy(
                        submissionStatuses = updatedCourseStatuses
                    )
                }
            }
        }
    }

    // Manual refresh function that can be called from UI
    fun refreshSelectedSubmissionStatus() {
        val assignmentId = _selectedAssignment.value?.id
        if (assignmentId != null) {
            refreshSubmissionStatus(assignmentId)
        }
    }

    /**
     * Fetch fresh submission status for a single assignment from server.
     * Used when opening the detail sheet to guarantee latest state.
     */
    fun fetchAndSyncSubmissionStatus(assignmentId: Int) {
        refreshSubmissionStatus(assignmentId)
    }

    // ========== REVERT / UNSUBMIT ==========
    fun revertSubmission(assignmentId: Int, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.revertSubmissionToDraft(assignmentId)
            if (result.isSuccess) {
                // Refresh status after revert
                fetchAndSyncSubmissionStatus(assignmentId)
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message)
            }
        }
    }
    fun loadForumDiscussions(forumId: Int) {
        viewModelScope.launch {
            _forumDetailState.value = ForumDetailUiState(isLoading = true)
            val result = repository.getForumDiscussions(forumId)
            result.onSuccess { response ->
                _forumDetailState.value = ForumDetailUiState(
                    isLoading = false,
                    discussions = response.discussions ?: emptyList()
                )
            }.onFailure { e ->
                _forumDetailState.value = ForumDetailUiState(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }

    // ========== GRADES ==========
    fun loadGrades(courses: List<Course>) {
        viewModelScope.launch {
            _gradesState.value = GradesUiState(isLoading = true)
            val grades = mutableListOf<GradeItem>()
            for (course in courses) {
                val result = repository.getGrades(course.id)
                result.onSuccess { response ->
                    response.tables?.forEach { table ->
                        table.tabledata?.forEach { row ->
                            val itemName = row.itemname?.content ?: ""
                            if (itemName.isNotBlank() && itemName != "Course total") {
                                grades.add(
                                    GradeItem(
                                        courseName = course.shortname,
                                        itemName = itemName,
                                        grade = row.grade?.content,
                                        weight = row.weight?.content,
                                        percentage = row.percentage?.content,
                                        feedback = row.feedback?.content
                                    )
                                )
                            }
                        }
                    }
                }
            }
            _gradesState.value = GradesUiState(isLoading = false, grades = grades)
        }
    }

    // ========== QUIZ ==========
    fun loadQuizDetail(quizId: Int) {
        viewModelScope.launch {
            _quizDetailState.value = QuizDetailUiState(isLoading = true)

            repository.getQuizAccessInfo(quizId).onSuccess {
                _quizDetailState.value = _quizDetailState.value.copy(accessInfo = it)
            }

            repository.getQuizUserAttempts(quizId).onSuccess { response ->
                _quizDetailState.value = QuizDetailUiState(
                    isLoading = false,
                    attempts = response.attempts ?: emptyList(),
                    accessInfo = _quizDetailState.value.accessInfo
                )
            }.onFailure { e ->
                _quizDetailState.value = QuizDetailUiState(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }

    // ========== PROFILE ==========
    fun loadProfile() {
        viewModelScope.launch {
            _profileState.value = ProfileUiState(isLoading = true)
            val result = repository.getSiteInfo()
            result.onSuccess { info ->
                _profileState.value = ProfileUiState(
                    isLoading = false,
                    siteInfo = info
                )
            }.onFailure { e ->
                _profileState.value = ProfileUiState(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }
}
