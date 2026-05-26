package com.vclass.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vclass.app.data.model.*
import com.vclass.app.ui.components.*
import com.vclass.app.ui.theme.*
import com.vclass.app.viewmodel.VClassViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: VClassViewModel,
    onCourseClick: (Course) -> Unit,
    onGradesClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val state by viewModel.dashboardState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullToRefreshState()

    // Filter state
    var selectedFilter by remember { mutableStateOf(FilterType.ALL) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchVisible) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                viewModel.searchAssignments(it)
                            },
                            placeholder = { Text("Cari tugas...", color = CardWhite.copy(alpha = 0.7f)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = CardWhite,
                                unfocusedTextColor = CardWhite,
                                focusedBorderColor = CardWhite,
                                unfocusedBorderColor = CardWhite.copy(alpha = 0.5f),
                                cursorColor = CardWhite
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Column {
                            Text(
                                "V-Class Gunadarma",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Mahasiswa",
                                style = MaterialTheme.typography.bodySmall,
                                color = GunadarmaLightBlue
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GunadarmaBlue,
                    titleContentColor = CardWhite
                ),
                actions = {
                    if (isSearchVisible) {
                        IconButton(onClick = {
                            isSearchVisible = false
                            searchQuery = ""
                            viewModel.searchAssignments("")
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Search", tint = CardWhite)
                        }
                    } else {
                        // Notification bell with badge
                        BadgedBox(
                            badge = {
                                if (state.unreadCount > 0) {
                                    Badge(
                                        containerColor = ErrorRed,
                                        contentColor = CardWhite
                                    ) {
                                        Text("${state.unreadCount}")
                                    }
                                }
                            }
                        ) {
                            IconButton(onClick = {
                                selectedFilter = FilterType.UNSUBMITTED
                            }) {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = CardWhite)
                            }
                        }
                        IconButton(onClick = onCalendarClick) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar", tint = CardWhite)
                        }
                        IconButton(onClick = { isSearchVisible = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = CardWhite)
                        }
                        IconButton(onClick = onGradesClick) {
                            Icon(Icons.Default.Grade, contentDescription = "Grades", tint = CardWhite)
                        }
                        // Profile menu
                        var showMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = CardWhite)
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Profil Saya") },
                                    onClick = {
                                        showMenu = false
                                        onProfileClick()
                                    },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Pengaturan") },
                                    onClick = {
                                        showMenu = false
                                        onSettingsClick()
                                    },
                                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Refresh") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.loadDashboard(isRefresh = true)
                                    },
                                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingScreen()
            state.errorMessage != null && state.courses.isEmpty() -> ErrorScreen(
                message = state.errorMessage!!,
                onRetry = { viewModel.loadDashboard() }
            )
            else -> {
                val now = System.currentTimeMillis() / 1000
                val oneHour = 3600L
                val oneDay = 86400L

                // Filter assignments by search
                val filteredAssignments = if (searchQuery.isNotBlank()) {
                    state.assignments.filter {
                        it.name.lowercase().contains(searchQuery.lowercase())
                    }
                } else {
                    state.assignments
                }

                // ===== DEADLINE MENDEKAT (<= 1 JAM) =====
                val urgentDeadlines = filteredAssignments
                    .filter { assignment ->
                        val duedate = assignment.duedate
                        duedate != null && duedate > 0 &&
                        duedate > now &&
                        (duedate - now) <= oneHour &&
                        state.submissionStatuses[assignment.id]?.lastattempt?.submission?.status != "submitted"
                    }
                    .sortedBy { it.duedate }

                // ===== DEADLINE MENDEKAT (<= 1 HARI) =====
                val nearDeadlines = filteredAssignments
                    .filter { assignment ->
                        val duedate = assignment.duedate
                        duedate != null && duedate > 0 &&
                        duedate > now &&
                        (duedate - now) > oneHour &&
                        (duedate - now) <= oneDay &&
                        state.submissionStatuses[assignment.id]?.lastattempt?.submission?.status != "submitted"
                    }
                    .sortedBy { it.duedate }

                // ===== BELUM SUBMIT =====
                val unsubmitted = filteredAssignments
                    .filter { assignment ->
                        val status = state.submissionStatuses[assignment.id]?.lastattempt?.submission?.status
                        status != "submitted" && status != "draft"
                    }
                    .sortedBy { it.duedate }

                // ===== SEMUA UPCOMING =====
                val allUpcoming = filteredAssignments
                    .filter { it.duedate != null && it.duedate > 0 }
                    .sortedBy { it.duedate }

                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { viewModel.loadDashboard(isRefresh = true) },
                    state = pullRefreshState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Offline indicator
                        if (state.isOffline) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = WarningOrange.copy(alpha = 0.1f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.CloudOff,
                                            contentDescription = null,
                                            tint = WarningOrange,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Mode offline - data mungkin tidak terbaru",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = WarningOrange
                                        )
                                    }
                                }
                            }
                        }

                        // ===== URGENT: DEADLINE <= 1 JAM =====
                        if (urgentDeadlines.isNotEmpty()) {
                            item {
                                UrgentDeadlineBanner(urgentDeadlines.size)
                            }
                            items(urgentDeadlines) { assignment ->
                                val course = state.courses.find { it.id == assignment.course }
                                UrgentAssignmentCard(
                                    assignment = assignment,
                                    courseName = course?.shortname ?: "",
                                    submissionStatus = state.submissionStatuses[assignment.id],
                                    onClick = { viewModel.selectAssignment(assignment) }
                                )
                            }
                        }

                        // ===== NEAR: DEADLINE <= 1 HARI =====
                        if (nearDeadlines.isNotEmpty()) {
                            item {
                                NearDeadlineBanner(nearDeadlines.size)
                            }
                            items(nearDeadlines) { assignment ->
                                val course = state.courses.find { it.id == assignment.course }
                                AssignmentCard(
                                    assignment = assignment,
                                    courseName = course?.shortname ?: "",
                                    submissionStatus = state.submissionStatuses[assignment.id],
                                    onClick = { viewModel.selectAssignment(assignment) },
                                    isUrgent = true
                                )
                            }
                        }

                        // ===== FILTER CHIPS =====
                        item {
                            FilterChips(
                                selectedFilter = selectedFilter,
                                onFilterSelected = { selectedFilter = it },
                                unsubmittedCount = unsubmitted.size,
                                upcomingCount = allUpcoming.size
                            )
                        }

                        // ===== FILTERED ASSIGNMENTS =====
                        val displayedAssignments = when (selectedFilter) {
                            FilterType.UNSUBMITTED -> unsubmitted
                            FilterType.ALL -> allUpcoming
                        }

                        if (displayedAssignments.isNotEmpty()) {
                            items(displayedAssignments) { assignment ->
                                val course = state.courses.find { it.id == assignment.course }
                                AssignmentCard(
                                    assignment = assignment,
                                    courseName = course?.shortname ?: "",
                                    submissionStatus = state.submissionStatuses[assignment.id],
                                    onClick = { viewModel.selectAssignment(assignment) }
                                )
                            }
                        } else {
                            item {
                                EmptyState(
                                    when (selectedFilter) {
                                        FilterType.UNSUBMITTED -> "Semua tugas udah di-submit! 🎉"
                                        FilterType.ALL -> "Tidak ada deadline mendatang"
                                    }
                                )
                            }
                        }

                        // ===== CALENDAR EVENTS =====
                        val upcomingEvents = state.calendarEvents
                            .sortedBy { it.timestart }
                            .take(5)

                        if (upcomingEvents.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Calendar Events",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            items(upcomingEvents) { event ->
                                EventCard(event = event)
                            }
                        }

                        // ===== ANNOUNCEMENTS =====
                        if (state.announcements.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Pengumuman",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            items(state.announcements.take(5)) { announcement ->
                                AnnouncementCard(announcement = announcement)
                            }
                        }

                        // ===== MY COURSES =====
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Matkul Ku",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        items(state.courses) { course ->
                            CourseCard(
                                course = course,
                                onClick = { onCourseClick(course) }
                            )
                        }

                        // Bottom spacer
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// ========== FILTER TYPE ==========
enum class FilterType(val label: String) {
    ALL("Semua"),
    UNSUBMITTED("Belum Submit")
}

