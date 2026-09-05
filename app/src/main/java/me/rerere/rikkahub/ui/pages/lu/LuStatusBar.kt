package me.rerere.rikkahub.ui.pages.lu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.json.JSONObject

@Composable
fun LuStatusBar(onOpen: () -> Unit) {
    val ctx = LocalContext.current
    var data by remember { mutableStateOf<JSONObject?>(null) }
    var enabled by remember { mutableStateOf(LuApi.statusBarEnabled(ctx)) }

    LaunchedEffect(Unit) {
        while (true) {
            enabled = LuApi.statusBarEnabled(ctx)
            if (enabled) {
                data = LuApi.mobile(ctx)
            } else {
                data = null
            }
            delay(60_000)
        }
    }

    if (!enabled || data == null || data?.optBoolean("ok") != true) return

    val room = data?.optString("room") ?: ""
    val act = data?.optString("activity") ?: ""
    val chips = data?.optJSONArray("chips")?.let { arr ->
        (0 until arr.length()).joinToString(" ") { arr.optString(it) }
    } ?: ""
    val since = data?.optInt("since_min", 0) ?: 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onOpen() }
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row {
            Text(
                "陸 · $room",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "  $act" + if (since > 0) "（${since}分）" else "",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (chips.isNotEmpty()) {
            Text(
                chips,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
