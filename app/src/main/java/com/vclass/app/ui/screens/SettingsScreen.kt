package com.vclass.app.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import com.vclass.app.ui.theme.*
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDarkMode: Boolean,
    notificationsEnabled: Boolean,
    refreshInterval: Int,
    onDarkModeChange: (Boolean) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onRefreshIntervalChange: (Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    val currentVersionLabel = remember {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode
        }
        "${packageInfo.versionName} ($versionCode)"
    }
    var showIntervalDialog by remember { mutableStateOf(false) }
    var showUpdateInfo by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<UpdateCheckResult?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ===== TAMPILAN =====
            Text(
                "Tampilan",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Dark mode toggle
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Mode Gelap",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            if (isDarkMode) "Aktif" else "Nonaktif",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { onDarkModeChange(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ===== NOTIFIKASI =====
            Text(
                "Notifikasi",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column {
                    // Notifications toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Notifikasi Deadline",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                if (notificationsEnabled) "Aktif" else "Nonaktif",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { onNotificationsChange(it) }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Refresh interval
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Interval Pengecekan",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Setiap $refreshInterval menit",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(onClick = { showIntervalDialog = true }) {
                            Text("Ubah")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ===== UPDATE =====
            Text(
                "Update aplikasi",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Versi saat ini",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                currentVersionLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isCheckingUpdate = true
                                updateResult = checkForUpdate(context.packageManager.getPackageInfo(context.packageName, 0).let { info ->
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                        info.longVersionCode.toInt()
                                    } else {
                                        @Suppress("DEPRECATION")
                                        info.versionCode
                                    }
                                })
                                isCheckingUpdate = false
                                showUpdateInfo = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isCheckingUpdate
                    ) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mengecek...")
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cek update")
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    PatchNotesCard()
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ===== TENTANG =====
            Text(
                "Tentang",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Versi App",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                currentVersionLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Institusi",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Universitas Gunadarma",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Dibuat oleh",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "rsvmusabb dan Gopan",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                }
            }
        }
    }

    // Interval picker dialog
    if (showIntervalDialog) {
        val intervals = listOf(5, 10, 15, 30, 60, 120)
        AlertDialog(
            onDismissRequest = { showIntervalDialog = false },
            title = { Text("Interval Pengecekan") },
            text = {
                Column {
                    intervals.forEach { interval ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = refreshInterval == interval,
                                onClick = {
                                    onRefreshIntervalChange(interval)
                                    showIntervalDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                when (interval) {
                                    5 -> "5 menit"
                                    10 -> "10 menit"
                                    15 -> "15 menit"
                                    30 -> "30 menit"
                                    60 -> "1 jam"
                                    120 -> "2 jam"
                                    else -> "$interval menit"
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIntervalDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    if (showUpdateInfo) {
        val result = updateResult
        AlertDialog(
            onDismissRequest = { showUpdateInfo = false },
            title = { Text("Update aplikasi") },
            text = {
                Column {
                    when (result) {
                        null -> Text("Belum ada hasil pengecekan.")
                        is UpdateCheckResult.Error -> {
                            Text(
                                result.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ErrorRed
                            )
                        }
                        is UpdateCheckResult.UpToDate -> {
                            Text(
                                "Aplikasi sudah versi terbaru (${result.versionName}).",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        is UpdateCheckResult.UpdateAvailable -> {
                            Text(
                                "Update tersedia: ${result.info.versionName}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            result.info.changelog.forEach { note ->
                                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                    Text("•", color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        note,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (result is UpdateCheckResult.UpdateAvailable) {
                    TextButton(onClick = {
                        showUpdateInfo = false
                        uriHandler.openUri(result.info.apkUrl)
                    }) {
                        Text("Download")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateInfo = false }) {
                    Text("Tutup")
                }
            }
        )
    }

}

private const val UPDATE_JSON_URL =
    "https://raw.githubusercontent.com/Gopyr/Vclass-App/main/releases/update.json"

private data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val changelog: List<String>,
    val mandatory: Boolean
)

private sealed class UpdateCheckResult {
    data class UpdateAvailable(val info: UpdateInfo) : UpdateCheckResult()
    data class UpToDate(val versionName: String) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

private suspend fun checkForUpdate(currentVersionCode: Int): UpdateCheckResult {
    return withContext(Dispatchers.IO) {
        runCatching {
            val raw = URL(UPDATE_JSON_URL).readText()
            val json = JSONObject(raw)
            val changelogArray = json.optJSONArray("changelog")
            val changelog = buildList {
                if (changelogArray != null) {
                    for (index in 0 until changelogArray.length()) {
                        add(changelogArray.optString(index))
                    }
                }
            }.filter { it.isNotBlank() }

            val info = UpdateInfo(
                versionCode = json.optInt("versionCode", 0),
                versionName = json.optString("versionName", "-"),
                apkUrl = json.optString("apkUrl"),
                changelog = changelog,
                mandatory = json.optBoolean("mandatory", false)
            )

            if (info.versionCode > currentVersionCode && info.apkUrl.isNotBlank()) {
                UpdateCheckResult.UpdateAvailable(info)
            } else {
                UpdateCheckResult.UpToDate(info.versionName)
            }
        }.getOrElse { error ->
            UpdateCheckResult.Error(error.message ?: "Gagal mengecek update")
        }
    }
}

@Composable
private fun PatchNotesCard() {
    Column {
        Text(
            "Patch info",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        val notes = listOf(
            "Login native dengan penyimpanan akun opsional",
            "Grade quiz memakai nilai akhir dari grade report",
            "Review quiz diurutkan dari Question 1 sampai selesai",
            "Forum cutoff tampil read-only",
            "Pengaturan update, patch info, dan logout ditambahkan"
        )
        notes.forEach { note ->
            Row(
                modifier = Modifier.padding(vertical = 2.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text("•", color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
