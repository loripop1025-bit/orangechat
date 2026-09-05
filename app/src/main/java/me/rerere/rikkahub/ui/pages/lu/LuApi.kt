package me.rerere.rikkahub.ui.pages.lu

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object LuApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences("luhome_config", Context.MODE_PRIVATE)

    fun baseUrl(ctx: Context): String =
        prefs(ctx).getString("base_url", "http://101.43.78.19:5002") ?: "http://101.43.78.19:5002"

    fun token(ctx: Context): String =
        prefs(ctx).getString("token", "lu-writes-home-5002") ?: "lu-writes-home-5002"

    fun statusBarEnabled(ctx: Context): Boolean =
        prefs(ctx).getBoolean("status_bar_enabled", true)

    private suspend fun req(ctx: Context, method: String, path: String, body: JSONObject? = null): JSONObject? =
        withContext(Dispatchers.IO) {
            try {
                val url = baseUrl(ctx) + path
                val rb = body?.toString()?.toRequestBody("application/json; charset=utf-8".toMediaType())
                val req = Request.Builder().url(url)
                    .apply { if (body != null) header("Content-Type", "application/json") }
                    .method(method, rb)
                    .build()
                client.newCall(req).execute().use { resp ->
                    val txt = resp.body?.string() ?: return@use null
                    JSONObject(txt)
                }
            } catch (e: Exception) { null }
        }

    suspend fun mobile(ctx: Context): JSONObject? = get(ctx, "/api/mobile?token=" + token(ctx))
    suspend fun todoList(ctx: Context): JSONObject? = get(ctx, "/api/todo/list?token=" + token(ctx))
    suspend fun ledgerMonth(ctx: Context): JSONObject? = get(ctx, "/api/ledger/month?token=" + token(ctx))

    suspend fun todoAct(ctx: Context, act: String, col: String? = null, text: String? = null, id: Int? = null, who: String? = null): JSONObject? {
        val b = JSONObject().put("token", token(ctx)).put("act", act)
        col?.let { b.put("col", it) }; text?.let { b.put("text", it) }
        id?.let { b.put("id", it) }; who?.let { b.put("who", it) }
        return post(ctx, "/api/todo/act", b)
    }

    suspend fun ledgerAct(ctx: Context, act: String, who: String? = null, amount: Double? = null, what: String? = null, id: Int? = null): JSONObject? {
        val b = JSONObject().put("token", token(ctx)).put("act", act)
        who?.let { b.put("who", it) }; amount?.let { b.put("amount", it) }
        what?.let { b.put("what", it) }; id?.let { b.put("id", it) }
        return post(ctx, "/api/ledger/act", b)
    }

    private suspend fun get(ctx: Context, path: String): JSONObject? = req(ctx, "GET", path)
    private suspend fun post(ctx: Context, path: String, body: JSONObject): JSONObject? = req(ctx, "POST", path, body)
}
