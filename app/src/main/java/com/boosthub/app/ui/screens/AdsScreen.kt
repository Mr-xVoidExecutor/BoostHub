package com.boosthub.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.boosthub.app.agent.Autopilot
import com.boosthub.app.data.Ai
import com.boosthub.app.data.MetaApi
import com.boosthub.app.data.Store
import com.boosthub.app.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AdsScreen(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val s = remember { Store(ctx) }
    val scope = rememberCoroutineScope()
    var camps by remember { mutableStateOf<List<MetaApi.Campaign>>(emptyList()) }
    var ins by remember { mutableStateOf<Map<String, MetaApi.Insight>>(emptyMap()) }
    var loading by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    var analysis by remember { mutableStateOf("") }
    var budgetFor by remember { mutableStateOf<MetaApi.Campaign?>(null) }
    var newBudget by remember { mutableStateOf("") }

    fun load() {
        scope.launch {
            loading = true; msg = ""
            try {
                val r = withContext(Dispatchers.IO) { MetaApi.campaigns(s) to MetaApi.insights(s) }
                camps = r.first; ins = r.second
            } catch (e: Exception) { msg = e.message ?: "এরর" }
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        if (s.fbToken.isNotBlank() && s.adAccount.isNotBlank()) load()
        else msg = "আগে সেটিংসে ফেসবুক টোকেন ও অ্যাড অ্যাকাউন্ট আইডি দিন"
    }

    if (budgetFor != null) {
        AlertDialog(
            onDismissRequest = { budgetFor = null },
            title = { Text("দৈনিক বাজেট বদলান") },
            text = { Field("নতুন বাজেট (মূল মুদ্রায়)", newBudget, { newBudget = it.filter { c -> c.isDigit() } }) },
            confirmButton = {
                TextButton(onClick = {
                    val c = budgetFor!!
                    val v = newBudget.toLongOrNull()
                    budgetFor = null
                    if (v != null && v > 0) scope.launch {
                        try {
                            withContext(Dispatchers.IO) { MetaApi.setBudget(s, c.id, v * 100) }
                            load()
                        } catch (e: Exception) { msg = e.message ?: "এরর" }
                    }
                }) { Text("সেভ") }
            },
            dismissButton = { TextButton(onClick = { budgetFor = null }) { Text("বাতিল") } }
        )
    }

    ScreenColumn(modifier) {
        item { H("অ্যাডস ম্যানেজার") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BrandButton(if (loading) "লোড হচ্ছে..." else "রিফ্রেশ", { load() }, enabled = !loading)
                BrandButton("AI বিশ্লেষণ", {
                    scope.launch {
                        analysis = "AI ভাবছে..."
                        analysis = try {
                            withContext(Dispatchers.IO) { Ai.analyze(s, Autopilot.dataJson(camps, ins)) }
                        } catch (e: Exception) { e.message ?: "এরর" }
                    }
                }, enabled = camps.isNotEmpty() && s.aiKey.isNotBlank())
            }
        }
        if (msg.isNotBlank()) item { Panel { Soft(msg) } }
        if (analysis.isNotBlank()) item { Panel { White(analysis) } }
        items(camps.size) { idx ->
            val c = camps[idx]
            val i = ins[c.id]
            val active = c.status == "ACTIVE"
            Panel {
                White(c.name, bold = true)
                Soft("স্ট্যাটাস: ${c.status} • দৈনিক বাজেট: ${c.dailyBudget / 100}")
                Soft("৭ দিন: খরচ ${i?.spend ?: 0.0} • ক্লিক ${i?.clicks ?: 0} • CTR ${i?.ctr ?: 0.0} • রেজাল্ট ${i?.results ?: 0.0}")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BrandButton(if (active) "বন্ধ করুন" else "চালু করুন", {
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) { MetaApi.setStatus(s, c.id, if (active) "PAUSED" else "ACTIVE") }
                                load()
                            } catch (e: Exception) { msg = e.message ?: "এরর" }
                        }
                    })
                    if (c.dailyBudget > 0) BrandButton("বাজেট", { newBudget = (c.dailyBudget / 100).toString(); budgetFor = c })
                }
            }
        }
    }
}