// ========== FILTER CHIPS ==========
@Composable
fun FilterChips(
    selectedFilter: FilterType,
    onFilterSelected: (FilterType) -> Unit,
    unsubmittedCount: Int,
    upcomingCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedFilter == FilterType.ALL,
            onClick = { onFilterSelected(FilterType.ALL) },
            label = { Text("Semua ($upcomingCount)") },
            leadingIcon = if (selectedFilter == FilterType.ALL) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else null
        )
        FilterChip(
            selected = selectedFilter == FilterType.UNSUBMITTED,
            onClick = { onFilterSelected(FilterType.UNSUBMITTED) },
            label = { Text("Belum Submit ($unsubmittedCount)") },
            leadingIcon = if (selectedFilter == FilterType.UNSUBMITTED) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else null
        )
    }
}

// ========== URGENT DEADLINE BANNER ==========
@Composable
fun UrgentDeadlineBanner(count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ErrorRed.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = ErrorRed,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    "Deadline Mendekat!",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ErrorRed
                )
                Text(
                    "$count tugas deadline-nya kurang dari 1 jam!",
                    style = MaterialTheme.typography.bodySmall,
                    color = ErrorRed.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// ========== NEAR DEADLINE BANNER ==========
@Composable
fun NearDeadlineBanner(count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = WarningOrange.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Schedule,
                contentDescription = null,
                tint = WarningOrange,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    "Deadline Besok",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = WarningOrange
                )
                Text(
                    "$count tugas deadline-nya kurang dari 24 jam",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarningOrange.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// ========== URGENT ASSIGNMENT CARD ==========
@Composable
fun UrgentAssignmentCard(
    assignment: Assignment,
    courseName: String = "",
    submissionStatus: SubmissionStatusResponse? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis() / 1000
    val remaining = (assignment.duedate ?: 0) - now
    val remainingText = when {
        remaining < 60 -> "$remaining detik lagi!"
        remaining < 3600 -> "${remaining / 60} menit lagi!"
        else -> "${remaining / 3600} jam lagi"
    }

    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ErrorRed.copy(alpha = 0.05f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = assignment.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ErrorRed,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(ErrorRed, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "URGENT",
                        style = MaterialTheme.typography.labelSmall,
                        color = CardWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (courseName.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = courseName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = ErrorRed
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = remainingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = ErrorRed,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


// ========== ANNOUNCEMENT CARD ==========
@Composable
fun AnnouncementCard(
    announcement: ForumDiscussion,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = WarningOrange.copy(alpha = 0.08f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(WarningOrange.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Campaign,
                    contentDescription = null,
                    tint = WarningOrange,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = announcement.subject ?: announcement.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!announcement.message.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = android.text.Html.fromHtml(
                            announcement.message,
                            android.text.Html.FROM_HTML_MODE_COMPACT
                        ).toString().take(100),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "oleh User #${announcement.userid ?: "?"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
