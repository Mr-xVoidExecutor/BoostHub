package com.boosthub.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.boosthub.app.ui.home.HomeScreen
import com.boosthub.app.ui.screens.AdsScreen
import com.boosthub.app.ui.screens.AiScreen
import com.boosthub.app.ui.screens.BillingScreen
import com.boosthub.app.ui.screens.InboxScreen
import com.boosthub.app.ui.screens.PostsScreen
import com.boosthub.app.ui.screens.SettingsScreen
import com.boosthub.app.ui.theme.BgLight
import com.boosthub.app.ui.theme.BorderGray
import com.boosthub.app.ui.theme.BrandBlue
import com.boosthub.app.ui.theme.SurfaceWhite
import com.boosthub.app.ui.theme.TextBlack
import com.boosthub.app.ui.theme.TextSoft

private data class Tab(val title: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("Home", Icons.Rounded.Home),
    Tab("Ads", Icons.Rounded.Campaign),
    Tab("Inbox", Icons.Rounded.Inbox),
    Tab("AI", Icons.Rounded.AutoAwesome),
    Tab("Settings", Icons.Rounded.Settings)
)

@Composable
fun AppRoot() {
    var selected by rememberSaveable { mutableIntStateOf(0) }

    // Posts (6) and Billing (5) are sub pages opened from Home
    BackHandler(enabled = selected != 0) { selected = 0 }

    Scaffold(
        containerColor = BgLight,
        bottomBar = {
            Column {
                HorizontalDivider(color = BorderGray)
                NavigationBar(containerColor = SurfaceWhite, tonalElevation = androidx.compose.ui.unit.Dp.Hairline) {
                    tabs.forEachIndexed { i, t ->
                        NavigationBarItem(
                            selected = selected == i || (i == 0 && selected >= 5),
                            onClick = { selected = i },
                            icon = { Icon(t.icon, contentDescription = t.title) },
                            label = { Text(t.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BrandBlue,
                                selectedTextColor = TextBlack,
                                indicatorColor = Color(0xFFE7F0FE),
                                unselectedIconColor = TextSoft,
                                unselectedTextColor = TextSoft
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        val m = Modifier.padding(padding)
        when (selected) {
            0 -> HomeScreen(m, onOpenTab = { selected = it })
            1 -> AdsScreen(m)
            2 -> InboxScreen(m)
            3 -> AiScreen(m)
            4 -> SettingsScreen(m)
            5 -> BillingScreen(m, onBack = { selected = 0 })
            else -> PostsScreen(m, onBack = { selected = 0 })
        }
    }
}
