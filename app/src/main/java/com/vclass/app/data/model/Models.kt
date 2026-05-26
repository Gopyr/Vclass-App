package com.vclass.app.data.model

import com.google.gson.annotations.SerializedName

// ========== SITE INFO ==========
data class SiteInfo(
    val sitename: String,
    val username: String,
    val firstname: String,
    val lastname: String,
    val fullname: String,
    val userid: Int,
    val siteurl: String
)

// ========== COURSES ==========
data class UserCoursesResponse(
    val courses: List<Course>? = null,
    val warnings: List<Warning>? = null
)

data class Course(
    val id: Int,
    val shortname: String,
    val fullname: String,
    val displayname: String? = null,
    val enrolledusercount: Int? = null,
    val visible: Int? = null,
    val summary: String? = null,
    val format: String? = null,
    val showgrades: Boolean? = null,
    val category: Int? = null,
    val progress: Double? = null,
    val completed: Boolean? = null,
    val startdate: Long? = null,
    val enddate: Long? = null,
    val lastaccess: Long? = null,
    val isfavourite: Boolean? = null,
    val hidden: Boolean? = null
)

// ========== COURSE CONTENTS ==========
typealias CourseContentsResponse = List<CourseSection>

data class CourseSection(
    val id: Int,
    val name: String,
    val summary: String? = null,
    val visible: Int? = null,
    val modules: List<CourseModule>? = null
)

data class CourseModule(
    val id: Int,
    val instance: Int? = null,
    val courseid: Int? = null,
    val name: String,
    val modname: String, // "assign", "resource", "forum", "quiz", etc.
    val modicon: String? = null,
    val description: String? = null,
    val url: String? = null,
    val visible: Int? = null,
    val contents: List<ModuleContent>? = null
)

data class ModuleContent(
    val filename: String? = null,
    val filepath: String? = null,
    val filesize: Long? = null,
    val fileurl: String? = null,
    val timemodified: Long? = null,
    val mimetype: String? = null,
    val type: String? = null
)

// ========== ASSIGNMENTS ==========
data class AssignmentsResponse(
    val courses: List<AssignmentCourse>? = null,
    val warnings: List<Warning>? = null
)

data class AssignmentCourse(
    val id: Int,
    val fullname: String,
    val shortname: String,
    val timemodified: Long? = null,
    val assignments: List<Assignment>? = null
)

data class Assignment(
    val id: Int,
    val cmid: Int,
    val course: Int,
    val name: String,
    val nosubmissions: Int? = null,
    val submissiondrafts: Int? = null,
    val sendnotifications: Int? = null,
    val sendlatenotifications: Int? = null,
    val sendstudentnotifications: Int? = null,
    val duedate: Long? = null,
    val allowsubmissionsfromdate: Long? = null,
    val grade: Int? = null,
    val timemodified: Long? = null,
    val completionsubmit: Int? = null,
    val cutoffdate: Long? = null,
    val gradingduedate: Long? = null,
    val teamsubmission: Int? = null,
    val attemptreopenmethod: String? = null,
    val maxattempts: Int? = null,
    val blindmarking: Int? = null,
    val hidegrader: Int? = null,
    val revealidentities: Int? = null,
    val intro: String? = null,
    val introformat: Int? = null,
    val introfiles: List<ModuleContent>? = null,
    val introattachments: List<ModuleContent>? = null
)

// ========== SUBMISSION STATUS ==========
data class SubmissionStatusResponse(
    val lastattempt: LastAttempt? = null,
    val warnings: List<Warning>? = null
)

data class LastAttempt(
    val submission: Submission? = null,
    val submissionsenabled: Boolean? = null,
    val locked: Boolean? = null,
    val graded: Boolean? = null,
    val canedit: Boolean? = null,
    val caneditowner: Boolean? = null,
    val cansubmit: Boolean? = null,
    val cansave: Boolean? = null,
    val extensionduedate: Long? = null,
    val blindmarking: Boolean? = null,
    val gradingstatus: String? = null,
    val usergroups: List<Any>? = null
)

data class Submission(
    val id: Int,
    val userid: Int,
    val attemptnumber: Int,
    val timecreated: Long,
    val timemodified: Long,
    val status: String, // "submitted", "draft", "new"
    val groupid: Int? = null,
    val assignment: Int? = null,
    val latest: Int? = null,
    val plugins: List<SubmissionPlugin>? = null
)

