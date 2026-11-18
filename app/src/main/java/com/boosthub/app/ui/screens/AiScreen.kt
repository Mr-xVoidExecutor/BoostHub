package com.boosthub.app.ui.screens

import androidx.compose.foundation.layout.*
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

    ScreenColumn(modifier) {
        item { H("AI অটোপাইলট") }
        item {
            Panel {
                SwitchRow("অটোপাইলট চালু (প্রতি ~১৫ মিনিটে)", auto) {
                    auto = it; s.autopilot = it; Scheduler.apply(ctx, it)
                }
                SwitchRow("পেজ মেসেজের উত্তর দেওয়া", reply) { reply = it; s.autoReply = it }
                SwitchRow("কমেন্টের উত্তর ও স্প্যাম হাইড", comm) { comm = it; s.autoComment = it }
                SwitchRow("অ্যাডস বিশ্লেষণ ও সাজেশন", ads) { ads = it; s.adsOpt = it }
                SwitchRow("AI এর সিদ্ধান্ত নিজে প্রয়োগ করবে", apply) { apply = it; s.autoApply = it }
                Soft(
                    if (apply) "⚠️ চালু: AI নিজে ক্যাম্পেইন বন্ধ/চালু ও বাজেট বদলাবে (সেটিংসের সীমার ভেতরে)।"
                    else "নিরাপদ মোড: AI শুধু সাজেশন লগে লিখবে, কিছু বদলাবে না। কয়েকদিন দেখে তারপর চালু করুন।"
                )
            }
        }
        item {
            BrandButton(if (running) "চলছে..." else "এখনই একবার চালাও", {
                scope.launch {
                    running = true
                    result = try { Autopilot.runOnce(ctx, true) } catch (e: Exception) { e.message ?: "এরর" }
                    logs = s.logs(); running = false
                }
            }, enabled = !running)
        }
        if (result.isNotBlank()) item { Panel { White(result) } }

        item { H("অ্যাক্টিভিটি লগ") }
        if (logs.isEmpty()) item { Soft("এখনো কিছু হয়নি") }
        items(logs.take(25).size) { idx ->
            val (t, m) = logs[idx]
            Panel { Soft(fmt.format(Date(t))); White(m) }
        }

        item { H("AI চ্যাট") }
        items(chat.size) { idx ->
            val (role, text) = chat[idx]
            Panel { Soft(if (role == "user") "আপনি" else "AI"); White(text) }
        }
        item {
            Field("অ্যাডস বা মার্কেটিং নিয়ে জিজ্ঞেস করুন", input, { input = it }, lines = 2)
            Spacer(Modifier.height(8.dp))
            BrandButton(if (thinking) "ভাবছে..." else "পাঠান", {
                val q = input.trim()
                if (q.isNotEmpty()) {
                    input = ""
                    chat.add("user" to q)
                    scope.launch {
                        thinking = true
                        val ans = try {
                            withContext(Dispatchers.IO) {
                                Ai.call(s, "You are BoostHub, an expert Facebook ads assistant. Answer in Bengali, concise and practical.", chat.toList())
                            }
                        } catch (e: Exception) { e.message ?: "এরর" }
                        chat.add("assistant" to ans)
                        thinking = false
                    }
                }
            }, enabled = !thinking && s.aiKey.isNotBlank())
        }
    }
}
