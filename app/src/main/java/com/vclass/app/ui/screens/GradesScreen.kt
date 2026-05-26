package com.vclass.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vclass.app.data.model.*
import com.vclass.app.ui.components.*
import com.vclass.app.ui.theme.*
import com.vclass.app.viewmodel.VClassViewModel

data class GradeItem(
    val courseName: String,
    val itemName: String,
    val grade: String?,
    val weight: String?,
    val percentage: String?,
    val feedback: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradesScreen(
    viewModel: VClassViewModel,
    courses: List<Course>,
    onBack: () -> Unit
) {
    var allGrades by remember { mutableStateOf<List<GradeItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedCourseFilter by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        val grades = mutableListOf<GradeItem>()
        for (course in courses) {
            val result = viewModel.repository.getGrades(course.id)
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
        allGrades = grades
        isLoading = false
    }

    val filteredGrades = if (selectedCourseFilter != null) {
        allGrades.filter { it.courseName == courses.find { c -> c.id == selectedCourseFilter }?.shortname }
    } else {
        allGrades
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Nilai / Grades", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GunadarmaBlue,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        when {
            isLoading -> LoadingScreen()
            errorMessage != null -> ErrorScreen(message = errorMessage!!, onRetry = {
                isLoading = true
                errorMessage = null
            })
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Course filter chips
                    item {
                        Text(
                            "Filter Matkul:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedCourseFilter == null,
                                onClick = { selectedCourseFilter = null },
                                label = { Text("Semua") }
                            )
                            courses.forEach { course ->
                                FilterChip(
                                    selected = selectedCourseFilter == course.id,
                                    onClick = { selectedCourseFilter = course.id },
                                    label = { Text(course.shortname, maxLines = 1) }
                                )
                            }
                        }
                    }

                    if (filteredGrades.isEmpty()) {
                        item {
                            EmptyState("Belum ada nilai")
                        }
                    } else {
                        items(filteredGrades) { grade ->
                            GradeCard(
                                itemName = grade.itemName,
                                grade = grade.grade,
                                weight = grade.weight,
                                percentage = grade.percentage,
                                feedback = grade.feedback
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
