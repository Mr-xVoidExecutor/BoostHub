package com.boosthub.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Settings
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
import com.boosthub.app.ui.theme.BgDark
import com.boosthub.app.ui.theme.BrandBlue
import com.boosthub.app.ui.theme.SurfaceDark
import com.boosthub.app.ui.theme.TextSoft

private data class Tab(val title: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("হোম", Icons.Filled.Home),
    Tab("অ্যাডস", Icons.Filled.Campaign),
    Tab("ইনবক্স", Icons.Filled.Inbox),
    Tab("AI", Icons.Filled.AutoAwesome),
    Tab("সেটিংস", Icons.Filled.Settings)
)

@Composable
fun AppRoot() {
    var selected by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        containerColor = BgDark,
        bottomBar = {
            NavigationBar(containerColor = SurfaceDark) {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = selected == i,
                        onClick = { selected = i },
                        icon = { Icon(t.icon, contentDescription = t.title) },
                        label = { Text(t.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            indicatorColor = BrandBlue,
                            unselectedIconColor = TextSoft,
                            unselectedTextColor = TextSoft
                        )
                    )
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
            5 -> BillingScreen(m)
            else -> PostsScreen(m)
        }
    }
}
