/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.ai.transformers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 共享待办差异推送转换器
 *
 * 只注入"上次注入之后发生的待办变化"（谁加了什么/谁完成了什么），
 * 无变化零token。数据在服务器 todo.py。
 */
object TodoDiffTransformer : InputMessageTransformer {
    private const val BASE = "http://101.43.78.19:5002"
    private const val TOKEN = "lu-writes-home-5002"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .build()
    }

    @Volatile
    private var lastSeq: Int = -1

    override suspend fun transform(
        ctx: TransformerContext,
        messages: List<UIMessage>,
    ): List<UIMessage> {
        val text = fetchDiff() ?: return messages
        val idx = messages.indexOfLast { it.role == MessageRole.USER }
        if (idx < 0) return messages
        return messages.toMutableList().apply {
            add(idx, UIMessage.user("<todo>$text</todo>"))
        }
    }

    private suspend fun fetchDiff(): String? = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE/api/todo/diff?token=$TOKEN&since=${if (lastSeq < 0) 0 else lastSeq}"
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val o = JSONObject(resp.body?.string() ?: return@use null)
                if (!o.optBoolean("ok")) return@use null
                val seq = o.optInt("seq", 0)
                if (seq == lastSeq) return@use null // 无变化
                val events = o.optJSONArray("events") ?: return@use null
                if (events.length() == 0) {
                    lastSeq = seq
                    return@use null
                }
                lastSeq = seq
                val sb = StringBuilder()
                for (i in 0 until events.length()) {
                    if (i > 0) sb.append("；")
                    val e = events.optJSONObject(i) ?: continue
                    val who = e.optString("by")
                    val col = when (e.optString("col")) { "taki" -> "小泷"; "lu" -> "陸"; else -> "我们" }
                    val text = e.optString("text")
                    when (e.optString("type")) {
                        "add" -> sb.append("$who 加了${col}的待办「$text」")
                        "done" -> sb.append("$who 完成了${col}的待办「$text」")
                        "undo" -> sb.append("$who 取消完成了${col}的待办「$text」")
                    }
                }
                sb.toString()
            }
        } catch (e: Exception) {
            null
        }
    }
}
