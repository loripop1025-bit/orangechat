package me.rerere.rikkahub.ui.pages.lu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.ui.components.nav.LocalNavController

@Composable
fun LuHomeHubPage() {
    val nav = LocalNavController.current
    val ctx = LocalContext.current
    var baseUrl by remember { mutableStateOf(LuApi.baseUrl(ctx)) }
    var token by remember { mutableStateOf(LuApi.token(ctx)) }
    var barOn by remember { mutableStateOf(LuApi.statusBarEnabled(ctx)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { nav.popBackStack() }) { Text("‹") }
            Text("陸的家", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth().clickable { nav.navigate(Screen.WebView(LuApi.baseUrl(ctx))) },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("🏠 家", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("户型图 · 日记 · 待办 · 账本 · 散步地图", fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
            }
        }
        Spacer(Modifier.height(10.dp))
        Card(
            modifier = Modifier.fillMaxWidth().clickable { nav.navigate(Screen.LuTodo) },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("✅ 待办", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("她的 / 他的 / 我们的", fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f))
            }
        }
        Spacer(Modifier.height(10.dp))
        Card(
            modifier = Modifier.fillMaxWidth().clickable { nav.navigate(Screen.LuLedger) },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("💰 账本", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("两个人的开销，一笔一笔", fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("设置", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("聊天页顶部状态栏", fontSize = 14.sp)
            Switch(checked = barOn, onCheckedChange = {
                barOn = it
                LuApi.prefs(ctx).edit().putBoolean("status_bar_enabled", it).apply()
            })
        }
        OutlinedTextField(
            value = baseUrl, onValueChange = { baseUrl = it },
            label = { Text("服务器地址") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = token, onValueChange = { token = it },
            label = { Text("Token") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = {
            LuApi.prefs(ctx).edit()
                .putString("base_url", baseUrl.trim().trimEnd('/'))
                .putString("token", token.trim())
                .apply()
        }) { Text("保存") }
    }
}
