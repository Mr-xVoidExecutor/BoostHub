package com.boosthub.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.boosthub.app.data.Ai
import com.boosthub.app.data.MetaApi
import com.boosthub.app.data.Store
import com.boosthub.app.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PostsScreen(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val s = remember { Store(ctx) }
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var copy by remember { mutableStateOf("") }
    var posts by remember { mutableStateOf<List<MetaApi.Post>>(emptyList()) }
    var target by remember { mutableStateOf<MetaApi.Post?>(null) }
    var budget by remember { mutableStateOf("5") }
    var days by remember { mutableStateOf("3") }
    var countries by remember { mutableStateOf("BD") }
    var ageMin by remember { mutableStateOf("18") }
    var ageMax by remember { mutableStateOf("45") }

    fun loadPosts() {
        scope.launch {
            try { posts = withContext(Dispatchers.IO) { MetaApi.pagePosts(s) } }
            catch (e: Exception) { msg = e.message ?: "এরর" }
        }
    }

    LaunchedEffect(Unit) {
        if (s.fbToken.isNotBlank() && s.pageId.isNotBlank()) loadPosts()
        else msg = "আগে সেটিংসে ফেসবুক টোকেন ও পেজ আইডি দিন"
    }

    val t = target
    if (t != null) {
        val total = (budget.toLongOrNull() ?: 0L) * (days.toLongOrNull() ?: 0L)
        AlertDialog(
            onDismissRequest = { target = null },
            title = { Text("পোস্ট বুস্ট") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Soft(t.message.take(100))
                    Field("দৈনিক বাজেট", budget, { budget = it.filter { c -> c.isDigit() } })
                    Field("কত দিন", days, { days = it.filter { c -> c.isDigit() } })
                    Field("দেশ কোড (কমা দিয়ে, যেমন BD,IN)", countries, { countries = it })
                    Field("বয়স (সর্বনিম্ন)", ageMin, { ageMin = it.filter { c -> c.isDigit() } })
                    Field("বয়স (সর্বোচ্চ)", ageMax, { ageMax = it.filter { c -> c.isDigit() } })
                    White("⚠️ সর্বোচ্চ খরচ: প্রায় $total (অ্যাকাউন্টের মুদ্রায়)", bold = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val b = budget.toLongOrNull() ?: 0L
                    val d = days.toIntOrNull() ?: 0
                    if (b <= 0 || d <= 0) { msg = "বাজেট ও দিন সঠিক দিন"; return@TextButton }
                    target = null
                    scope.launch {
                        busy = true; msg = "বুস্ট তৈরি হচ্ছে..."
                        msg = try {
                            val id = withContext(Dispatchers.IO) {
                                MetaApi.boostPost(s, t.id, b * 100, d, countries, ageMin.toIntOrNull() ?: 18, ageMax.toIntOrNull() ?: 45)
                            }
                            "বুস্ট চালু হয়েছে ✅ (ক্যাম্পেইন $id)। অ্যাডস ট্যাবে দেখুন।"
                        } catch (e: Exception) { e.message ?: "এরর" }
                        busy = false
                    }
                }) { Text("বুস্ট শুরু করুন (খরচ হবে)") }
            },
            dismissButton = { TextButton(onClick = { target = null }) { Text("বাতিল") } }
        )
    }

    ScreenColumn(modifier) {
        item { H("পোস্ট ও বুস্ট") }
        if (msg.isNotBlank()) item { Panel { White(msg) } }

        item {
            Panel {
                White("নতুন পোস্ট", bold = true)
                Soft("আইডিয়া লিখে AI দিয়ে ক্যাপশন বানাতে পারেন, বা নিজে লিখুন")
                Spacer(Modifier.height(8.dp))
                Field("পোস্টের লেখা বা আইডিয়া", text, { text = it }, lines = 5)
                Spacer(Modifier.height(8.dp))
                Field("কত ঘণ্টা পরে পাবলিশ (ফাঁকা = এখনই)", hours, { hours = it.filter { c -> c.isDigit() } })
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BrandButton("AI দিয়ে লিখুন", {
                        scope.launch {
                            busy = true
                            try { text = withContext(Dispatchers.IO) { Ai.writePost(s, text) } }
                            catch (e: Exception) { msg = e.message ?: "এরর" }
                            busy = false
                        }
                    }, enabled = !busy && text.isNotBlank() && s.aiKey.isNotBlank())
                    BrandButton(if (hours.isBlank()) "পাবলিশ" else "শিডিউল", {
                        scope.launch {
                            busy = true
                            try {
                                val at = hours.toLongOrNull()?.let { System.currentTimeMillis() / 1000 + it * 3600 }
                                withContext(Dispatchers.IO) { MetaApi.publishPost(s, text, at) }
                                msg = if (at == null) "পাবলিশ হয়েছে ✅" else "শিডিউল হয়েছে ✅"
                                text = ""; hours = ""
                                loadPosts()
                            } catch (e: Exception) { msg = e.message ?: "এরর" }
                            busy = false
                        }
                    }, enabled = !busy && text.isNotBlank())
                }
                Spacer(Modifier.height(8.dp))
                BrandButton("৩টা অ্যাড কপি বানান", {
                    scope.launch {
                        busy = true; copy = "AI লিখছে..."
                        copy = try { withContext(Dispatchers.IO) { Ai.adCopy(s, text) } } catch (e: Exception) { e.message ?: "এরর" }
                        busy = false
                    }
                }, enabled = !busy && text.isNotBlank() && s.aiKey.isNotBlank())
            }
        }
        if (copy.isNotBlank()) item { Panel { White(copy) } }

        item { H("আগের পোস্ট") }
        items(posts.size) { idx ->
            val p = posts[idx]
            Panel {
                White(p.message.ifBlank { "(লেখা নেই)" }.take(140))
                Spacer(Modifier.height(8.dp))
                BrandButton("বুস্ট করুন", { target = p }, enabled = !busy && s.adAccount.isNotBlank())
            }
        }
    }
}
