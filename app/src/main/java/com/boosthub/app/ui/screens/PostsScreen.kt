package com.boosthub.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.boosthub.app.data.Ai
import com.boosthub.app.data.MetaApi
import com.boosthub.app.data.Store
import com.boosthub.app.ui.components.*
import com.boosthub.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PostsScreen(modifier: Modifier = Modifier, onBack: (() -> Unit)? = null) {
    val ctx = LocalContext.current
    val s = remember { Store(ctx) }
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var adCopy by remember { mutableStateOf("") }
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
            catch (e: Exception) { msg = e.message ?: "Error" }
        }
    }

    LaunchedEffect(Unit) {
        if (s.fbToken.isNotBlank() && s.pageId.isNotBlank()) loadPosts()
        else msg = "Add your Facebook token and Page ID in Settings first."
    }

    val t = target
    if (t != null) {
        val total = (budget.toLongOrNull() ?: 0L) * (days.toLongOrNull() ?: 0L)
        AppDialog(
            title = "Boost post",
            confirmText = "Start boost",
            onConfirm = {
                val b = budget.toLongOrNull() ?: 0L
                val d = days.toIntOrNull() ?: 0
                if (b <= 0 || d <= 0) {
                    msg = "Enter a valid budget and number of days."
                } else {
                    target = null
                    scope.launch {
                        busy = true; msg = "Creating boost..."
                        msg = try {
                            val id = withContext(Dispatchers.IO) {
                                MetaApi.boostPost(s, t.id, b * 100, d, countries, ageMin.toIntOrNull() ?: 18, ageMax.toIntOrNull() ?: 45)
                            }
                            "Boost started (campaign $id). Check the Ads tab."
                        } catch (e: Exception) { e.message ?: "Error" }
                        busy = false
                    }
                }
            },
            onDismiss = { target = null }
        ) {
            Muted(t.message.take(100))
            Field("Daily budget", budget, { budget = it.filter { c -> c.isDigit() } })
            Field("Number of days", days, { days = it.filter { c -> c.isDigit() } })
            Field("Country codes (comma separated, e.g. BD,IN)", countries, { countries = it })
            Field("Minimum age", ageMin, { ageMin = it.filter { c -> c.isDigit() } })
            Field("Maximum age", ageMax, { ageMax = it.filter { c -> c.isDigit() } })
            Notice("This will spend real money. Maximum spend: about $total (account currency).", isError = true)
        }
    }

    ScreenColumn(modifier, title = "Posts & Boost", onBack = onBack) {
        if (msg.isNotBlank()) item { Notice(msg) }

        item {
            Panel {
                CardHeader(
                    Icons.Rounded.Article, BrandPink, "New post",
                    "Write your own text or let AI write it from an idea"
                )
                Field("Post text or idea", text, { text = it }, lines = 5)
                Spacer(Modifier.height(10.dp))
                Field("Publish after (hours). Leave empty to publish now", hours, { hours = it.filter { c -> c.isDigit() } })
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlineButton(
                        "Write with AI",
                        {
                            scope.launch {
                                busy = true
                                try { text = withContext(Dispatchers.IO) { Ai.writePost(s, text) } }
                                catch (e: Exception) { msg = e.message ?: "Error" }
                                busy = false
                            }
                        },
                        Modifier.weight(1f),
                        enabled = !busy && text.isNotBlank() && s.aiKey.isNotBlank(),
                        icon = Icons.Rounded.AutoAwesome
                    )
                    BrandButton(
                        if (hours.isBlank()) "Publish" else "Schedule",
                        {
                            scope.launch {
                                busy = true
                                try {
                                    val at = hours.toLongOrNull()?.let { System.currentTimeMillis() / 1000 + it * 3600 }
                                    withContext(Dispatchers.IO) { MetaApi.publishPost(s, text, at) }
                                    msg = if (at == null) "Post published." else "Post scheduled."
                                    text = ""; hours = ""
                                    loadPosts()
                                } catch (e: Exception) { msg = e.message ?: "Error" }
                                busy = false
                            }
                        },
                        Modifier.weight(1f),
                        enabled = !busy && text.isNotBlank(),
                        icon = if (hours.isBlank()) Icons.Rounded.Send else Icons.Rounded.Schedule
                    )
                }
                Spacer(Modifier.height(10.dp))
                OutlineButton(
                    "Generate 3 ad copies",
                    {
                        scope.launch {
                            busy = true; adCopy = "AI is writing..."
                            adCopy = try { withContext(Dispatchers.IO) { Ai.adCopy(s, text) } } catch (e: Exception) { e.message ?: "Error" }
                            busy = false
                        }
                    },
                    Modifier.fillMaxWidth(),
                    enabled = !busy && text.isNotBlank() && s.aiKey.isNotBlank(),
                    icon = Icons.Rounded.Campaign
                )
            }
        }
        if (adCopy.isNotBlank()) item {
            Panel {
                CardHeader(Icons.Rounded.AutoAwesome, BrandPurple, "Ad copy ideas")
                Body(adCopy)
            }
        }

        item { SectionTitle("Recent posts") }
        if (posts.isEmpty()) item { Muted("No posts loaded yet.") }
        items(posts.size) { idx ->
            val p = posts[idx]
            Panel {
                Row(verticalAlignment = Alignment.Top) {
                    IconTile(Icons.Rounded.Article, BrandBlue, 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Body(p.message.ifBlank { "(no text)" }.take(140), Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                BrandButton(
                    "Boost post",
                    { target = p },
                    Modifier.fillMaxWidth(),
                    enabled = !busy && s.adAccount.isNotBlank(),
                    icon = Icons.Rounded.RocketLaunch
                )
            }
        }
    }
}
