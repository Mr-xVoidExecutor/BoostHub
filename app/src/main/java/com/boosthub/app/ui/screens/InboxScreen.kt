package com.boosthub.app.ui.screens

import androidx.compose.foundation.layout.*
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
            catch (e: Exception) { msg = e.message ?: "এরর" }
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        if (s.fbToken.isNotBlank() && s.pageId.isNotBlank()) load()
        else msg = "আগে সেটিংসে ফেসবুক টোকেন ও পেজ আইডি দিন"
    }

    if (target != null) {
        AlertDialog(
            onDismissRequest = { target = null },
            title = { Text("${target!!.name} কে উত্তর") },
            text = { Field("উত্তর (এডিট করতে পারেন)", draft, { draft = it }, lines = 4) },
            confirmButton = {
                TextButton(onClick = {
                    val t = target!!
                    target = null
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) { MetaApi.sendMessage(s, t.psid, draft) }
                            msg = "পাঠানো হয়েছে ✅"
                            load()
                        } catch (e: Exception) { msg = e.message ?: "এরর" }
                    }
                }) { Text("পাঠান") }
            },
            dismissButton = { TextButton(onClick = { target = null }) { Text("বাতিল") } }
        )
    }

    ScreenColumn(modifier) {
        item { H("পেজ ইনবক্স") }
        item { BrandButton(if (loading) "লোড হচ্ছে..." else "রিফ্রেশ", { load() }, enabled = !loading) }
        if (msg.isNotBlank()) item { Panel { Soft(msg) } }
        items(convs.size) { idx ->
            val c = convs[idx]
            Panel {
                White(c.name, bold = true)
                Soft(c.lastText.take(160))
                if (!c.lastFromPage && c.psid.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    BrandButton("AI দিয়ে উত্তর", {
                        scope.launch {
                            draft = "AI লিখছে..."
                            target = c
                            draft = try { withContext(Dispatchers.IO) { Ai.reply(s, c.transcript) } } catch (e: Exception) { e.message ?: "এরর" }
                        }
                    }, enabled = s.aiKey.isNotBlank())
                } else Soft("✅ উত্তর দেওয়া আছে")
            }
        }
    }
}
