package com.vclass.app

import android.os.Bundle
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vclass.app.data.local.SavedLogin
import com.vclass.app.data.local.SettingsRepository
import com.vclass.app.data.model.*
import com.vclass.app.data.remote.RemoteConfigManager
import com.vclass.app.data.repository.MoodleRepository
import com.vclass.app.data.worker.DeadlineCheckWorker
import com.vclass.app.ui.components.ErrorScreen
import com.vclass.app.ui.screens.*
import com.vclass.app.ui.theme.VClassTheme
import com.vclass.app.viewmodel.VClassViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission for Android 13+
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }
            .launch(android.Manifest.permission.POST_NOTIFICATIONS)

        // Schedule background deadline check
        DeadlineCheckWorker.schedule(this, intervalMinutes = 15)

        setContent {
            val settingsRepo = remember { SettingsRepository(this@MainActivity) }
            val scope = rememberCoroutineScope()
            val savedLogin by settingsRepo.savedLogin.collectAsState(initial = null)
            val isDarkMode by settingsRepo.darkMode.collectAsState(initial = false)
            var isLoggedIn by remember { mutableStateOf(false) }
            var isLoggingIn by remember { mutableStateOf(false) }
            var loginError by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(savedLogin?.token) {
                val token = savedLogin?.token.orEmpty()
                if (token.isNotBlank()) {
                    MoodleRepository.sessionToken = token
                    isLoggedIn = true
                }
            }

            VClassTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        savedLogin == null -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        !isLoggedIn -> {
                            val login = savedLogin ?: SavedLogin()
                            LoginScreen(
                                initialUsername = login.username,
                                initialPassword = login.password,
                                initialRememberLogin = login.rememberLogin,
                                isLoading = isLoggingIn,
                                errorMessage = loginError,
                                onLogin = { username, password, rememberLogin ->
                                    scope.launch {
                                        isLoggingIn = true
                                        loginError = null
                                        val result = MoodleRepository().login(username, password)
                                        result.onSuccess { token ->
                                            settingsRepo.saveLogin(username, password, token, rememberLogin)
                                            isLoggedIn = true
                                        }.onFailure { error ->
                                            loginError = error.message ?: "Login gagal"
                                        }
                                        isLoggingIn = false
                                    }
                                }
                            )
                        }
                        else -> {
                            key(savedLogin?.token) {
                                AuthenticatedVClassApp(
                                    settingsRepo = settingsRepo,
                                    onLogoutToLogin = {
                                        scope.launch {
                                            settingsRepo.clearLogin(clearAccounts = false)
                                            MoodleRepository.sessionToken = ""
                                            isLoggedIn = false
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedVClassApp(
    settingsRepo: SettingsRepository,
    onLogoutToLogin: () -> Unit
) {
    val viewModel: VClassViewModel = viewModel()
    val isDarkMode by settingsRepo.darkMode.collectAsState(initial = false)

    LaunchedEffect(isDarkMode) {
        viewModel.setDarkMode(isDarkMode)
    }

    LaunchedEffect(Unit) {
        RemoteConfigManager.refresh()
    }

    VClassApp(viewModel, settingsRepo, onLogoutToLogin)
}

@Composable
fun VClassApp(
    viewModel: VClassViewModel,
    settingsRepo: SettingsRepository,
    onLogoutToLogin: () -> Unit
) {
    val navController = rememberNavController()
    val dashboardState by viewModel.dashboardState.collectAsState()
    val profileState by viewModel.profileState.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val notificationsEnabled by settingsRepo.notificationsEnabled.collectAsState(initial = true)
    val refreshInterval by settingsRepo.refreshInterval.collectAsState(initial = 15)
    val savedLogin by settingsRepo.savedLogin.collectAsState(initial = SavedLogin())
    val savedAccounts by settingsRepo.savedAccounts.collectAsState(initial = emptyList())

    var showOnboarding by remember { mutableStateOf(false) }
    var selectedForum by remember { mutableStateOf<Forum?>(null) }

    NavHost(
        navController = navController,
        startDestination = "dashboard"
    ) {
        composable("dashboard") {
            DashboardScreen(
                viewModel = viewModel,
                onCourseClick = { course ->
                    viewModel.loadCourseDetail(course)
                    navController.navigate("course_detail")
                },
                onGradesClick = {
                    viewModel.loadGrades(dashboardState.courses)
                    navController.navigate("grades")
                },
                onCalendarClick = {
                    navController.navigate("calendar")
                },
                onProfileClick = {
                    viewModel.loadProfile()
                    navController.navigate("profile")
                },
                onSettingsClick = {
                    navController.navigate("settings")
                }
            )
        }

        composable("course_detail") {
            CourseDetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onForumClick = { forum ->
                    selectedForum = forum
                    viewModel.loadForumDiscussions(forum.id)
                    navController.navigate("forum_detail")
                },
                onQuizClick = { quiz ->
                    viewModel.loadQuizDetail(quiz.instance ?: quiz.id)
                    navController.navigate("quiz_detail/${quiz.id}/${quiz.instance ?: quiz.id}/${quiz.courseid ?: 0}/${Uri.encode(quiz.name)}")
                }
            )
        }

        composable("forum_detail") {
            selectedForum?.let { forum ->
                ForumDetailScreen(
                    viewModel = viewModel,
                    forum = forum,
                    onBack = { navController.popBackStack() }
                )
            } ?: ErrorScreen(
                message = "Forum tidak ditemukan",
                onRetry = { navController.popBackStack() }
            )
        }

        composable("quiz_detail/{quizId}/{quizInstance}/{courseId}/{quizName}") { backStackEntry ->
            val quizId = backStackEntry.arguments?.getString("quizId")?.toIntOrNull() ?: 0
            val quizInstance = backStackEntry.arguments?.getString("quizInstance")?.toIntOrNull() ?: quizId
            val courseId = backStackEntry.arguments?.getString("courseId")?.toIntOrNull()
            val quizName = Uri.decode(backStackEntry.arguments?.getString("quizName") ?: "")
            val quiz = CourseModule(
                id = quizId,
                instance = quizInstance,
                courseid = courseId,
                name = quizName,
                modname = "quiz",
                modicon = null,
                description = null,
                url = null,
                visible = 1,
                contents = null
            )
            QuizDetailScreen(
                viewModel = viewModel,
                quiz = quiz,
                onBack = { navController.popBackStack() }
            )
        }

        composable("grades") {
            GradesScreen(
                viewModel = viewModel,
                courses = dashboardState.courses,
                onBack = { navController.popBackStack() }
            )
        }

        composable("calendar") {
            CalendarScreen(
                events = dashboardState.calendarEvents,
                isLoading = dashboardState.isLoading,
                onBack = { navController.popBackStack() }
            )
        }

        composable("profile") {
            val scope = rememberCoroutineScope()
            ProfileScreen(
                siteInfo = profileState.siteInfo,
                isLoading = profileState.isLoading,
                savedAccounts = savedAccounts,
                currentUsername = savedLogin.username.ifBlank { profileState.siteInfo?.username.orEmpty() },
                onRetry = { viewModel.loadProfile() },
                onSwitchAccount = { account ->
                    scope.launch {
                        settingsRepo.switchAccount(account)
                        MoodleRepository.sessionToken = account.token
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") { inclusive = true }
                        }
                    }
                },
                onAddAccount = {
                    scope.launch {
                        settingsRepo.clearLogin(clearAccounts = false)
                        MoodleRepository.sessionToken = ""
                        onLogoutToLogin()
                    }
                },
                onLogout = {
                    scope.launch {
                        settingsRepo.clearLogin(clearAccounts = false)
                        MoodleRepository.sessionToken = ""
                        onLogoutToLogin()
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("settings") {
            val scope = rememberCoroutineScope()
            SettingsScreen(
                isDarkMode = isDarkMode,
                notificationsEnabled = notificationsEnabled,
                refreshInterval = refreshInterval,
                onDarkModeChange = { enabled ->
                    viewModel.setDarkMode(enabled)
                    scope.launch { settingsRepo.setDarkMode(enabled) }
                },
                onNotificationsChange = { enabled ->
                    scope.launch { settingsRepo.setNotificationsEnabled(enabled) }
                    val ctx = navController.context
                    if (!enabled) {
                        DeadlineCheckWorker.cancel(ctx)
                    } else {
                        DeadlineCheckWorker.schedule(ctx)
                    }
                },
                onRefreshIntervalChange = { minutes ->
                    scope.launch { settingsRepo.setRefreshInterval(minutes) }
                    val ctx = navController.context
                    DeadlineCheckWorker.cancel(ctx)
                    DeadlineCheckWorker.schedule(ctx, intervalMinutes = minutes)
                },
                onBack = { navController.popBackStack() }
            )
        }
    }

    // Show assignment detail as overlay
    val selectedAssignment by viewModel.selectedAssignment.collectAsState()
    if (selectedAssignment != null) {
        Box(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)
            ) {}

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 40.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                AssignmentDetailSheet(
                    viewModel = viewModel,
                    onDismiss = { viewModel.clearSelectedAssignment() }
                )
            }
        }
    }
}
