package com.boosthub.app.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boosthub.app.data.MetaApi
import com.boosthub.app.data.Store
import com.boosthub.app.ui.components.BoostLogo
import com.boosthub.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private data class Module(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val c1: Color,
    val c2: Color,
    val tab: Int
)

private val modules = listOf(
    Module("অ্যাডস ম্যানেজার", "ক্যাম্পেইন, বাজেট, রিপোর্ট", Icons.Filled.Campaign, BrandBlue, Color(0xFF00C6FF), 1),
    Module("পোস্ট ও বুস্ট", "AI দিয়ে পোস্ট, শিডিউল, বুস্ট", Icons.Filled.RocketLaunch, Color(0xFFEE0979), Color(0xFFFF6A00), 6),
    Module("পেজ ইনবক্স", "মেসেজ ও AI উত্তর", Icons.Filled.Storefront, BrandPurple, BrandPink, 2),
    Module("AI অটোপাইলট", "নিজে মেসেজ, কমেন্ট ও অ্যাডস ম্যানেজ", Icons.Filled.AutoAwesome, Color(0xFFFF8008), BrandGold, 3),
    Module("বিলিং ও পেমেন্ট", "ব্যালেন্স, সীমা, পেমেন্ট মেথড", Icons.Filled.Payments, Color(0xFFF7971E), Color(0xFFFFD200), 5),
    Module("সেটিংস ও কানেকশন", "ফেসবুক ও AI কী, নিরাপত্তা সীমা", Icons.Filled.Settings, Color(0xFF11998E), BrandGreen, 4)
)

@Composable
fun HomeScreen(modifier: Modifier = Modifier, onOpenTab: (Int) -> Unit = {}) {
    val ctx = LocalContext.current
    val store = remember { Store(ctx) }
    var summary by remember { mutableStateOf<MetaApi.Summary?>(null) }
    LaunchedEffect(Unit) {
        if (store.fbToken.isNotBlank() && store.adAccount.isNotBlank()) {
            summary = try { withContext(Dispatchers.IO) { MetaApi.summary(store) } } catch (e: Exception) { null }
        }
    }

    val inf = rememberInfiniteTransition(label = "bg")
    val shift by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse),
        label = "shift"
    )

    Box(modifier.fillMaxSize().background(BgDark)) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.width * 0.85f
            val c1 = Offset(size.width * (0.1f + 0.6f * shift), size.height * 0.12f)
            val c2 = Offset(size.width * (0.9f - 0.6f * shift), size.height * 0.6f)
            drawCircle(
                brush = Brush.radialGradient(listOf(BrandBlue.copy(alpha = 0.35f), Color.Transparent), center = c1, radius = r),
                radius = r,
                center = c1
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(BrandPurple.copy(alpha = 0.30f), Color.Transparent), center = c2, radius = r),
                radius = r,
                center = c2
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Header() }
            item { StatsRow(summary) }
            item { SpendChart(summary?.daily ?: listOf("1" to 0.0)) }
            item {
                Text("আপনার টুলস", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            itemsIndexed(modules) { index, m ->
                ModuleCard(m, index) { onOpenTab(m.tab) }
            }
            if (summary == null) {
                item { Text("* ফেসবুক টোকেন ও অ্যাড অ্যাকাউন্ট দিলে এখানে আসল ডেটা দেখাবে", color = TextSoft, fontSize = 11.sp) }
            }
        }
    }
}

@Composable
private fun Header() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BoostLogo(size = 56.dp)
        Spacer(Modifier.width(14.dp))
        Column {
            Text("BoostHub", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Text("আপনার অল-ইন-ওয়ান বুস্টিং সেন্টার", color = TextSoft, fontSize = 13.sp)
        }
    }
}

private fun Long.toIntSafe(): Int = this.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

@Composable
private fun StatsRow(sm: MetaApi.Summary?) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard("খরচ (৭ দিন)", (sm?.spend ?: 0.0).toInt(), Modifier.weight(1f))
        StatCard("ইমপ্রেশন", (sm?.impressions ?: 0L).toIntSafe(), Modifier.weight(1f))
        StatCard("ক্লিক", (sm?.clicks ?: 0L).toIntSafe(), Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, target: Int, modifier: Modifier) {
    val value by animateIntAsState(
        targetValue = target,
        animationSpec = tween(1400, easing = FastOutSlowInEasing),
        label = "count"
    )
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceDark.copy(alpha = 0.9f))
            .padding(14.dp)
    ) {
        Text(label, color = TextSoft, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Text("$value", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SpendChart(daily: List<Pair<String, Double>>) {
    val max = (daily.maxOfOrNull { it.second } ?: 0.0).coerceAtLeast(1.0)
    var go by remember { mutableStateOf(false) }
    LaunchedEffect(daily) { go = false; delay(50); go = true }
    val grow by animateFloatAsState(if (go) 1f else 0f, tween(900), label = "grow")
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceDark.copy(alpha = 0.9f))
            .padding(14.dp)
    ) {
        Text("শেষ ৭ দিনের খরচ", color = TextSoft, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Canvas(Modifier.fillMaxWidth().height(90.dp)) {
            val n = daily.size.coerceAtLeast(1)
            val gap = 8.dp.toPx()
            val bw = (size.width - gap * (n - 1)) / n
            daily.forEachIndexed { i, (_, v) ->
                val h = (v / max).toFloat() * size.height * grow
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(BrandBlue, BrandPurple), startY = size.height - h, endY = size.height),
                    topLeft = Offset(i * (bw + gap), size.height - h),
                    size = Size(bw, h),
                    cornerRadius = CornerRadius(6.dp.toPx())
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            daily.forEach { Text(it.first.takeLast(2), color = TextSoft, fontSize = 10.sp) }
        }
    }
}

@Composable
private fun ModuleCard(m: Module, index: Int, onClick: () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(150L + index * 130L)
        shown = true
    }
    val appear by animateFloatAsState(if (shown) 1f else 0f, tween(500), label = "appear")

    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val press by animateFloatAsState(
        if (pressed) 0.96f else 1f,
        spring(dampingRatio = 0.5f),
        label = "press"
    )

    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = (1f - appear) * 60f }
            .alpha(appear)
            .scale(press)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(m.c1, m.c2)))
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(m.icon, contentDescription = null, tint = Color.White)
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(m.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(m.subtitle, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
        }
    }
}
