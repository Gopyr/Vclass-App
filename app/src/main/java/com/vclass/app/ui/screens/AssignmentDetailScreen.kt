package com.vclass.app.ui.screens

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import java.io.File
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentDetailSheet(
    viewModel: VClassViewModel,
    onDismiss: () -> Unit
) {
    val assignment by viewModel.selectedAssignment.collectAsState()
    val submissionStatus by viewModel.selectedSubmissionStatus.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    if (assignment == null) return

    val a = assignment!!
    val isSubmitted = submissionStatus?.lastattempt?.submission?.status == "submitted"
    val isGraded = submissionStatus?.lastattempt?.graded == true
    val canEdit = submissionStatus?.lastattempt?.canedit == true
    val canSubmit = submissionStatus?.lastattempt?.cansubmit == true
        || !isSubmitted
        || (isSubmitted && canEdit && !isGraded)

    // Collect selectionCounter to force re-fetch each time sheet opens
    val selectionCounter by viewModel.selectionCounter.collectAsState()

    // Fetch fresh submission status from server every time sheet opens (even same assignment)
    LaunchedEffect(selectionCounter) {
        viewModel.fetchAndSyncSubmissionStatus(a.id)
    }

    // File picker launcher
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                selectedFileName = cursor.getString(nameIndex) ?: "file"
            } ?: run {
                selectedFileName = "file_${System.currentTimeMillis()}"
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Detail Tugas",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.refreshSelectedSubmissionStatus() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Assignment name
            Text(
                text = a.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Status
            InfoRow(
                icon = Icons.Default.Info,
                label = "Status",
                value = when {
                    isGraded -> "Sudah Dinilai"
                    isSubmitted && canEdit -> "Sudah Submit (bisa edit)"
                    isSubmitted -> "Sudah Submit"
                    else -> "Belum Submit"
                },
                valueColor = when {
                    isGraded -> SuccessGreen
                    isSubmitted && canEdit -> WarningOrange
                    isSubmitted -> GunadarmaBlue
                    else -> WarningOrange
                }
            )

            // Due date
            InfoRow(
                icon = Icons.Default.Schedule,
                label = "Deadline",
                value = a.duedate?.toFormattedDate() ?: "No due date"
            )

            // Due date relative
            InfoRow(
                icon = Icons.Default.Timer,
                label = "Sisa Waktu",
                value = a.duedate?.toRelativeTime() ?: "No deadline",
                valueColor = if ((a.duedate?.toRelativeTime() ?: "") == "Overdue") ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Grade
            InfoRow(
                icon = Icons.Default.Grade,
                label = "Nilai Maks",
                value = "${a.grade ?: 0}"
            )

            // Submission files
            if (isSubmitted) {
                val files = submissionStatus?.lastattempt?.submission?.plugins
                    ?.flatMap { it.fileareas ?: emptyList() }
                    ?.flatMap { it.files ?: emptyList() }

                if (!files.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "File yang di-submit:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    files.forEach { file ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = SuccessGreen.copy(alpha = 0.08f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.InsertDriveFile,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.filename ?: "Unknown",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = formatFileSize(file.filesize ?: 0),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = "Download",
                                    tint = SuccessGreen,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable {
                                            val url = file.fileurl
                                            if (!url.isNullOrBlank()) {
                                                downloadFile(context, url, file.filename ?: "file", "Submission: ${a.name}")
                                            }
                                        }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }

            // ===== SUBMIT / RE-SUBMIT SECTION =====
            if (canSubmit && !isGraded) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    if (isSubmitted) "Submit Ulang:" else "Submit Tugas:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                // File picker
                OutlinedButton(
                    onClick = { filePickerLauncher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (selectedFileName.isBlank()) "Pilih File" else selectedFileName)
                }

                if (selectedFileUri != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = GunadarmaBlue.copy(alpha = 0.08f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                tint = GunadarmaBlue
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectedFileName,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                selectedFileUri = null
                                selectedFileName = ""
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Submit with file button
                Button(
                    onClick = {
                        selectedFileUri?.let { uri ->
                            coroutineScope.launch {
                                isSubmitting = true
                                try {
                                    val inputStream = context.contentResolver.openInputStream(uri)
                                    val cacheFile = File(context.cacheDir, selectedFileName)
                                    inputStream?.use { input ->
                                        cacheFile.outputStream().use { output ->
                                            input.copyTo(output)
                                        }
                                    }

                                    val uploadResult = viewModel.repository.uploadFile(cacheFile)
                                    if (uploadResult.isSuccess) {
                                        val uploadedFile = uploadResult.getOrNull()
                                        if (uploadedFile != null) {
                                            val saveResult = viewModel.repository.submitAssignment(a.id, uploadedFile.itemid ?: 0)
                                            if (saveResult.isSuccess) {
                                                submitAndFinish(context, a, viewModel)
                                            } else {
                                                Toast.makeText(context, "Submit gagal: ${saveResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                                isSubmitting = false
                                            }
                                        } else {
                                            Toast.makeText(context, "Upload returned empty", Toast.LENGTH_LONG).show()
                                            isSubmitting = false
                                        }
                                    } else {
                                        Toast.makeText(context, "Upload gagal: ${uploadResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                        isSubmitting = false
                                    }
                                    cacheFile.delete()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                    isSubmitting = false
                                }
                            }
                        } ?: run {
                            // No file selected — submit for grading directly
                            coroutineScope.launch {
                                isSubmitting = true
                                submitAndFinish(context, a, viewModel)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = GunadarmaBlue)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isSubmitted) "Mengirim Ulang..." else "Submitting...")
                    } else {
                        Icon(
                            if (isSubmitted) Icons.Default.Refresh else Icons.Default.Upload,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isSubmitted) "Submit Ulang" else "Submit Tugas")
                    }
                }

                // "Submit tanpa file baru" button for re-submit case
                if (isSubmitted && canEdit) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                isSubmitting = true
                                submitAndFinish(context, a, viewModel)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSubmitting
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Submit Ulang (tanpa file baru)")
                    }
                }
            }

            // Intro / description
            if (!a.intro.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Deskripsi:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = android.text.Html.fromHtml(a.intro, android.text.Html.FROM_HTML_MODE_COMPACT).toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Intro attachments (soal)
            if (!a.introattachments.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "File Soal:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                a.introattachments.forEach { file ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = GunadarmaBlue.copy(alpha = 0.08f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AttachFile,
                                contentDescription = null,
                                tint = GunadarmaBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.filename ?: "Unknown",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = formatFileSize(file.filesize ?: 0),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.Default.Download,
                                contentDescription = "Download",
                                tint = GunadarmaBlue,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable {
                                        val url = file.fileurl
                                        if (!url.isNullOrBlank()) {
                                            downloadFile(context, url, file.filename ?: "file", "Soal: ${a.name}")
                                        }
                                    }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

// ========== SUBMIT HELPER ==========
private suspend fun submitAndFinish(
    context: Context,
    a: Assignment,
    viewModel: VClassViewModel
) {
    val gradingResult = viewModel.repository.submitForGrading(a.id)
    if (gradingResult.isSuccess) {
        Toast.makeText(context, "Tugas berhasil di-submit!", Toast.LENGTH_SHORT).show()
        viewModel.selectAssignment(a)
    } else {
        Toast.makeText(context, "Submit final gagal: ${gradingResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}
