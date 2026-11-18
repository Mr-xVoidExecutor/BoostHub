package com.boosthub.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.boosthub.app.data.Backend
import com.boosthub.app.data.Store
import com.boosthub.app.ui.components.*
import com.boosthub.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
private fun StatusRow(label: String, ok: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Body(label)
        StatusChip(if (ok) "Connected" else "Not set", if (ok) BrandGreen else BrandOrange)
    }
}

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val s = remember { Store(ctx) }
    val scope = rememberCoroutineScope()

    var url by remember { mutableStateOf(s.serverUrl) }
    var code by remember { mutableStateOf(s.accessCode) }
    var fbOk by remember { mutableStateOf(s.fbReady) }
    var aiOk by remember { mutableStateOf(s.aiReady) }
    var acct by remember { mutableStateOf(s.adAccount) }
    var page by remember { mutableStateOf(s.pageId) }
    var model by remember { mutableStateOf(s.aiModel) }
    var testing by remember { mutableStateOf(false) }
    var connMsg by remember { mutableStateOf("") }
    var connErr by remember { mutableStateOf(false) }

    var biz by remember { mutableStateOf(s.bizInfo) }
    var cap by remember { mutableStateOf(s.dailyCap.toString()) }
    var pct by remember { mutableStateOf(s.maxChangePct.toString()) }
    var acts by remember { mutableStateOf(s.maxActions.toString()) }
    var reps by remember { mutableStateOf(s.maxReplies.toString()) }
    var saved by remember { mutableStateOf(false) }

    ScreenColumn(modifier, title = "Settings") {
        item {
            Panel {
                CardHeader(
                    Icons.Rounded.Link, BrandBlue, "Server connection",
                    "Your Vercel server holds all API keys"
                )
                Field("Server URL (e.g. https://your-app.vercel.app)", url, { url = it })
                Spacer(Modifier.height(10.dp))
                Field("Access code", code, { code = it }, secret = true)
                Spacer(Modifier.height(12.dp))
                BrandButton(
                    if (testing) "Testing..." else "Save & test connection",
                    {
                        scope.launch {
                            testing = true; connMsg = ""; connErr = false
                            try {
                                s.serverUrl = url
                                s.accessCode = code
                                withContext(Dispatchers.IO) { Backend.sync(s) }
                                fbOk = s.fbReady; aiOk = s.aiReady
                                acct = s.adAccount; page = s.pageId; model = s.aiModel
                                connMsg = "Connected to your server."
                            } catch (e: Exception) {
                                connMsg = e.message ?: "Connection failed"
                                connErr = true
                            }
                            testing = false
                        }
                    },
                    Modifier.fillMaxWidth(),
                    enabled = !testing && url.isNotBlank() && code.isNotBlank(),
                    icon = Icons.Rounded.Link
                )
                if (connMsg.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Notice(connMsg, isError = connErr)
                }
                Spacer(Modifier.height(12.dp))
                Separator()
                StatusRow("Facebook access token", fbOk)
                StatusRow("Gemini API key", aiOk)
                if (acct.isNotBlank()) Muted("Ad account ID: $acct")
                if (page.isNotBlank()) Muted("Page ID: $page")
                if (model.isNotBlank()) Muted("AI model: $model")
            }
        }
        item {
            Panel {
                CardHeader(
                    Icons.Rounded.Business, BrandOrange, "Business information",
                    "Products, prices, delivery and what AI must not say"
                )
                Field("Business info and rules", biz, { biz = it }, lines = 6)
            }
        }
        item {
            Panel {
                CardHeader(
                    Icons.Rounded.Shield, BrandGreen, "Safety limits",
                    "AI can never go beyond these limits"
                )
                Field("Total daily budget cap for all campaigns (0 = no budget increases)", cap, { cap = it.filter { c -> c.isDigit() } })
                Spacer(Modifier.height(10.dp))
                Field("Max budget change per step (%)", pct, { pct = it.filter { c -> c.isDigit() } })
                Spacer(Modifier.height(10.dp))
                Field("Max ad actions per run", acts, { acts = it.filter { c -> c.isDigit() } })
                Spacer(Modifier.height(10.dp))
                Field("Max message replies per run", reps, { reps = it.filter { c -> c.isDigit() } })
            }
        }
        item {
            BrandButton(
                "Save settings",
                {
                    s.bizInfo = biz
                    s.dailyCap = cap.toIntOrNull() ?: 0
                    s.maxChangePct = (pct.toIntOrNull() ?: 20).coerceIn(1, 50)
                    s.maxActions = (acts.toIntOrNull() ?: 3).coerceIn(1, 10)
                    s.maxReplies = (reps.toIntOrNull() ?: 5).coerceIn(1, 20)
                    saved = true
                },
                Modifier.fillMaxWidth(),
                icon = Icons.Rounded.Check
            )
            if (saved) {
                Spacer(Modifier.height(8.dp))
                Notice("Settings saved.")
            }
        }
    }
}
