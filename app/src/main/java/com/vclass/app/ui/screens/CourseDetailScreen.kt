package com.vclass.app.ui.screens

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vclass.app.data.model.*
import com.vclass.app.ui.components.*
import com.vclass.app.ui.theme.*
import com.vclass.app.viewmodel.VClassViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    viewModel: VClassViewModel,
    onBack: () -> Unit,
    onForumClick: (Forum) -> Unit,
    onQuizClick: (CourseModule) -> Unit
) {
    val state by viewModel.courseDetailState.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            state.course?.shortname ?: "Course Detail",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = CardWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GunadarmaBlue,
                    titleContentColor = CardWhite
                ),
                actions = {
                    IconButton(onClick = {
                        state.course?.let { viewModel.loadCourseDetail(it, isRefresh = true) }
                    }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = CardWhite
                        )
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingScreen()
            state.errorMessage != null -> ErrorScreen(
                message = state.errorMessage!!,
                onRetry = { state.course?.let { viewModel.loadCourseDetail(it) } }
            )
            else -> {
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { state.course?.let { viewModel.loadCourseDetail(it, isRefresh = true) } },
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
                        if (state.sections.isEmpty()) {
                            item {
                                EmptyState("Belum ada materi di course ini")
                            }
                        }

                        state.sections.forEach { section ->
                            val modules = section.modules.orEmpty()
                            if (modules.isNotEmpty()) {
                                item(key = "section-${section.id}") {
                                    CourseSectionHeader(section = section)
                                }

                                items(
                                    items = modules,
                                    key = { module -> "module-${module.id}" }
                                ) { module ->
                                    val assignment = state.assignments.firstOrNull {
                                        it.cmid == module.id || it.id == module.instance
                                    }
                                    val forum = state.forums.firstOrNull {
                                        it.id == module.instance
                                    }

                                    CourseModuleCard(
                                        module = module.copy(courseid = state.course?.id),
                                        assignment = assignment,
                                        forum = forum,
                                        submissionStatus = assignment?.let { state.submissionStatuses[it.id] },
                                        onAssignmentClick = {
                                            assignment?.let { viewModel.selectAssignment(it) }
                                        },
                                        onForumClick = {
                                            forum?.let { onForumClick(it) }
                                        },
                                        onQuizClick = {
                                            onQuizClick(module)
                                        }
                                    )
                                }
                            }
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

@Composable
fun CourseSectionHeader(section: CourseSection) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
    ) {
        Text(
            text = section.name.ifBlank { "General" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (!section.summary.isNullOrBlank()) {
            Text(
                text = android.text.Html.fromHtml(section.summary, android.text.Html.FROM_HTML_MODE_COMPACT).toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun CourseModuleCard(
    module: CourseModule,
    assignment: Assignment?,
    forum: Forum?,
    submissionStatus: SubmissionStatusResponse?,
    onAssignmentClick: () -> Unit,
    onForumClick: () -> Unit,
    onQuizClick: () -> Unit
) {
    when (module.modname) {
        "assign" -> {
            if (assignment != null) {
                AssignmentCard(
                    assignment = assignment,
                    submissionStatus = submissionStatus,
                    onClick = onAssignmentClick
                )
            } else {
                GenericModuleCard(
                    module = module,
                    icon = Icons.Default.Assignment,
                    tint = WarningOrange,
                    subtitle = "Tugas",
                    onClick = {}
                )
            }
        }
        "forum" -> {
            if (forum != null) {
                ForumCard(forum = forum, onClick = onForumClick)
            } else {
                GenericModuleCard(
                    module = module,
                    icon = Icons.Default.Forum,
                    tint = GunadarmaLightBlue,
                    subtitle = "Forum",
                    onClick = {}
                )
            }
        }
        "quiz" -> QuizCard(quiz = module, onClick = onQuizClick)
        "resource", "url", "page", "folder", "book", "label" -> ResourceCard(resource = module)
        else -> GenericModuleCard(
            module = module,
            icon = Icons.Default.Extension,
            tint = MaterialTheme.colorScheme.primary,
            subtitle = module.modname.replaceFirstChar { it.uppercase() },
            onClick = {}
        )
    }
}

@Composable
fun GenericModuleCard(
    module: CourseModule,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = module.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ResourceCard(resource: CourseModule) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.InsertDriveFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = resource.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (!resource.contents.isNullOrEmpty()) {
                    val file = resource.contents.first()
                    Text(
                        text = "${file.filename} (${formatFileSize(file.filesize ?: 0)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.Default.Download,
                contentDescription = "Download",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(20.dp)
                    .clickable {
                        if (!resource.contents.isNullOrEmpty()) {
                            val file = resource.contents.first()
                            val fileUrl = file.fileurl
                            if (!fileUrl.isNullOrBlank()) {
                                downloadFile(context, fileUrl, file.filename ?: "file", resource.name)
                            }
                        }
                    }
            )
        }
    }
}

@Composable
fun QuizCard(quiz: CourseModule, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Quiz,
                contentDescription = null,
                tint = WarningOrange,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quiz.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Quiz",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

fun downloadFile(context: Context, fileUrl: String, fileName: String, resourceName: String) {
    try {
        val downloadUrl = if (fileUrl.contains("?")) {
            "$fileUrl&token=YOUR_MOODLE_TOKEN"
        } else {
            "$fileUrl?token=YOUR_MOODLE_TOKEN"
        }

        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle(resourceName)
            .setDescription("Downloading $fileName")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)

        Toast.makeText(context, "Downloading $fileName...", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }
}
