package com.vclass.app.ui.screens

import android.text.Html
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vclass.app.data.model.*
import com.vclass.app.data.remote.RemoteConfigManager
import com.vclass.app.ui.components.*
import com.vclass.app.ui.theme.*
import com.vclass.app.viewmodel.VClassViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizDetailScreen(
    viewModel: VClassViewModel,
    quiz: CourseModule,
    onBack: () -> Unit
) {
    var attempts by remember { mutableStateOf<List<QuizAttemptInfo>>(emptyList()) }
    var accessInfo by remember { mutableStateOf<QuizAccessResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isStarting by remember { mutableStateOf(false) }
    var selectedAttempt by remember { mutableStateOf<QuizAttemptInfo?>(null) }
    var finalGrade by remember { mutableStateOf<QuizFinalGrade?>(null) }
    var isGradeLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Extract quiz ID from module
    val quizId = quiz.instance ?: quiz.id

    LaunchedEffect(quizId) {
        isLoading = true
        // Get access info
        viewModel.repository.getQuizAccessInfo(quizId).onSuccess {
            accessInfo = it
        }
        // Get user attempts
        viewModel.repository.getQuizUserAttempts(quizId).onSuccess { response ->
            attempts = response.attempts ?: emptyList()
            isLoading = false
        }.onFailure { e ->
            errorMessage = e.message
            isLoading = false
        }
        isGradeLoading = true
        val courseIds = buildList {
            quiz.courseid?.takeIf { it > 0 }?.let { add(it) }
            viewModel.dashboardState.value.courses.forEach { course ->
                if (!contains(course.id)) add(course.id)
            }
        }
        courseIds.forEach { courseId ->
            if (finalGrade == null) {
                viewModel.repository.getGrades(courseId).onSuccess { response ->
                    finalGrade = findQuizFinalGrade(response, quiz.name)
                }
            }
        }
        if (finalGrade == null) {
            viewModel.repository.getAllGrades().onSuccess { response ->
                    finalGrade = findQuizFinalGrade(response, quiz.name)
            }
        }
        isGradeLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(quiz.name, fontWeight = FontWeight.Bold, maxLines = 1)
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
                if (selectedAttempt != null) {
                    QuizReviewScreen(
                        viewModel = viewModel,
                        attempt = selectedAttempt!!,
                        quizName = quiz.name,
                        finalGrade = finalGrade,
                        onBack = { selectedAttempt = null },
                        modifier = Modifier.padding(padding)
                    )
                } else {
                    val preventAccessReasons = accessInfo?.preventaccessreasons.orEmpty()
                    val canStartQuiz = accessInfo?.canattempt == true && preventAccessReasons.isEmpty()
                    val highestGrade = finalGrade

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Quiz info
                        item {
                            QuizInfoCard(
                                quiz = quiz,
                                accessInfo = accessInfo
                            )
                        }

                        if (attempts.isNotEmpty()) {
                            item {
                                Text(
                                    "Summary of your previous attempts",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            items(attempts) { attempt ->
                                AttemptSummaryCard(
                                    attempt = attempt,
                                    finalGrade = finalGrade,
                                    isGradeLoading = isGradeLoading,
                                    onReviewClick = { selectedAttempt = attempt }
                                )
                            }
                            highestGrade?.let { grade ->
                                item {
                                    Text(
                                        text = "Highest grade: ${grade.displayText}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        } else {
                            item {
                                EmptyState("Belum ada attempt")
                            }
                        }

                        if (canStartQuiz) {
                            item {
                                QuizAttemptButton(
                                    isReattempt = attempts.isNotEmpty(),
                                    isStarting = isStarting,
                                    onClick = {
                                        isStarting = true
                                        coroutineScope.launch {
                                            viewModel.repository.startQuizAttempt(quizId).onSuccess { response ->
                                                response.attempt?.let { attempt ->
                                                    selectedAttempt = attempt
                                                }
                                            }.onFailure { e ->
                                                errorMessage = e.message
                                            }
                                            isStarting = false
                                        }
                                    }
                                )
                            }
                        } else if (preventAccessReasons.isNotEmpty()) {
                            item {
                                QuizBlockedCard(
                                    preventAccessReasons = preventAccessReasons,
                                    accessRules = accessInfo?.accessrules.orEmpty()
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
}

@Composable
fun QuizInfoCard(
    quiz: CourseModule,
    accessInfo: QuizAccessResponse?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = GunadarmaBlue.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = quiz.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )
            if (!quiz.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = Html.fromHtml(quiz.description, Html.FROM_HTML_MODE_COMPACT).toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            accessInfo?.accessrules.orEmpty().forEach { rule ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = rule,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun AttemptSummaryCard(
    attempt: QuizAttemptInfo,
    finalGrade: QuizFinalGrade?,
    isGradeLoading: Boolean,
    onReviewClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Attempt ${attempt.attempt ?: "-"}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when (attempt.state) {
                            "finished" -> "Finished"
                            "inprogress" -> "In progress"
                            else -> attempt.state ?: "Unknown"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (attempt.timefinish != null && attempt.timefinish > 0) {
                        Text(
                            text = "Submitted ${attempt.timefinish.toFormattedDate()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = when {
                            finalGrade != null -> finalGrade.displayText
                            isGradeLoading -> "Mengambil nilai..."
                            else -> "Nilai akhir belum tersedia"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onReviewClick) {
                        Text("Review")
                    }
                }
            }
        }
    }
}

@Composable
fun QuizAttemptButton(
    isReattempt: Boolean,
    isStarting: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = !isStarting,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = GunadarmaBlue)
    ) {
        if (isStarting) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Memulai...")
        } else {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isReattempt) "Re-attempt quiz" else "Attempt quiz")
        }
    }
}

@Composable
fun QuizBlockedCard(preventAccessReasons: List<String>, accessRules: List<String>) {
    val combinedStatus = (preventAccessReasons + accessRules).joinToString(" ").lowercase()
    val quizRules = RemoteConfigManager.current.quiz
    val hasNoQuestions = quizRules.noQuestionsTexts.any { combinedStatus.contains(it.lowercase()) }
    val isClosed = quizRules.closedTexts.any { combinedStatus.contains(it.lowercase()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                when {
                    hasNoQuestions -> "Soal quiz belum tersedia"
                    isClosed -> "Quiz sudah ditutup"
                    else -> "Quiz belum bisa dikerjakan"
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ErrorRed
            )
            Spacer(modifier = Modifier.height(6.dp))
            preventAccessReasons.forEach { reason ->
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AttemptCard(
    attempt: QuizAttemptInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                when (attempt.state) {
                    "finished" -> Icons.Default.CheckCircle
                    "inprogress" -> Icons.Default.AccessTime
                    else -> Icons.Default.Quiz
                },
                contentDescription = null,
                tint = when (attempt.state) {
                    "finished" -> SuccessGreen
                    "inprogress" -> WarningOrange
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Attempt #${attempt.attempt}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = when (attempt.state) {
                        "finished" -> "Selesai"
                        "inprogress" -> "Sedang berjalan"
                        else -> attempt.state ?: "Unknown"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (attempt.timefinish != null && attempt.timefinish > 0) {
                    Text(
                        text = "Selesai: ${attempt.timefinish.toFormattedDate()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (attempt.state == "finished") {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = attempt.grade?.let { "%.1f".format(it) } ?: "-",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen
                    )
                    Text(
                        text = "Nilai",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun QuizReviewScreen(
    viewModel: VClassViewModel,
    attempt: QuizAttemptInfo,
    quizName: String,
    finalGrade: QuizFinalGrade?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var review by remember { mutableStateOf<QuizAttemptReviewResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(attempt.id) {
        isLoading = true
        viewModel.repository.getQuizAttemptReview(attempt.id).onSuccess {
            review = it
            isLoading = false
        }.onFailure { e ->
            errorMessage = e.message
            isLoading = false
        }
    }

    val summaryRows = listOfNotNull(
        attempt.timestart?.takeIf { it > 0 }?.let { "Started on" to it.toFormattedDate() },
        "State" to when (attempt.state) {
            "finished" -> "Finished"
            "inprogress" -> "In progress"
            else -> attempt.state.orEmpty().ifBlank { "Unknown" }
        },
        attempt.timefinish?.takeIf { it > 0 }?.let { "Completed on" to it.toFormattedDate() },
        "Grade" to (finalGrade?.displayReviewText
            ?: "Nilai akhir belum tersedia dari API")
    )

    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = GunadarmaBlue.copy(alpha = 0.08f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text(
                        text = quizName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Attempt #${attempt.attempt} - Nilai: ${
                            finalGrade?.grade ?: attempt.grade?.let { "%.1f".format(it) } ?: "-"
                        }",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (summaryRows.isNotEmpty()) {
            AttemptReviewSummary(
                rows = summaryRows,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        when {
            isLoading -> LoadingScreen()
            errorMessage != null -> ErrorScreen(message = errorMessage!!, onRetry = {
                isLoading = true
                errorMessage = null
            })
            review != null -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    review?.questions
                        ?.sortedWith(
                            compareBy<QuizQuestion> { it.slot ?: Int.MAX_VALUE }
                                .thenBy { extractReviewQuestionNumber(it.html.orEmpty()) ?: Int.MAX_VALUE }
                        )
                        ?.forEachIndexed { index, question ->
                        item {
                            QuestionReviewCard(question = question, displayNumber = index + 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuestionReviewCard(question: QuizQuestion, displayNumber: Int) {
    val cleanText = cleanQuizReviewHtml(question.html.orEmpty())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Question $displayNumber",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = question.status ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        question.status?.contains("Correct", ignoreCase = true) == true -> SuccessGreen
                        question.status?.contains("Wrong", ignoreCase = true) == true -> ErrorRed
                        else -> WarningOrange
                    }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = cleanText,
                style = MaterialTheme.typography.bodyMedium
            )
            if (!question.mark.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Mark: ${question.mark} / ${question.maxmark ?: "-"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AttemptReviewSummary(
    rows: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            rows.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(110.dp)
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

fun cleanQuizReviewHtml(html: String): String {
    if (html.isBlank()) return ""

    val withoutNoise = html
        .replace(Regex("(?is)<script\\b.*?</script>"), "")
        .replace(Regex("(?is)<style\\b.*?</style>"), "")
        .replace(Regex("(?is)<input\\b[^>]*>"), "")
        .replace(Regex("(?is)<label[^>]*class=\"[^\"]*questionflag[^\"]*\".*?</label>"), "")
        .replace(Regex("(?is)<div[^>]*class=\"[^\"]*questionflag[^\"]*\".*?</div>"), "")
        .replace(Regex("(?is)<div[^>]*class=[\"'][^\"']*info[^\"']*[\"'][^>]*>.*?</div>"), "")
        .replace(Regex("(?is)<h4[^>]*class=\"[^\"]*accesshide[^\"]*\".*?</h4>"), "")

    return Html.fromHtml(withoutNoise, Html.FROM_HTML_MODE_COMPACT)
        .toString()
        .replace("\uFFFC", "")
        .replace(Regex("(?m)^\\s*(Question\\s+\\d+|Correct|Incorrect|Partially correct|Complete|Not complete)\\s*$"), "")
        .replace(Regex("(?m)^\\s*Mark\\s+[0-9.,]+\\s+out\\s+of\\s+[0-9.,]+\\s*$"), "")
        .replace(Regex("(?m)^\\s*(Question text|Feedback|Comments|Comment:|Flag question)\\s*$"), "")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
}

fun extractReviewQuestionNumber(html: String): Int? {
    return Regex("(?is)<span[^>]*class=[\"'][^\"']*qno[^\"']*[\"'][^>]*>\\s*(\\d+)\\s*</span>")
        .find(html)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
}

data class QuizFinalGrade(
    val grade: String,
    val maxGrade: String?
) {
    val displayText: String
        get() = if (!maxGrade.isNullOrBlank()) "$grade / $maxGrade" else grade

    val displayReviewText: String
        get() = if (!maxGrade.isNullOrBlank()) "$grade out of $maxGrade" else grade
}

fun findQuizFinalGrade(response: GradesTableResponse, quizName: String): QuizFinalGrade? {
    val normalizedQuizName = normalizeGradeText(quizName)

    response.tables.orEmpty().forEach { table ->
        table.tabledata.orEmpty().forEach { row ->
            val itemName = normalizeGradeText(
                Html.fromHtml(row.itemname?.content.orEmpty(), Html.FROM_HTML_MODE_COMPACT).toString()
            )

            if (itemName.contains(normalizedQuizName) || normalizedQuizName.contains(itemName)) {
                val grade = Html.fromHtml(row.grade?.content.orEmpty(), Html.FROM_HTML_MODE_COMPACT)
                    .toString()
                    .trim()
                    .takeIf { it.isNotBlank() && it != "-" }

                val maxGrade = extractMaxGradeFromRemoteConfig(row.range?.content.orEmpty())

                if (grade != null) return QuizFinalGrade(grade = grade, maxGrade = maxGrade)

                val percentageGrade = Html.fromHtml(row.percentage?.content.orEmpty(), Html.FROM_HTML_MODE_COMPACT)
                    .toString()
                    .replace("%", "")
                    .trim()
                    .takeIf { it.isNotBlank() && it != "-" }

                if (percentageGrade != null) {
                    return QuizFinalGrade(grade = percentageGrade, maxGrade = "100.00")
                }
            }
        }
    }

    return null
}

private fun extractMaxGradeFromRemoteConfig(rangeContent: String): String? {
    val rangeText = Html.fromHtml(rangeContent, Html.FROM_HTML_MODE_COMPACT)
        .toString()
        .trim()

    if (rangeText.isBlank() || rangeText == "-") return null

    RemoteConfigManager.current.quiz.rangeMaxPatterns.forEach { pattern ->
        Regex(pattern, RegexOption.IGNORE_CASE)
            .find(rangeText)
            ?.groupValues
            ?.getOrNull(1)
            ?.replace(",", ".")
            ?.let { return it }
    }

    return null
}

private fun extractMaxGrade(rangeContent: String): String? {
    val rangeText = Html.fromHtml(rangeContent, Html.FROM_HTML_MODE_COMPACT)
        .toString()
        .trim()

    if (rangeText.isBlank() || rangeText == "-") return null

    return Regex("""(?:-|–|to)\s*([0-9]+(?:[.,][0-9]+)?)\s*$""", RegexOption.IGNORE_CASE)
        .find(rangeText)
        ?.groupValues
        ?.getOrNull(1)
        ?.replace(",", ".")
}

private fun normalizeGradeText(value: String): String {
    return value
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
}
