package com.example.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.AtahyaatTheme

sealed class NavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : NavItem("home", "Today", Icons.Default.Home)
    object Schedule : NavItem("schedule", "Schedule", Icons.Default.CalendarMonth)
    object Streak : NavItem("streak", "Streak", Icons.Default.LocalFireDepartment)
    object Tasbih : NavItem("tasbih", "Tasbih", Icons.Default.SelfImprovement)
    object Settings : NavItem("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun AtahyaatApp(
    viewModel: MainViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    var currentTab by remember { mutableStateOf<NavItem>(NavItem.Home) }

    val navItems = listOf(
        NavItem.Home,
        NavItem.Schedule,
        NavItem.Streak,
        NavItem.Tasbih,
        NavItem.Settings
    )

    AtahyaatTheme(themeMode = settings.themeMode) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    navItems.forEach { item ->
                        val isSelected = currentTab == item
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab = item },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title
                                )
                            },
                            label = { Text(item.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_${item.route}")
                        )
                    }
                }
            }
        ) { innerPadding ->
            val contentModifier = Modifier.padding(innerPadding)

            when (currentTab) {
                is NavItem.Home -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToStreak = { currentTab = NavItem.Streak },
                    modifier = contentModifier
                )
                is NavItem.Schedule -> ScheduleScreen(
                    viewModel = viewModel,
                    modifier = contentModifier
                )
                is NavItem.Streak -> StreakScreen(
                    viewModel = viewModel,
                    modifier = contentModifier
                )
                is NavItem.Tasbih -> SpiritualScreen(
                    viewModel = viewModel,
                    modifier = contentModifier
                )
                is NavItem.Settings -> SettingsScreen(
                    viewModel = viewModel,
                    modifier = contentModifier
                )
            }
        }
    }
}
