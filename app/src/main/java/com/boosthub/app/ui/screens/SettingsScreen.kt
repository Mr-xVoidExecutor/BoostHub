package com.boosthub.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.boosthub.app.data.Store
import com.boosthub.app.ui.components.*

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

    ScreenColumn(modifier) {
        item { H("সেটিংস ও কানেকশন") }
        item {
            Panel {
                White("ফেসবুক", bold = true)
                Soft("System User বা Long-lived টোকেন (ads_management, pages_messaging, pages_manage_metadata, pages_read_engagement, pages_manage_engagement, pages_manage_posts)")
                Spacer(Modifier.height(8.dp))
                Field("ফেসবুক অ্যাক্সেস টোকেন", fb, { fb = it }, secret = true)
                Spacer(Modifier.height(8.dp))
                Field("অ্যাড অ্যাকাউন্ট আইডি (যেমন 1234567890)", acct, { acct = it })
                Spacer(Modifier.height(8.dp))
                Field("ফেসবুক পেজ আইডি", page, { page = it })
            }
        }
        item {
            Panel {
                White("AI (Anthropic)", bold = true)
                Spacer(Modifier.height(8.dp))
                Field("AI API Key", ai, { ai = it }, secret = true)
                Spacer(Modifier.height(8.dp))
                Field("মডেল", model, { model = it })
                Spacer(Modifier.height(8.dp))
                Field("ব্যবসার তথ্য ও নিয়ম (প্রোডাক্ট, দাম, ডেলিভারি, কী বলা যাবে না)", biz, { biz = it }, lines = 6)
            }
        }
        item {
            Panel {
                White("নিরাপত্তা সীমা", bold = true)
                Soft("AI এই সীমার বাইরে যেতে পারবে না")
                Spacer(Modifier.height(8.dp))
                Field("সব ক্যাম্পেইনের মোট দৈনিক বাজেট সীমা (0 = বাজেট বাড়ানো বন্ধ)", cap, { cap = it.filter { c -> c.isDigit() } })
                Spacer(Modifier.height(8.dp))
                Field("একবারে সর্বোচ্চ বাজেট বদল %", pct, { pct = it.filter { c -> c.isDigit() } })
                Spacer(Modifier.height(8.dp))
                Field("প্রতি রানে সর্বোচ্চ অ্যাডস অ্যাকশন", acts, { acts = it.filter { c -> c.isDigit() } })
                Spacer(Modifier.height(8.dp))
                Field("প্রতি রানে সর্বোচ্চ মেসেজ উত্তর", reps, { reps = it.filter { c -> c.isDigit() } })
            }
        }
        item {
            BrandButton("সেভ করুন", {
                s.fbToken = fb; s.adAccount = acct; s.pageId = page; s.pageToken = ""
                s.aiKey = ai; s.aiModel = model.ifBlank { "claude-sonnet-5-5" }; s.bizInfo = biz
                s.dailyCap = cap.toIntOrNull() ?: 0
                s.maxChangePct = (pct.toIntOrNull() ?: 20).coerceIn(1, 50)
                s.maxActions = (acts.toIntOrNull() ?: 3).coerceIn(1, 10)
                s.maxReplies = (reps.toIntOrNull() ?: 5).coerceIn(1, 20)
                saved = true
            })
            if (saved) { Spacer(Modifier.height(8.dp)); Soft("সেভ হয়েছে ✅") }
        }
    }
}