data class SubmissionPlugin(
    val type: String,
    val name: String,
    val fileareas: List<FileArea>? = null
)

data class FileArea(
    val area: String,
    val files: List<ModuleContent>? = null
)

// ========== FORUMS ==========
data class ForumsResponse(
    val forums: List<Forum>? = null,
    val warnings: List<Warning>? = null
)

data class Forum(
    val id: Int,
    val course: Int,
    val type: String, // "news", "qanda", "general", etc.
    val name: String,
    val intro: String? = null,
    val introformat: Int? = null,
    val duedate: Long? = null,
    val cutoffdate: Long? = null,
    val assessed: Int? = null,
    val scale: Int? = null,
    val maxbytes: Long? = null,
    val maxattachments: Int? = null,
    val forcesubscribe: Int? = null,
    val trackingtype: Int? = null,
    val rsstype: Int? = null,
    val rssarticles: Int? = null,
    val timemodified: Long? = null,
    val numdiscussions: Int? = null,
    val cancreatediscussions: Boolean? = null,
    val lockdiscussionafter: Long? = null,
    val istracked: Boolean? = null
)

// ========== CALENDAR EVENTS ==========
data class CalendarEventsResponse(
    val events: List<CalendarEvent>? = null,
    val warnings: List<Warning>? = null
)

data class CalendarEvent(
    val id: Int,
    val name: String,
    val description: String? = null,
    val format: Int? = null,
    val courseid: Int? = null,
    val course: EventCourse? = null,
    val groupid: Int? = null,
    val userid: Int? = null,
    val repeatid: Int? = null,
    val modulename: String? = null,
    val instance: Int? = null,
    val eventtype: String? = null,
    val timestart: Long,
    val timeduration: Long? = null,
    val timesort: Long? = null,
    val visible: Int? = null,
    val icon: EventIcon? = null
)

data class EventCourse(
    val id: Int,
    val fullname: String,
    val shortname: String
)

data class EventIcon(
    val key: String,
    val component: String,
    val alttext: String
)

// ========== GRADES ==========
data class GradesTableResponse(
    val tables: List<GradeTable>? = null,
    val warnings: List<Warning>? = null
)

data class GradeTable(
    val courseid: Int,
    val userid: Int,
    val userfullname: String,
    val maxdepth: Int,
    val tabledata: List<GradeTableData>? = null
)

data class GradeTableData(
    val itemname: GradeCell? = null,
    val weight: GradeCell? = null,
    val grade: GradeCell? = null,
    val range: GradeCell? = null,
    val percentage: GradeCell? = null,
    val feedback: GradeCell? = null,
    val contributiontocoursetotal: GradeCell? = null
)

data class GradeCell(
    val cssClass: String? = null,
    val colspan: Int? = null,
    val content: String? = null,
    val celltype: String? = null,
    val id: String? = null
)

// ========== COMMON ==========
data class Warning(
    val item: String? = null,
    val itemid: Int? = null,
    val warningcode: String? = null,
    val message: String
)

// ========== MOODLE ERROR ==========
data class MoodleError(
    val exception: String? = null,
    val errorcode: String? = null,
    val message: String? = null,
    val debuginfo: String? = null
)

data class MoodleTokenResponse(
    val token: String? = null,
    val privatetoken: String? = null,
    val error: String? = null,
    val errorcode: String? = null,
    val stacktrace: String? = null,
    val debuginfo: String? = null,
    val reproductionlink: String? = null
)

// ========== SUBMISSION ==========
data class SaveSubmissionResponse(
    val status: Boolean? = null,
    val warnings: List<Warning>? = null
)

data class UploadedFile(
    val filename: String? = null,
    val filepath: String? = null,
    val filesize: Long? = null,
    val fileurl: String? = null,
    val timemodified: Long? = null,
    val mimetype: String? = null,
    val itemid: Int? = null
)

// ========== FORUM ==========
data class ForumDiscussionsResponse(
    val discussions: List<ForumDiscussion>? = null,
    val warnings: List<Warning>? = null
)

