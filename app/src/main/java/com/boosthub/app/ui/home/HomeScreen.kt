package com.boosthub.app.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boosthub.app.data.MetaApi
import com.boosthub.app.data.Store
import com.boosthub.app.ui.components.*
import com.boosthub.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private data class Tool(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val tab: Int
)

private val tools = listOf(
    Tool("Ads Manager", "Campaigns, budgets and reports", Icons.Rounded.Campaign, BrandBlue, 1),
    Tool("Posts & Boost", "Create, schedule and boost posts", Icons.Rounded.RocketLaunch, BrandPink, 6),
    Tool("Page Inbox", "Messages and AI replies", Icons.Rounded.Forum, BrandPurple, 2),
    Tool("AI Autopilot", "Auto replies, comments and ads", Icons.Rounded.AutoAwesome, BrandOrange, 3),
    Tool("Billing", "Balance, limits and payment methods", Icons.Rounded.Payments, BrandGreen, 5),
    Tool("Settings", "Server connection and safety limits", Icons.Rounded.Settings, Color(0xFF455A64), 4)
)

@Composable
fun HomeScreen(modifier: Modifier = Modifier, onOpenTab: (Int) -> Unit = {}) {
    val ctx = LocalContext.current
    val store = remember { Store(ctx) }
    var summary by remember { mutableStateOf<MetaApi.Summary?>(null) }
    LaunchedEffect(Unit) {
        if (store.fbReady && store.adAccount.isNotBlank()) {
            summary = try { withContext(Dispatchers.IO) { MetaApi.summary(store) } } catch (e: Exception) { null }
        }
    }

    Column(modifier.fillMaxSize().background(BgLight)) {
        Header()
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { StatsRow(summary) }
            item { SpendChart(summary?.daily ?: listOf("1" to 0.0)) }
            item { SectionTitle("Quick actions") }
            items(tools.chunked(2)) { pair ->
                Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEach { t ->
                        ToolTile(t, Modifier.weight(1f).fillMaxHeight()) { onOpenTab(t.tab) }
                    }
                }
            }
            if (summary == null) {
                item { Muted("Connect your server in Settings to see live data here.") }
            }
        }
    }
}

@Composable
private fun Header() {
    Column(Modifier.fillMaxWidth().background(SurfaceWhite).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BoostLogo(size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("BoostHub", color = TextBlack, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Muted("Your all-in-one boosting center")
            }
        }
        HorizontalDivider(color = BorderGray)
    }
}

private fun Long.toIntSafe(): Int = this.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

@Composable
private fun StatsRow(sm: MetaApi.Summary?) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard("Spend (7d)", (sm?.spend ?: 0.0).toInt(), Icons.Rounded.Payments, BrandGreen, Modifier.weight(1f))
        StatCard("Impressions", (sm?.impressions ?: 0L).toIntSafe(), Icons.Rounded.Visibility, BrandBlue, Modifier.weight(1f))
        StatCard("Clicks", (sm?.clicks ?: 0L).toIntSafe(), Icons.Rounded.TouchApp, BrandOrange, Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, target: Int, icon: ImageVector, tint: Color, modifier: Modifier) {
    val value by animateIntAsState(
        targetValue = target,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "count"
    )
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceWhite)
            .border(1.dp, BorderGray, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        IconTile(icon, tint, 32.dp)
        Spacer(Modifier.height(8.dp))
        Text("$value", color = TextBlack, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, color = TextSoft, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun SpendChart(daily: List<Pair<String, Double>>) {
    val max = (daily.maxOfOrNull { it.second } ?: 0.0).coerceAtLeast(1.0)
    var go by remember { mutableStateOf(false) }
    LaunchedEffect(daily) { go = false; delay(50); go = true }
    val grow by animateFloatAsState(if (go) 1f else 0f, tween(800), label = "grow")
    Panel {
        Text("Spend in the last 7 days", color = TextBlack, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Canvas(Modifier.fillMaxWidth().height(90.dp)) {
            val n = daily.size.coerceAtLeast(1)
            val gap = 8.dp.toPx()
            val bw = (size.width - gap * (n - 1)) / n
            daily.forEachIndexed { i, (_, v) ->
                val x = i * (bw + gap)
                drawRoundRect(
                    color = Color(0xFFEEF1F5),
                    topLeft = Offset(x, 0f),
                    size = Size(bw, size.height),
                    cornerRadius = CornerRadius(6.dp.toPx())
                )
                val h = (v / max).toFloat() * size.height * grow
                drawRoundRect(
                    color = BrandBlue,
                    topLeft = Offset(x, size.height - h),
                    size = Size(bw, h),
                    cornerRadius = CornerRadius(6.dp.toPx())
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            daily.forEach { Text(it.first.takeLast(2), color = TextSoft, fontSize = 10.sp) }
        }
    }
}

@Composable
private fun ToolTile(t: Tool, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceWhite)
            .border(1.dp, BorderGray, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        IconTile(t.icon, t.color, 48.dp)
        Spacer(Modifier.height(12.dp))
        Body(t.title, bold = true)
        Spacer(Modifier.height(2.dp))
        Muted(t.subtitle)
    }
}
