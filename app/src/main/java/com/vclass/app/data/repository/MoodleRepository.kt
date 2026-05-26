package com.vclass.app.data.repository

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.vclass.app.data.api.RetrofitClient
import com.vclass.app.data.model.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class MoodleRepository {

    private val api = RetrofitClient.apiService
    private val gson = Gson()
    private var token = sessionToken
    private var userId = 0

    companion object {
        var sessionToken: String = ""
    }

    suspend fun login(username: String, password: String): Result<String> {
        return try {
            val response = api.createToken(username = username, password = password)
            val body = response.body()
            val newToken = body?.token

            if (response.isSuccessful && !newToken.isNullOrBlank()) {
                sessionToken = newToken
                token = newToken
                Result.success(newToken)
            } else {
                Result.failure(Exception(body?.error ?: "Login gagal"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun useToken(savedToken: String) {
        sessionToken = savedToken
        token = savedToken
    }

    private suspend fun ensureUserId() {
        if (userId <= 0) {
            getSiteInfo().onSuccess { info -> userId = info.userid }
        }
    }

    // ========== COURSES ==========
    suspend fun getUserCourses(): Result<List<Course>> {
        return try {
            if (token.isBlank()) {
                return Result.failure(Exception("Silakan login terlebih dahulu"))
            }
            ensureUserId()
            val response = api.getUserCoursesRaw(token, userId = userId)
            if (response.isSuccessful) {
                val rawBody = response.body()?.string().orEmpty()
                val json = JsonParser.parseString(rawBody)

                if (json.isJsonArray) {
                    val type = object : TypeToken<List<Course>>() {}.type
                    Result.success(gson.fromJson(json, type))
                } else {
                    val moodleError = gson.fromJson(json, MoodleError::class.java)
                    Result.failure(Exception(moodleError.message ?: "Moodle returned an unexpected response"))
                }
            } else {
                Result.failure(Exception("Failed to get courses: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== COURSE CONTENTS ==========
    suspend fun getCourseContents(courseId: Int): Result<CourseContentsResponse> {
        return try {
            val response = api.getCourseContents(token, courseId = courseId)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Failed to get course contents: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== ASSIGNMENTS ==========
    suspend fun getAssignments(courseIds: List<Int>): Result<AssignmentsResponse> {
        return try {
            val courseIdsMap = courseIds.mapIndexed { index, id -> "courseids[$index]" to id }.toMap()
            val response = api.getAssignments(token, courseIds = courseIdsMap)
            if (response.isSuccessful) {
                Result.success(response.body() ?: AssignmentsResponse())
            } else {
                Result.failure(Exception("Failed to get assignments: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== SUBMISSION STATUS ==========
    suspend fun getSubmissionStatus(assignId: Int): Result<SubmissionStatusResponse> {
        return try {
            ensureUserId()
            val response = api.getSubmissionStatus(token, assignId = assignId, userId = userId)
            if (response.isSuccessful) {
                Result.success(response.body() ?: SubmissionStatusResponse())
            } else {
                Result.failure(Exception("Failed to get submission status: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== UPLOAD FILE ==========
    suspend fun uploadFile(file: File): Result<UploadedFile> {
        return try {
            val requestFile = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val response = api.uploadFile(body, token)
            if (response.isSuccessful) {
                val files = response.body()
                if (!files.isNullOrEmpty()) {
                    Result.success(files[0])
                } else {
                    Result.failure(Exception("Upload returned empty"))
                }
            } else {
                Result.failure(Exception("Upload failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== SUBMIT ASSIGNMENT (save draft with file) ==========
    suspend fun submitAssignment(assignmentId: Int, fileManagerId: Int): Result<SaveSubmissionResponse> {
        return try {
            val response = api.saveSubmission(
                token = token,
                assignmentId = assignmentId,
                fileManagerId = fileManagerId
            )
            if (response.isSuccessful) {
                val body = response.body()
                // Check for warnings/errors in response
                val warnings = body?.warnings
                if (warnings != null && warnings.isNotEmpty()) {
                    val errorMsg = warnings.joinToString("; ") { it.message }
                    Result.failure(Exception("Submit warning: $errorMsg"))
                } else {
                    Result.success(body ?: SaveSubmissionResponse())
                }
            } else {
                Result.failure(Exception("Submit failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== SUBMIT FOR GRADING (final submit) ==========
    suspend fun submitForGrading(assignmentId: Int): Result<SaveSubmissionResponse> {
        return try {
            val response = api.submitForGrading(
                token = token,
                assignmentId = assignmentId
            )
            if (response.isSuccessful) {
                val body = response.body()
                val warnings = body?.warnings
                if (warnings != null && warnings.isNotEmpty()) {
                    val errorMsg = warnings.joinToString("; ") { it.message }
                    Result.failure(Exception("Submit warning: $errorMsg"))
                } else {
                    Result.success(body ?: SaveSubmissionResponse())
                }
            } else {
                Result.failure(Exception("Submit for grading failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== REVERT / UNSUBMIT (back to draft) ==========
    suspend fun revertSubmissionToDraft(assignmentId: Int): Result<RevertSubmissionResponse> {
        return try {
            ensureUserId()
            val response = api.revertSubmissionToDraft(
                token = token,
                assignmentId = assignmentId,
                userId = userId
            )
            if (response.isSuccessful) {
                val body = response.body()
                val warnings = body?.warnings
                if (warnings != null && warnings.isNotEmpty()) {
                    val errorMsg = warnings.joinToString("; ") { it.message }
                    Result.failure(Exception("Revert warning: $errorMsg"))
                } else {
                    Result.success(body ?: RevertSubmissionResponse())
                }
            } else {
                Result.failure(Exception("Revert failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== FORUM: GET DISCUSSIONS ==========
    suspend fun getForumDiscussions(forumId: Int): Result<ForumDiscussionsResponse> {
        return try {
            val response = api.getForumDiscussions(token, forumId = forumId)
            if (response.isSuccessful) {
                Result.success(response.body() ?: ForumDiscussionsResponse())
            } else {
                Result.failure(Exception("Failed to get discussions: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== FORUM: GET DISCUSSION POSTS ==========
    suspend fun getDiscussionPosts(discussionId: Int): Result<DiscussionPostsResponse> {
        return try {
            val response = api.getForumDiscussionPosts(token, discussionId = discussionId)
            if (response.isSuccessful) {
                val body = response.body() ?: DiscussionPostsResponse()
                if (body.posts.isNullOrEmpty()) {
                    val legacyResponse = api.getLegacyForumDiscussionPosts(token, discussionId = discussionId)
                    if (legacyResponse.isSuccessful) {
                        Result.success(legacyResponse.body() ?: body)
                    } else {
                        Result.success(body)
                    }
                } else {
                    Result.success(body)
                }
            } else {
                val legacyResponse = api.getLegacyForumDiscussionPosts(token, discussionId = discussionId)
                if (legacyResponse.isSuccessful) {
                    Result.success(legacyResponse.body() ?: DiscussionPostsResponse())
                } else {
                    Result.failure(Exception("Failed to get posts: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== FORUM: ADD REPLY ==========
    suspend fun addDiscussionReply(postId: Int, message: String): Result<AddPostResponse> {
        return try {
            val response = api.addDiscussionPost(
                token = token,
                postId = postId,
                message = message
            )
            if (response.isSuccessful) {
                Result.success(response.body() ?: AddPostResponse())
            } else {
                Result.failure(Exception("Failed to reply: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== FORUM: NEW DISCUSSION ==========
    suspend fun addNewDiscussion(forumId: Int, subject: String, message: String): Result<AddDiscussionResponse> {
        return try {
            val response = api.addNewDiscussion(
                token = token,
                forumId = forumId,
                subject = subject,
                message = message
            )
            if (response.isSuccessful) {
                Result.success(response.body() ?: AddDiscussionResponse())
            } else {
                Result.failure(Exception("Failed to create discussion: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== QUIZ: GET USER ATTEMPTS ==========
    suspend fun getQuizUserAttempts(quizId: Int): Result<QuizAttemptsResponse> {
        return try {
            ensureUserId()
            val response = api.getQuizUserAttempts(token, quizId = quizId, userId = userId)
            if (response.isSuccessful) {
                Result.success(response.body() ?: QuizAttemptsResponse())
            } else {
                Result.failure(Exception("Failed to get quiz attempts: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== QUIZ: START ATTEMPT ==========
    suspend fun startQuizAttempt(quizId: Int): Result<QuizAttemptResponse> {
        return try {
            val response = api.startQuizAttempt(token, quizId = quizId)
            if (response.isSuccessful) {
                Result.success(response.body() ?: QuizAttemptResponse())
            } else {
                Result.failure(Exception("Failed to start quiz: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== QUIZ: GET ATTEMPT REVIEW ==========
    suspend fun getQuizAttemptReview(attemptId: Int): Result<QuizAttemptReviewResponse> {
        return try {
            val response = api.getQuizAttemptReview(token, attemptId = attemptId)
            if (response.isSuccessful) {
                Result.success(response.body() ?: QuizAttemptReviewResponse())
            } else {
                Result.failure(Exception("Failed to get attempt review: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== QUIZ: PROCESS ATTEMPT ==========
    suspend fun processQuizAttempt(attemptId: Int, dataName: String, dataValue: String, finish: Boolean = true): Result<ProcessAttemptResponse> {
        return try {
            val response = api.processQuizAttempt(
                token = token,
                attemptId = attemptId,
                dataName = dataName,
                dataValue = dataValue,
                finishAttempt = if (finish) 1 else 0
            )
            if (response.isSuccessful) {
                Result.success(response.body() ?: ProcessAttemptResponse())
            } else {
                Result.failure(Exception("Failed to process attempt: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== QUIZ: GET ACCESS INFO ==========
    suspend fun getQuizAccessInfo(quizId: Int): Result<QuizAccessResponse> {
        return try {
            val response = api.getQuizAccessInformation(token, quizId = quizId)
            if (response.isSuccessful) {
                Result.success(response.body() ?: QuizAccessResponse())
            } else {
                Result.failure(Exception("Failed to get quiz access: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== FORUMS ==========
    suspend fun getForums(courseIds: List<Int>): Result<List<Forum>> {
        return try {
            val courseIdsMap = courseIds.mapIndexed { index, id -> "courseids[$index]" to id }.toMap()
            val response = api.getForums(token, courseIds = courseIdsMap)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Failed to get forums: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== CALENDAR ==========
    suspend fun getCalendarEvents(): Result<CalendarEventsResponse> {
        return try {
            val response = api.getCalendarEvents(token)
            if (response.isSuccessful) {
                Result.success(response.body() ?: CalendarEventsResponse())
            } else {
                Result.failure(Exception("Failed to get calendar events: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== GRADES ==========
    suspend fun getGrades(courseId: Int): Result<GradesTableResponse> {
        return try {
            ensureUserId()
            val response = api.getGrades(token, courseId = courseId, userId = userId)
            if (response.isSuccessful) {
                Result.success(response.body() ?: GradesTableResponse())
            } else {
                Result.failure(Exception("Failed to get grades: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== SITE INFO ==========
    suspend fun getSiteInfo(): Result<SiteInfo> {
        return try {
            if (token.isBlank()) {
                return Result.failure(Exception("Silakan login terlebih dahulu"))
            }
            val response = api.getSiteInfo(token)
            if (response.isSuccessful) {
                Result.success(response.body() ?: SiteInfo("", "", "", "", "", 0, ""))
            } else {
                Result.failure(Exception("Failed to get site info: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== ALL GRADES ==========
    suspend fun getAllGrades(): Result<GradesTableResponse> {
        return try {
            ensureUserId()
            val response = api.getAllGrades(token, userId = userId)
            if (response.isSuccessful) {
                Result.success(response.body() ?: GradesTableResponse())
            } else {
                Result.failure(Exception("Failed to get all grades: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
