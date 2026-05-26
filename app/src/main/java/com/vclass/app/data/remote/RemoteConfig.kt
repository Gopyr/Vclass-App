package com.vclass.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

data class RemoteConfig(
    val configVersion: Int = 1,
    val quiz: QuizRules = QuizRules(),
    val forum: ForumRules = ForumRules(),
    val patchNotes: List<String> = emptyList()
)

data class QuizRules(
    val rangeMaxPatterns: List<String> = listOf(
        """(?:-|–|to)\s*([0-9]+(?:[.,][0-9]+)?)\s*$"""
    ),
    val closedTexts: List<String> = listOf(
        "closed",
        "close",
        "no more attempts"
    ),
    val noQuestionsTexts: List<String> = listOf(
        "no questions have been added",
        "no questions"
    )
)

data class ForumRules(
    val readOnlyTexts: List<String> = listOf(
        "cut-off date for posting",
        "can no longer post"
    ),
    val showRepliesEvenIfClosed: Boolean = true
)

object RemoteConfigManager {
    private const val REMOTE_CONFIG_URL =
        "https://raw.githubusercontent.com/Gopyr/Vclass-App/main/releases/remote_config.json"

    @Volatile
    var current: RemoteConfig = RemoteConfig()
        private set

    suspend fun refresh(): Result<RemoteConfig> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val raw = URL("$REMOTE_CONFIG_URL?ts=${System.currentTimeMillis()}")
                    .openConnection()
                    .apply {
                        useCaches = false
                        setRequestProperty("Cache-Control", "no-cache")
                        setRequestProperty("Pragma", "no-cache")
                    }
                    .getInputStream()
                    .bufferedReader()
                    .use { it.readText() }

                parse(JSONObject(raw)).also { current = it }
            }
        }
    }

    private fun parse(json: JSONObject): RemoteConfig {
        val quiz = json.optJSONObject("quiz")
        val forum = json.optJSONObject("forum")

        return RemoteConfig(
            configVersion = json.optInt("configVersion", 1),
            quiz = QuizRules(
                rangeMaxPatterns = quiz.stringList("rangeMaxPatterns", RemoteConfig().quiz.rangeMaxPatterns),
                closedTexts = quiz.stringList("closedTexts", RemoteConfig().quiz.closedTexts),
                noQuestionsTexts = quiz.stringList("noQuestionsTexts", RemoteConfig().quiz.noQuestionsTexts)
            ),
            forum = ForumRules(
                readOnlyTexts = forum.stringList("readOnlyTexts", RemoteConfig().forum.readOnlyTexts),
                showRepliesEvenIfClosed = forum?.optBoolean("showRepliesEvenIfClosed", true) ?: true
            ),
            patchNotes = json.stringList("patchNotes", emptyList())
        )
    }

    private fun JSONObject?.stringList(key: String, fallback: List<String>): List<String> {
        val array = this?.optJSONArray(key) ?: return fallback
        return buildList {
            for (index in 0 until array.length()) {
                val value = array.optString(index)
                if (value.isNotBlank()) add(value)
            }
        }.ifEmpty { fallback }
    }
}
