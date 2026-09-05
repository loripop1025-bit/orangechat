package me.rerere.rikkahub.ui.pages.lu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.rerere.rikkahub.ui.components.nav.LocalNavController
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun LuLedgerPage() {
    val nav = LocalNavController.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var data by remember { mutableStateOf<JSONObject?>(null) }
    var who by remember { mutableStateOf("taki") }
    var amount by remember { mutableStateOf("") }
    var what by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<JSONObject?>(null) }
    var editAmount by remember { mutableStateOf("") }
    var editWhat by remember { mutableStateOf("") }

    fun load() {
        scope.launch { data = LuApi.ledgerMonth(ctx) }
    }

    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { nav.popBackStack() }) { Text("‹") }
            Text("账本 · " + (data?.optString("month") ?: ""), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("小泷", fontSize = 13.sp)
                    Text("¥" + (data?.optDouble("taki_total", 0.0) ?: 0.0),
                        fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("陸", fontSize = 13.sp)
                    Text("¥" + (data?.optDouble("lu_total", 0.0) ?: 0.0),
                        fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { who = "taki" }) {
                Text("小泷", color = if (who == "taki") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.width(6.dp))
            OutlinedButton(onClick = { who = "lu" }) {
                Text("陸", color = if (who == "lu") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = amount, onValueChange = { amount = it },
                label = { Text("金额") },
                modifier = Modifier.weight(0.35f), singleLine = true
            )
            Spacer(Modifier.width(6.dp))
            OutlinedTextField(
                value = what, onValueChange = { what = it },
                label = { Text("花在哪") },
                modifier = Modifier.weight(1f), singleLine = true
            )
        }
        TextButton(onClick = {
            val amt = amount.toDoubleOrNull()
            if (amt != null && what.isNotBlank()) {
                scope.launch {
                    LuApi.ledgerAct(ctx, "add", who = who, amount = amt, what = what)
                    amount = ""; what = ""
                    data = LuApi.ledgerMonth(ctx)
                }
            }
        }) { Text("记一笔") }

        // 明细：固定一块区域内部滚动
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column {
                val es = data?.optJSONArray("entries") ?: JSONArray()
                (0 until es.length()).forEach { i ->
                    val e = es.optJSONObject(i) ?: return@forEach
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                (if (e.optString("who") == "lu") "陸" else "小泷") + "  ¥" + e.optDouble("amount", 0.0) +
                                    "  " + e.optString("what"),
                                fontSize = 14.sp
                            )
                            Text(
                                e.optString("date") + " " + e.optString("at"),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        TextButton(onClick = {
                            editing = e
                            editAmount = e.optDouble("amount", 0.0).toString()
                            editWhat = e.optString("what")
                        }) { Text("改", fontSize = 12.sp) }
                        TextButton(onClick = {
                            scope.launch {
                                LuApi.ledgerAct(ctx, "remove", id = e.optInt("id"))
                                data = LuApi.ledgerMonth(ctx)
                            }
                        }) { Text("删", fontSize = 12.sp, color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }

    editing?.let { e ->
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("改这笔") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editAmount, onValueChange = { editAmount = it },
                        label = { Text("金额") }, singleLine = true
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = editWhat, onValueChange = { editWhat = it },
                        label = { Text("花在哪") }, singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amt = editAmount.toDoubleOrNull()
                    if (amt != null) {
                        scope.launch {
                            LuApi.ledgerAct(ctx, "edit", id = e.optInt("id"), amount = amt, what = editWhat)
                            data = LuApi.ledgerMonth(ctx)
                        }
                    }
                    editing = null
                }) { Text("存") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("算了") } }
        )
    }
}
