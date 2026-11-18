package com.boosthub.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.boosthub.app.data.Store
import com.boosthub.app.ui.components.*
import com.boosthub.app.ui.theme.*

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val s = remember { Store(ctx) }
    var fb by remember { mutableStateOf(s.fbToken) }
    var acct by remember { mutableStateOf(s.adAccount) }
    var page by remember { mutableStateOf(s.pageId) }
    var ai by remember { mutableStateOf(s.aiKey) }
    var model by remember { mutableStateOf(s.aiModel) }
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
                    Icons.Rounded.Link, BrandBlue, "Facebook connection",
                    "System User or long-lived token"
                )
                Muted("Required permissions: ads_management, pages_messaging, pages_manage_metadata, pages_read_engagement, pages_manage_engagement, pages_manage_posts")
                Spacer(Modifier.height(12.dp))
                Field("Facebook access token", fb, { fb = it }, secret = true)
                Spacer(Modifier.height(10.dp))
                Field("Ad account ID (e.g. 1234567890)", acct, { acct = it })
                Spacer(Modifier.height(10.dp))
                Field("Facebook Page ID", page, { page = it })
            }
        }
        item {
            Panel {
                CardHeader(Icons.Rounded.Key, BrandPurple, "AI connection")
                Field("AI API key", ai, { ai = it }, secret = true)
                Spacer(Modifier.height(10.dp))
                Field("Model", model, { model = it })
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
                    s.fbToken = fb; s.adAccount = acct; s.pageId = page; s.pageToken = ""
                    s.aiKey = ai; s.aiModel = model.ifBlank { "claude-sonnet-5-5" }; s.bizInfo = biz
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
