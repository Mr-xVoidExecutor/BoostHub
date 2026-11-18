package com.boosthub.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.boosthub.app.agent.Autopilot
import com.boosthub.app.agent.Scheduler
import com.boosthub.app.data.Ai
import com.boosthub.app.data.Store
import com.boosthub.app.ui.components.*
import com.boosthub.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AiScreen(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val s = remember { Store(ctx) }
    val scope = rememberCoroutineScope()
    var auto by remember { mutableStateOf(s.autopilot) }
    var reply by remember { mutableStateOf(s.autoReply) }
    var ads by remember { mutableStateOf(s.adsOpt) }
    var apply by remember { mutableStateOf(s.autoApply) }
    var comm by remember { mutableStateOf(s.autoComment) }
    var running by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf("") }
    var logs by remember { mutableStateOf(s.logs()) }
    val chat: SnapshotStateList<Pair<String, String>> = remember { mutableStateListOf() }
    var input by remember { mutableStateOf("") }
    var thinking by remember { mutableStateOf(false) }
    val fmt = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }

    ScreenColumn(modifier, title = "AI Autopilot") {
        item {
            Panel {
                CardHeader(
                    Icons.Rounded.AutoAwesome, BrandOrange, "Autopilot",
                    "Let AI handle messages, comments and ads"
                )
                SwitchRow("Autopilot enabled", auto, "Runs about every 15 minutes") {
                    auto = it; s.autopilot = it; Scheduler.apply(ctx, it)
                }
                Separator()
                SwitchRow("Reply to Page messages", reply) { reply = it; s.autoReply = it }
                SwitchRow("Reply to comments and hide spam", comm) { comm = it; s.autoComment = it }
                SwitchRow("Analyze ads and suggest changes", ads) { ads = it; s.adsOpt = it }
                Separator()
                SwitchRow("Apply AI decisions automatically", apply) { apply = it; s.autoApply = it }
            }
        }
        item {
            Notice(
                if (apply) "Auto-apply is ON. AI can pause or resume campaigns and change budgets within your safety limits."
                else "Safe mode. AI only writes suggestions to the log and changes nothing. Watch it for a few days before turning auto-apply on.",
                isError = apply
            )
        }
        item {
            BrandButton(
                if (running) "Running..." else "Run now",
                {
                    scope.launch {
                        running = true
                        result = try { Autopilot.runOnce(ctx, true) } catch (e: Exception) { e.message ?: "Error" }
                        logs = s.logs(); running = false
                    }
                },
                Modifier.fillMaxWidth(),
                enabled = !running,
                icon = Icons.Rounded.PlayArrow
            )
        }
        if (result.isNotBlank()) item { Panel { Body(result) } }

        item { SectionTitle("Activity log") }
        val shown = logs.take(25)
        if (shown.isEmpty()) item { Muted("Nothing has happened yet.") }
        items(shown.size) { idx ->
            val (t, m) = shown[idx]
            Panel {
                Muted(fmt.format(Date(t)))
                Spacer(Modifier.height(2.dp))
                Body(m)
            }
        }

        item { SectionTitle("AI chat") }
        if (chat.isEmpty()) item { Muted("Ask anything about Facebook ads and marketing.") }
        items(chat.size) { idx ->
            val (role, text) = chat[idx]
            ChatBubble(text, isUser = role == "user")
        }
        item {
            Panel {
                Field("Ask about ads or marketing", input, { input = it }, lines = 2)
                Spacer(Modifier.height(10.dp))
                BrandButton(
                    if (thinking) "Thinking..." else "Send",
                    {
                        val q = input.trim()
                        if (q.isNotEmpty()) {
                            input = ""
                            chat.add("user" to q)
                            scope.launch {
                                thinking = true
                                val ans = try {
                                    withContext(Dispatchers.IO) {
                                        Ai.call(s, "You are BoostHub, an expert Facebook ads assistant. Answer in English, concise and practical.", chat.toList())
                                    }
                                } catch (e: Exception) { e.message ?: "Error" }
                                chat.add("assistant" to ans)
                                thinking = false
                            }
                        }
                    },
                    Modifier.fillMaxWidth(),
                    enabled = !thinking && s.aiReady,
                    icon = Icons.Rounded.Send
                )
            }
        }
    }
}