data class ForumDiscussion(
    val id: Int,
    val forumid: Int? = null,
    val forum: Int? = null,
    val name: String,
    val subject: String? = null,
    val message: String? = null,
    val userid: Int? = null,
    val userfullname: String? = null,
    val firstpost: Int? = null,
    val firstuserfullname: String? = null,
    val firstuserpicture: Int? = null,
    val firstuseremail: String? = null,
    val lastpost: Int? = null,
    val lastuserid: Int? = null,
    val lastuserfullname: String? = null,
    val groupid: Int? = null,
    val timestart: Long? = null,
    val timeend: Long? = null,
    val numreplies: Int? = null,
    val timemodified: Long? = null,
    val usermodified: Int? = null,
    val starred: Boolean? = null,
    val locked: Boolean? = null,
    val pinned: Boolean? = null,
    val attachments: List<ModuleContent>? = null
)

data class DiscussionPostsResponse(
    val posts: List<DiscussionPost>? = null,
    val warnings: List<Warning>? = null,
    val discussion: ForumDiscussion? = null,
    val courseid: Int? = null,
    val forumid: Int? = null,
    val ratinginfo: Any? = null
)

data class DiscussionPost(
    val id: Int,
    val parentid: Int? = null,
    val userid: Int? = null,
    val fullname: String? = null,
    val userfullname: String? = null,
    val author: Map<String, Any>? = null,
    val subject: String? = null,
    val message: String? = null,
    val created: Long? = null,
    val modified: Long? = null,
    val canreply: Boolean? = null,
    val unread: Boolean? = null,
    val isprivatereply: Boolean? = null,
    val attachments: List<ModuleContent>? = null,
    val children: List<DiscussionPost>? = null,
    val profileimageurl: String? = null
)

data class AddPostResponse(
    val postid: Int? = null,
    val warnings: List<Warning>? = null
)

data class AddDiscussionResponse(
    val discussionid: Int? = null,
    val warnings: List<Warning>? = null
)

// ========== QUIZ ==========
data class QuizAttemptsResponse(
    val attempts: List<QuizAttemptInfo>? = null,
    val warnings: List<Warning>? = null
)

data class QuizAttemptInfo(
    val id: Int,
    val quiz: Int? = null,
    val userid: Int? = null,
    val attempt: Int? = null,
    val uniqueid: String? = null,
    val layout: String? = null,
    val currentpage: Int? = null,
    val preview: Int? = null,
    val state: String? = null,
    val timestart: Long? = null,
    val timefinish: Long? = null,
    val timemodified: Long? = null,
    val timemodifiedoffline: Long? = null,
    val timecheckstate: Long? = null,
    val sumgrades: Double? = null,
    val grade: Double? = null
)

data class QuizAttemptResponse(
    val attempt: QuizAttemptInfo? = null,
    val warnings: List<Warning>? = null
)

data class QuizAttemptReviewResponse(
    val attempt: QuizAttemptInfo? = null,
    val additionaldata: List<Map<String, Any>>? = null,
    val questions: List<QuizQuestion>? = null,
    val warnings: List<Warning>? = null
)

data class QuizQuestion(
    val slot: Int? = null,
    val type: String? = null,
    val page: Int? = null,
    val html: String? = null,
    val sequencecheck: Int? = null,
    val flag: Boolean? = null,
    val state: String? = null,
    val status: String? = null,
    val blockedbyprevious: Boolean? = null,
    val mark: String? = null,
    val maxmark: String? = null,
    val options: List<Map<String, Any>>? = null
)

data class ProcessAttemptResponse(
    val state: String? = null,
    val messages: List<String>? = null,
    val warnings: List<Warning>? = null
)

data class QuizAccessResponse(
    val canattempt: Boolean? = null,
    val canpreview: Boolean? = null,
    val canreviewmyattempts: Boolean? = null,
    val canmanage: Boolean? = null,
    val canviewreports: Boolean? = null,
    val accessrules: List<String>? = null,
    val activerulenames: List<String>? = null,
    val preventaccessreasons: List<String>? = null,
    val warnings: List<Warning>? = null
)

// ========== REVERT / UNSUBMIT ==========
data class RevertSubmissionResponse(
    val status: Boolean? = null,
    val warnings: List<Warning>? = null
)
