package com.boosthub.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.boosthub.app.agent.Autopilot
import com.boosthub.app.data.Ai
import com.boosthub.app.data.MetaApi
import com.boosthub.app.data.Store
import com.boosthub.app.ui.components.*
import com.boosthub.app.ui.theme.*
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
            } catch (e: Exception) { msg = e.message ?: "Error" }
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        if (s.fbToken.isNotBlank() && s.adAccount.isNotBlank()) load()
        else msg = "Add your Facebook token and Ad Account ID in Settings first."
    }

    val bf = budgetFor
    if (bf != null) {
        AppDialog(
            title = "Edit daily budget",
            confirmText = "Save",
            onConfirm = {
                val v = newBudget.toLongOrNull()
                budgetFor = null
                if (v != null && v > 0) scope.launch {
                    try {
                        withContext(Dispatchers.IO) { MetaApi.setBudget(s, bf.id, v * 100) }
                        load()
                    } catch (e: Exception) { msg = e.message ?: "Error" }
                }
            },
            onDismiss = { budgetFor = null }
        ) {
            Muted(bf.name)
            Field("New daily budget (account currency)", newBudget, { newBudget = it.filter { c -> c.isDigit() } })
        }
    }

    ScreenColumn(modifier, title = "Ads Manager") {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlineButton(
                    if (loading) "Loading..." else "Refresh",
                    { load() },
                    Modifier.weight(1f),
                    enabled = !loading,
                    icon = Icons.Rounded.Refresh
                )
                BrandButton(
                    "AI Analysis",
                    {
                        scope.launch {
                            analysis = "AI is thinking..."
                            analysis = try {
                                withContext(Dispatchers.IO) { Ai.analyze(s, Autopilot.dataJson(camps, ins)) }
                            } catch (e: Exception) { e.message ?: "Error" }
                        }
                    },
                    Modifier.weight(1f),
                    enabled = camps.isNotEmpty() && s.aiKey.isNotBlank(),
                    icon = Icons.Rounded.Insights
                )
            }
        }
        if (msg.isNotBlank()) item { Notice(msg) }
        if (analysis.isNotBlank()) item {
            Panel {
                CardHeader(Icons.Rounded.Insights, BrandPurple, "AI analysis")
                Body(analysis)
            }
        }
        if (camps.isEmpty() && !loading && msg.isBlank()) item {
            Panel { Muted("No campaigns found in this ad account.") }
        }
        items(camps.size) { idx ->
            val c = camps[idx]
            val i = ins[c.id]
            val active = c.status == "ACTIVE"
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Rounded.Campaign, BrandBlue, 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Body(c.name, bold = true)
                        Muted("Daily budget: ${c.dailyBudget / 100}")
                    }
                    StatusChip(
                        if (active) "Active" else c.status.lowercase().replaceFirstChar { it.uppercase() },
                        if (active) BrandGreen else BrandOrange
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBox("Spend", "%.2f".format(i?.spend ?: 0.0), Modifier.weight(1f))
                    StatBox("Clicks", "${i?.clicks ?: 0}", Modifier.weight(1f))
                    StatBox("CTR", "%.1f%%".format(i?.ctr ?: 0.0), Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Muted("Results (7 days): ${"%.0f".format(i?.results ?: 0.0)}")
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlineButton(
                        if (active) "Pause" else "Resume",
                        {
                            scope.launch {
                                try {
                                    withContext(Dispatchers.IO) { MetaApi.setStatus(s, c.id, if (active) "PAUSED" else "ACTIVE") }
                                    load()
                                } catch (e: Exception) { msg = e.message ?: "Error" }
                            }
                        },
                        Modifier.weight(1f),
                        icon = if (active) Icons.Rounded.Pause else Icons.Rounded.PlayArrow
                    )
                    if (c.dailyBudget > 0) {
                        OutlineButton(
                            "Budget",
                            { newBudget = (c.dailyBudget / 100).toString(); budgetFor = c },
                            Modifier.weight(1f),
                            icon = Icons.Rounded.Edit
                        )
                    }
                }
            }
        }
    }
}
