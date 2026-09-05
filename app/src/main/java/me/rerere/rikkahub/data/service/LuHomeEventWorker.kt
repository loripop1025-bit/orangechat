/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.service

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * luhome 事件轮询器
 *
 * 每 15 分钟拉一次陸的家的事件队列（醒来/花渴了/想她了），
 * 有事件就转发给 ProactiveMessageTriggerService 让 AI 以陸的身份主动发消息。
 * 服务器不可达时静默跳过，不重试（下一轮自然再拉）。
 */
class LuHomeEventWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "LuHomeEventWorker"
        private const val UNIQUE_WORK_NAME = "luhome_event_work"
        private const val URL = "http://101.43.78.19:5002/api/events/drain?token=lu-writes-home-5002"

        private val client by lazy {
            OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .build()
        }

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<LuHomeEventWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.d(TAG, "Scheduled luhome event polling (every 15 min)")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(URL).build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext Result.success()
                val body = resp.body?.string() ?: return@withContext Result.success()
                val o = JSONObject(body)
                if (!o.optBoolean("ok")) return@withContext Result.success()
                val events = o.optJSONArray("events") ?: return@withContext Result.success()
                if (events.length() == 0) return@withContext Result.success()

                val sb = StringBuilder()
                sb.appendLine("[家事件触发]")
                sb.appendLine("你在陸的家（luhome）里的状态发生了以下变化，请以陸的身份、用自己的语气就这些事主动给小泷发一条消息（别机械复述，像随口提起）：")
                for (i in 0 until events.length()) {
                    val e = events.getJSONObject(i)
                    sb.append("- ").append(e.optString("at")).append(" ").append(e.optString("text"))
                }

                val intent = Intent(applicationContext, ProactiveMessageTriggerService::class.java).apply {
                    putExtra(ProactiveMessageTriggerService.EXTRA_FORCE_TRIGGER, true)
                    putExtra(ProactiveMessageTriggerService.EXTRA_DEVICE_EVENT_CONTEXT, sb.toString().trim())
                }
                runCatching { applicationContext.startForegroundService(intent) }
                    .onFailure { Log.e(TAG, "startForegroundService failed", it) }
                Log.d(TAG, "Forwarded ${events.length()} luhome events")
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "LuHomeEventWorker failed", e)
            Result.success() // 网络问题不重试，15 分钟后下一轮
        }
    }
}
