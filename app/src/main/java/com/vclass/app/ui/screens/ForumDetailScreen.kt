package com.vclass.app.ui.screens

import android.text.Html
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
import com.vclass.app.ui.components.*
import com.vclass.app.ui.theme.*
import com.vclass.app.viewmodel.VClassViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumDetailScreen(
    viewModel: VClassViewModel,
    forum: Forum,
    onBack: () -> Unit
) {
    var discussions by remember { mutableStateOf<List<ForumDiscussion>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedDiscussion by remember { mutableStateOf<ForumDiscussion?>(null) }
    var showNewDiscussionField by remember { mutableStateOf(false) }
    var newDiscussionSubject by remember { mutableStateOf("") }
    var newDiscussionMessage by remember { mutableStateOf("") }
    var isCreatingDiscussion by remember { mutableStateOf(false) }
    val isPostingClosed = forumPostingClosed(forum)
    val canCreateDiscussion = forum.cancreatediscussions == true && !isPostingClosed
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(forum.id) {
        isLoading = true
        val result = viewModel.repository.getForumDiscussions(forum.id)
        result.onSuccess { response ->
            discussions = response.discussions ?: emptyList()
            isLoading = false
        }.onFailure { e ->
            errorMessage = e.message
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        forum.name,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
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
                if (selectedDiscussion != null) {
                    DiscussionPostsScreen(
                        viewModel = viewModel,
                        forum = forum,
                        discussion = selectedDiscussion!!,
                        onBack = { selectedDiscussion = null },
                        modifier = Modifier.padding(padding)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            ForumPostingStatusCard(
                                forum = forum,
                                isPostingClosed = isPostingClosed
                            )
                        }
                        if (canCreateDiscussion) {
                            item {
                                OutlinedButton(
                                    onClick = { showNewDiscussionField = !showNewDiscussionField },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Tambah diskusi")
                                }
                            }
                        }
                        if (showNewDiscussionField && canCreateDiscussion) {
                            item {
                                NewDiscussionCard(
                                    subject = newDiscussionSubject,
                                    message = newDiscussionMessage,
                                    isLoading = isCreatingDiscussion,
                                    onSubjectChange = { newDiscussionSubject = it },
                                    onMessageChange = { newDiscussionMessage = it },
                                    onCancel = {
                                        showNewDiscussionField = false
                                        newDiscussionSubject = ""
                                        newDiscussionMessage = ""
                                    },
                                    onSubmit = {
                                        if (newDiscussionSubject.isNotBlank() && newDiscussionMessage.isNotBlank()) {
                                            coroutineScope.launch {
                                                isCreatingDiscussion = true
                                                viewModel.repository.addNewDiscussion(
                                                    forumId = forum.id,
                                                    subject = newDiscussionSubject,
                                                    message = newDiscussionMessage
                                                ).onSuccess {
                                                    newDiscussionSubject = ""
                                                    newDiscussionMessage = ""
                                                    showNewDiscussionField = false
                                                    viewModel.repository.getForumDiscussions(forum.id).onSuccess { response ->
                                                        discussions = response.discussions ?: emptyList()
                                                    }
                                                }.onFailure { e ->
                                                    errorMessage = e.message
                                                }
                                                isCreatingDiscussion = false
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        if (discussions.isEmpty()) {
                            item {
                                EmptyState("Belum ada diskusi di forum ini")
                            }
                        } else {
                            items(discussions) { discussion ->
                                DiscussionCard(
                                    discussion = discussion,
                                    onClick = { selectedDiscussion = discussion }
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
fun DiscussionCard(
    discussion: ForumDiscussion,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = discussion.subject ?: discussion.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (discussion.pinned == true) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = WarningOrange,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = Html.fromHtml(discussion.message ?: "", Html.FROM_HTML_MODE_COMPACT).toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${discussion.numreplies} replies",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = discussion.timemodified?.toFormattedDate() ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DiscussionPostsScreen(
    viewModel: VClassViewModel,
    forum: Forum,
    discussion: ForumDiscussion,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var posts by remember { mutableStateOf<List<DiscussionPost>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var replyText by remember { mutableStateOf("") }
    var isReplying by remember { mutableStateOf(false) }
    var showReplyField by remember { mutableStateOf(false) }
    val isPostingClosed = forumPostingClosed(forum)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(discussion.id) {
        isLoading = true
        val result = viewModel.repository.getDiscussionPosts(discussion.id)
        result.onSuccess { response ->
            posts = response.posts ?: emptyList()
            isLoading = false
        }.onFailure { e ->
            errorMessage = e.message
            isLoading = false
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Discussion header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = GunadarmaBlue.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = discussion.subject ?: discussion.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = Html.fromHtml(discussion.message ?: "", Html.FROM_HTML_MODE_COMPACT).toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Reply button
        if (!isPostingClosed) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = { showReplyField = !showReplyField }) {
                    Icon(Icons.Default.Reply, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reply")
                }
            }
        } else {
            ForumPostingStatusCard(
                forum = forum,
                isPostingClosed = true,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Reply field
        if (showReplyField && !isPostingClosed) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        label = { Text("Tulis reply...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = {
                            showReplyField = false
                            replyText = ""
                        }) {
                            Text("Batal")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (replyText.isNotBlank()) {
                                    isReplying = true
                                    // Reply to the first post (the discussion starter)
                                    val parentPost = posts.firstOrNull()
                                    if (parentPost != null) {
                                        coroutineScope.launch {
                                            viewModel.repository.addDiscussionReply(
                                                postId = parentPost.id,
                                                message = replyText
                                            ).onSuccess {
                                                replyText = ""
                                                showReplyField = false
                                                // Reload posts
                                                val reload = viewModel.repository.getDiscussionPosts(discussion.id)
                                                reload.onSuccess { response ->
                                                    posts = response.posts ?: emptyList()
                                                }
                                            }
                                            isReplying = false
                                        }
                                    }
                                }
                            },
                            enabled = replyText.isNotBlank() && !isReplying
                        ) {
                            if (isReplying) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Kirim")
                            }
                        }
                    }
                }
            }
        }

        // Posts list
        when {
            isLoading -> LoadingScreen()
            errorMessage != null -> ErrorScreen(message = errorMessage!!, onRetry = {
                isLoading = true
                errorMessage = null
            })
            else -> {
                val apiPosts = flattenDiscussionPosts(posts)
                val starterPost = discussion.toStarterPost()
                val visiblePosts = buildList {
                    add(starterPost)
                    addAll(apiPosts.filter { it.id != starterPost.id })
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(visiblePosts) { post ->
                        PostCard(post = post)
                    }

                    if (apiPosts.isEmpty() && (discussion.numreplies ?: 0) > 0) {
                        item {
                            ForumReplyWarningCard(discussion.numreplies ?: 0)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ForumPostingStatusCard(
    forum: Forum,
    isPostingClosed: Boolean,
    modifier: Modifier = Modifier
) {
    val message = when {
        isPostingClosed -> {
            val closedAt = (forum.cutoffdate ?: forum.duedate)?.takeIf { it > 0 }?.toFormattedDate()
            closedAt?.let { "Forum sudah melewati batas posting ($it). Kamu masih bisa membaca diskusi." }
                ?: "Forum ini sekarang hanya bisa dibaca."
        }
        forum.duedate != null && forum.duedate > 0 -> "Batas posting: ${forum.duedate.toFormattedDate()}"
        else -> "Forum masih menerima diskusi dan reply."
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPostingClosed) WarningOrange.copy(alpha = 0.10f) else GunadarmaBlue.copy(alpha = 0.08f)
        )
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun NewDiscussionCard(
    subject: String,
    message: String,
    isLoading: Boolean,
    onSubjectChange: (String) -> Unit,
    onMessageChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSubmit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            OutlinedTextField(
                value = subject,
                onValueChange = onSubjectChange,
                label = { Text("Judul diskusi") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = message,
                onValueChange = onMessageChange,
                label = { Text("Isi diskusi") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onCancel, enabled = !isLoading) {
                    Text("Batal")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onSubmit,
                    enabled = subject.isNotBlank() && message.isNotBlank() && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Kirim")
                    }
                }
            }
        }
    }
}

fun forumPostingClosed(forum: Forum): Boolean {
    val now = System.currentTimeMillis() / 1000
    val cutoff = forum.cutoffdate?.takeIf { it > 0 }
    val due = forum.duedate?.takeIf { it > 0 }
    return (cutoff != null && cutoff < now) || (cutoff == null && due != null && due < now)
}

fun flattenDiscussionPosts(posts: List<DiscussionPost>): List<DiscussionPost> {
    val flattened = mutableListOf<DiscussionPost>()
    fun visit(post: DiscussionPost) {
        flattened.add(post)
        post.children.orEmpty().forEach(::visit)
    }
    posts.forEach(::visit)
    return flattened
}

@Composable
fun PostCard(post: DiscussionPost) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = post.displayAuthorName(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = post.created?.toFormattedDate() ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!post.subject.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = post.subject,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = Html.fromHtml(post.message ?: "", Html.FROM_HTML_MODE_COMPACT).toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ForumReplyWarningCard(replyCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Reply belum terbaca",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ErrorRed
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "V-Class mencatat ada $replyCount reply, tapi endpoint forum Moodle tidak mengirim isi reply ke app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun ForumDiscussion.toStarterPost(): DiscussionPost {
    return DiscussionPost(
        id = firstpost ?: id,
        parentid = null,
        userid = userid,
        fullname = firstuserfullname ?: userfullname,
        userfullname = userfullname,
        subject = subject ?: name,
        message = message,
        created = timestart ?: timemodified,
        modified = timemodified,
        canreply = false,
        children = emptyList()
    )
}

fun DiscussionPost.displayAuthorName(): String {
    val authorName = author?.get("fullname")?.toString()
    return fullname
        ?: userfullname
        ?: authorName?.takeIf { it.isNotBlank() && it != "null" }
        ?: "Tidak diketahui"
}
