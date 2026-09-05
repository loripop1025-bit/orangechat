package me.rerere.rikkahub.ui.pages.lu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.rerere.rikkahub.ui.components.nav.LocalNavController
import org.json.JSONArray

private val COLS = listOf("taki" to "她的", "lu" to "他的", "ours" to "我们的")

@Composable
fun LuTodoPage() {
    val nav = LocalNavController.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var todos by remember { mutableStateOf<JSONArray?>(null) }
    var tab by remember { mutableIntStateOf(0) }
    var input by remember { mutableStateOf("") }

    fun load() {
        scope.launch { todos = LuApi.todoList(ctx)?.optJSONArray("todos") }
    }

    LaunchedEffect(Unit) {
        while (true) {
            todos = LuApi.todoList(ctx)?.optJSONArray("todos")
            delay(30_000)
        }
    }

    val col = COLS[tab].first

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { nav.popBackStack() }) { Text("‹") }
            Text("待办", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        TabRow(selectedTabIndex = tab) {
            COLS.forEachIndexed { i, (_, label) ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label) })
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input, onValueChange = { input = it },
                placeholder = { Text("加一条到「${COLS[tab].second}」…") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(Modifier.width(6.dp))
            TextButton(onClick = {
                if (input.isNotBlank()) {
                    val t = input
                    input = ""
                    scope.launch {
                        LuApi.todoAct(ctx, "add", col = col, text = t, who = "taki")
                        todos = LuApi.todoList(ctx)?.optJSONArray("todos")
                    }
                }
            }) { Text("加") }
        }
        Spacer(Modifier.height(8.dp))
        val list = todos ?: JSONArray()
        val filtered = (0 until list.length()).mapNotNull { list.optJSONObject(it) }
            .filter { it.optString("col") == col }
        LazyColumn(Modifier.fillMaxSize()) {
            items(filtered) { t ->
                val done = t.optBoolean("done")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) {
                    Checkbox(checked = done, onCheckedChange = { _ ->
                        scope.launch {
                            LuApi.todoAct(ctx, "toggle", id = t.optInt("id"), who = "taki")
                            todos = LuApi.todoList(ctx)?.optJSONArray("todos")
                        }
                    })
                    Text(
                        t.optString("text"),
                        fontSize = 15.sp,
                        textDecoration = if (done) TextDecoration.LineThrough else null,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = {
                        scope.launch {
                            LuApi.todoAct(ctx, "remove", id = t.optInt("id"))
                            todos = LuApi.todoList(ctx)?.optJSONArray("todos")
                        }
                    }) { Text("✕", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}
