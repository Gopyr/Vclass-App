package com.vclass.app.data.api

import com.vclass.app.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface MoodleApiService {

    @FormUrlEncoded
    @POST("login/token.php")
    suspend fun createToken(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("service") service: String = "moodle_mobile_app"
    ): Response<MoodleTokenResponse>

    // ========== SITE INFO ==========
    @GET("webservice/rest/server.php")
    suspend fun getSiteInfo(
        @Query("wstoken") token: String,
        @Query("wsfunction") function: String = "core_webservice_get_site_info",
        @Query("moodlewsrestformat") format: String = "json"
    ): Response<SiteInfo>

    // ========== COURSES ==========
    @GET("webservice/rest/server.php")
    suspend fun getUserCourses(
        @Query("wstoken") token: String,
        @Query("wsfunction") function: String = "core_enrol_get_users_courses",
        @Query("moodlewsrestformat") format: String = "json",
        @Query("userid") userId: Int
    ): Response<List<Course>>

    @GET("webservice/rest/server.php")
    suspend fun getUserCoursesRaw(
        @Query("wstoken") token: String,
        @Query("wsfunction") function: String = "core_enrol_get_users_courses",
        @Query("moodlewsrestformat") format: String = "json",
        @Query("userid") userId: Int
    ): Response<ResponseBody>

    // ========== COURSE CONTENTS ==========
    @GET("webservice/rest/server.php")
    suspend fun getCourseContents(
        @Query("wstoken") token: String,
        @Query("wsfunction") function: String = "core_course_get_contents",
        @Query("moodlewsrestformat") format: String = "json",
        @Query("courseid") courseId: Int
    ): Response<CourseContentsResponse>

    // ========== ASSIGNMENTS ==========
    @GET("webservice/rest/server.php")
    suspend fun getAssignments(
        @Query("wstoken") token: String,
        @Query("wsfunction") function: String = "mod_assign_get_assignments",
        @Query("moodlewsrestformat") format: String = "json",
        @QueryMap courseIds: Map<String, Int>
    ): Response<AssignmentsResponse>

    // ========== SUBMISSION STATUS ==========
    @GET("webservice/rest/server.php")
    suspend fun getSubmissionStatus(
        @Query("wstoken") token: String,
        @Query("wsfunction") function: String = "mod_assign_get_submission_status",
        @Query("moodlewsrestformat") format: String = "json",
        @Query("assignid") assignId: Int,
        @Query("userid") userId: Int
    ): Response<SubmissionStatusResponse>

    // ========== SUBMIT ASSIGNMENT (save draft with file) ==========
    @GET("webservice/rest/server.php")
    suspend fun saveSubmission(
        @Query("wstoken") token: String,
        @Query("wsfunction") function: String = "mod_assign_save_submission",
        @Query("moodlewsrestformat") format: String = "json",
        @Query("assignmentid") assignmentId: Int,
        @Query("plugindata[onlinetext_editor][text]") onlinetext: String = "",
        @Query("plugindata[onlinetext_editor][format]") editorFormat: Int = 1,
        @Query("plugindata[onlinetext_editor][itemid]") itemId: Int = 0,
        @Query("plugindata[files_filemanager]") fileManagerId: Int = 0
    ): Response<SaveSubmissionResponse>

    // ========== SUBMIT FOR GRADING (final submit) ==========
    @GET("webservice/rest/server.php")
    suspend fun submitForGrading(
        @Query("wstoken") token: String,
        @Query("wsfunction") function: String = "mod_assign_submit_for_grading",
        @Query("moodlewsrestformat") format: String = "json",
        @Query("assignmentid") assignmentId: Int,
        @Query("acceptsubmissionstatement") acceptStatement: Int = 1
    ): Response<SaveSubmissionResponse>

    // ========== REVERT / UNSUBMIT (back to draft) ==========
    @GET("webservice/rest/server.php")
    suspend fun revertSubmissionToDraft(
        @Query("wstoken") token: String,
        @Query("wsfunction") function: String = "mod_assign_revert_submissions_to_draft",
        @Query("moodlewsrestformat") format: String = "json",
        @Query("assignmentid") assignmentId: Int,
        @Query("userid") userId: Int
    ): Response<RevertSubmissionResponse>

    // ========== FILE UPLOAD ==========
    @Multipart
    @POST("webservice/upload.php")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part,
        @Query("token") token: String
    ): Response<List<UploadedFile>>

    // ========== FORUM: GET DISCUSSION POSTS ==========
    @GET("webservice/rest/server.php")
    suspend fun getForumDiscussionPosts(
        @Query("wstoken") token: String,
        @Query("wsfunction") function: String = "mod_forum_get_discussion_posts",
        @Query("moodlewsrestformat") format: String = "json",
        @Query("discussionid") discussionId: Int
    ): Response<DiscussionPostsResponse>

    @GET("webservice/rest/server.php")
    suspend fun getLegacyForumDiscussionPosts(
        @Query("wstoken") token: String,
        @Query("wsfunction") function: String = "mod_forum_get_forum_discussion_posts",
        @Query("moodlewsrestformat") format: String = "json",
        @Query("discussionid") discussionId: Int,
        @Query("sortby") sortBy: String = "created",
        @Query("sortdirection") sortDirection: String = "ASC"
    ): Response<DiscussionPostsResponse>

    // ========== FORUM: GET DISCUSSIONS ==========
    @GET("webservice/rest/server.php")
    suspend fun getForumDiscussions(
        @Query("wstoken") token: String,
        @Query("wsfunction") function: String = "mod_forum_get_forum_discussions",
        @Query("moodlewsrestformat") format: String = "json",
        @Query("forumid") forumId: Int
    ): Response<ForumDiscussionsResponse>

    // ========== FORUM: ADD DISCUSSION POST (reply) ==========
    @FormUrlEncoded
    @POST("webservice/rest/server.php")
    suspend fun addDiscussionPost(
        @Query("wstoken") token: String,
        @Query("wsfunction") function: String = "mod_forum_add_discussion_post",
        @Query("moodlewsrestformat") format: String = "json",
        @Query("postid") postId: Int,
        @Field("message") message: String,
        @Field("subject") subject: String? = null
    ): Response<AddPostResponse>

    // ========== NEW DISCUSSION ==========
    @FormUrlEncoded
    @POST("webservice/rest/server.php")
    suspend fun addNewDiscussion(
        @Query("wstoken") token: String,
        @Query("wsfunction") function: String = "mod_forum_add_discussion",
        @Query("moodlewsrestformat") format: String = "json",
        @Query("forumid") forumId: Int,
        @Field("subject") subject: String,
        @Field("message") message: String
    ): Response<AddDiscussionResponse>

    // ========== QUIZ: GET USER ATTEMPTS ==========
    @GET("webservice/rest/server.php")
    suspend fun getQuizUserAttempts(
        @Query("wstoken") token: String,
        @Query("wsfunction") function: String = "mod_quiz_get_user_attempts",
        @Query("moodlewsrestformat") format: String = "json",
        @Query("quizid") quizId: Int,
        @Query("userid") userId: Int
    ): Response<QuizAttemptsResponse>

    // ========== QUIZ: START ATTEMPT ==========
    @GET("webservice/rest/server.php")
    suspend fun startQuizAttempt(
        @Query("wstoken") token: String,
        @Query("wsfunction") function: String = "mod_quiz_start_attempt",
        @Query("moodlewsrestformat") format: String = "json",
        @Query("quizid") quizId: Int
    ): Response<QuizAttemptResponse>

    // ========== QUIZ: GET ATTEMPT REVIEW (question + answer review) ==========
    @GET("webservice/rest/server.php")
    suspend fun getQuizAttemptReview(
        @Query("wstoken") token: String,
        @Query("wsfunction") function: String = "mod_quiz_get_attempt_review",
        @Query("moodlewsrestformat") format: String = "json",
        @Query("attemptid") attemptId: Int
    ): Response<QuizAttemptReviewResponse>

    // ========== QUIZ: PROCESS ATTEMPT (submit answers) ==========
    @FormUrlEncoded
    @POST("webservice/rest/server.php")
    suspend fun processQuizAttempt(
        @Query("wstoken") token: String,
        @Query("wsfunction") function: String = "mod_quiz_process_attempt",
        @Query("moodlewsrestformat") format: String = "json",
        @Field("attemptid") attemptId: Int,
        @Field("data[0][name]") dataName: String,
        @Field("data[0][value]") dataValue: String,
        @Field("finishattempt") finishAttempt: Int = 1
    ): Response<ProcessAttemptResponse>

    // ========== QUIZ: GET QUIZ ACCESS INFO ==========
    @GET("webservice/rest/server.php")
    suspend fun getQuizAccessInformation(
        @Query("wstoken") token: String,
        @Query("wsfunction") function: String = "mod_quiz_get_quiz_access_information",
        @Query("moodlewsrestformat") format: String = "json",
        @Query("quizid") quizId: Int
    ): Response<QuizAccessResponse>

    // ========== FORUMS LIST ==========
    @GET("webservice/rest/server.php")
    suspend fun getForums(
        @Query("wstoken") token: String,
        @Query("wsfunction") function: String = "mod_forum_get_forums_by_courses",
        @Query("moodlewsrestformat") format: String = "json",
        @QueryMap courseIds: Map<String, Int>
    ): Response<List<Forum>>

    // ========== CALENDAR ==========
    @GET("webservice/rest/server.php")
    suspend fun getCalendarEvents(
        @Query("wstoken") token: String,
        @Query("wsfunction") function: String = "core_calendar_get_calendar_upcoming_view",
        @Query("moodlewsrestformat") format: String = "json"
    ): Response<CalendarEventsResponse>

    // ========== GRADES ==========
    @GET("webservice/rest/server.php")
    suspend fun getGrades(
        @Query("wstoken") token: String,
        @Query("wsfunction") function: String = "gradereport_user_get_grades_table",
        @Query("moodlewsrestformat") format: String = "json",
        @Query("courseid") courseId: Int,
        @Query("userid") userId: Int
    ): Response<GradesTableResponse>

    // ========== ALL GRADES (across courses) ==========
    @GET("webservice/rest/server.php")
    suspend fun getAllGrades(
        @Query("wstoken") token: String,
        @Query("wsfunction") function: String = "gradereport_user_get_grades_table",
        @Query("moodlewsrestformat") format: String = "json",
        @Query("userid") userId: Int
    ): Response<GradesTableResponse>

}
