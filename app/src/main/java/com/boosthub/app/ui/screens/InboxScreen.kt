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
fun InboxScreen(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val s = remember { Store(ctx) }
    val scope = rememberCoroutineScope()
    var convs by remember { mutableStateOf<List<MetaApi.Conv>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    var target by remember { mutableStateOf<MetaApi.Conv?>(null) }
    var draft by remember { mutableStateOf("") }

    fun load() {
        scope.launch {
            loading = true; msg = ""
            try { convs = withContext(Dispatchers.IO) { MetaApi.conversations(s) } }
            catch (e: Exception) { msg = e.message ?: "Error" }
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        if (s.fbReady && s.pageId.isNotBlank()) load()
        else msg = "Connect your server in Settings first."
    }

    val t0 = target
    if (t0 != null) {
        AppDialog(
            title = "Reply to ${t0.name}",
            confirmText = "Send",
            onConfirm = {
                target = null
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) { MetaApi.sendMessage(s, t0.psid, draft) }
                        msg = "Message sent."
                        load()
                    } catch (e: Exception) { msg = e.message ?: "Error" }
                }
            },
            onDismiss = { target = null }
        ) {
            Field("Reply (you can edit)", draft, { draft = it }, lines = 4)
        }
    }

    ScreenColumn(modifier, title = "Page Inbox") {
        item {
            OutlineButton(
                if (loading) "Loading..." else "Refresh",
                { load() },
                Modifier.fillMaxWidth(),
                enabled = !loading,
                icon = Icons.Rounded.Refresh
            )
        }
        if (msg.isNotBlank()) item { Notice(msg) }
        if (convs.isEmpty() && !loading && msg.isBlank()) item { Panel { Muted("No conversations found.") } }
        items(convs.size) { idx ->
            val c = convs[idx]
            val needsReply = !c.lastFromPage && c.psid.isNotBlank()
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Rounded.Person, BrandPurple, 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Body(c.name, bold = true)
                        Muted(c.lastText.take(160))
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (needsReply) {
                    BrandButton(
                        "Reply with AI",
                        {
                            scope.launch {
                                draft = "AI is writing..."
                                target = c
                                draft = try { withContext(Dispatchers.IO) { Ai.reply(s, c.transcript) } } catch (e: Exception) { e.message ?: "Error" }
                            }
                        },
                        Modifier.fillMaxWidth(),
                        enabled = s.aiReady,
                        icon = Icons.Rounded.AutoAwesome
                    )
                } else {
                    StatusChip("Replied", BrandGreen)
                }
            }
        }
    }
}
