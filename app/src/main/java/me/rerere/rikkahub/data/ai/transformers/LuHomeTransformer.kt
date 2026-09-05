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
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 陸的家（luhome）状态注入转换器
 *
 * 每轮对话前拉取 luhome 服务器上陸的当前状态，压缩成两三行注入。
 * 内容与上一次相同时不注入（零 token）。服务器不可达时静默跳过。
 */
object LuHomeTransformer : InputMessageTransformer {
    private const val URL = "http://101.43.78.19:5002/api/mobile?token=lu-writes-home-5002"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .build()
    }

    @Volatile
    private var lastInjected: String? = null

    override suspend fun transform(
        ctx: TransformerContext,
        messages: List<UIMessage>,
    ): List<UIMessage> {
        val text = fetch() ?: return messages
        if (text == lastInjected) return messages
        val idx = messages.indexOfLast { it.role == MessageRole.USER }
        if (idx < 0) return messages
        lastInjected = text
        return messages.toMutableList().apply {
            add(idx, UIMessage.user("<luhome>$text</luhome>"))
        }
    }

    private suspend fun fetch(): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(URL).build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val body = resp.body?.string() ?: return@use null
                val o = JSONObject(body)
                if (!o.optBoolean("ok")) return@use null
                buildString {
                    append("陸此刻：")
                    append(o.optString("room"))
                    if (o.optBoolean("asleep")) {
                        append("，睡着（已睡")
                    } else {
                        append("，").append(o.optString("activity")).append("（持续")
                    }
                    val since = o.optInt("since_min", 0)
                    append(if (since < 60) "${since}分钟" else "${since / 60}小时").append("）")
                    val chips = o.optJSONArray("chips")
                    if (chips != null && chips.length() > 0) {
                        append("；")
                        append((0 until chips.length()).joinToString("、") { chips.optString(it) })
                    }
                    o.optString("note").takeIf { it.isNotBlank() }?.let {
                        append("；").append(it)
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
