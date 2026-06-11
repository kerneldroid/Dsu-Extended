package com.dsu.extended.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FlexibleBottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dsu.extended.R
import com.dsu.extended.ui.screen.about.AboutScreen
import com.dsu.extended.ui.screen.adb.AdbScreen
import com.dsu.extended.ui.screen.home.Home
import com.dsu.extended.ui.screen.libraries.LibrariesScreen
import com.dsu.extended.ui.screen.logs.LogsScreen
import com.dsu.extended.ui.screen.settings.Settings
import com.dsu.extended.ui.theme.DSUAnimations

object Destinations {
    const val Homepage = "home"
    const val Logs = "logs"
    const val Preferences = "preferences"
    const val ADBInstallation = "adb_installation"
    const val About = "about"
    const val Libraries = "libraries"
    const val Up = "up"
}

private fun isMainTabRoute(route: String?): Boolean {
    return route == Destinations.Homepage || route == Destinations.Logs
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.mainTabEnterTransition(): EnterTransition {
    if (!isMainTabRoute(initialState.destination.route) || !isMainTabRoute(targetState.destination.route)) {
        return EnterTransition.None
    }
    return DSUAnimations.screenEnterAnimation
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.mainTabExitTransition(): ExitTransition {
    if (!isMainTabRoute(initialState.destination.route) || !isMainTabRoute(targetState.destination.route)) {
        return ExitTransition.None
    }
    return DSUAnimations.screenExitAnimation
}

@Composable
fun Navigation() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showMainTabs = currentRoute == Destinations.Homepage || currentRoute == Destinations.Logs

    val navigate: (String) -> Unit = androidx.compose.runtime.remember(navController) {
        { destination ->
            if (destination == Destinations.Up) {
                navController.navigateUp()
            } else if (isMainTabRoute(destination)) {
                navController.navigate(destination) {
                    launchSingleTop = true
                    restoreState = true
                    popUpTo(Destinations.Homepage) {
                        saveState = true
                    }
                }
            } else {
                navController.navigate(destination)
            }
        }
    }

    // Global back handler - disable on main tabs to avoid system gesture intercept issues
    // For sub-screens, this prevents predictive scaling while allowing custom pop transitions
    if (!isMainTabRoute(currentRoute)) {
        BackHandler {
            navController.navigateUp()
        }
    }

    Scaffold(
        bottomBar = {
            androidx.compose.animation.AnimatedVisibility(
                visible = showMainTabs,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(expandFrom = androidx.compose.ui.Alignment.Bottom),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically(shrinkTowards = androidx.compose.ui.Alignment.Bottom),
            ) {
                MainBottomTabs(
                    currentRoute = currentRoute.orEmpty(),
                    onSelectRoute = { destination ->
                        if (destination == currentRoute) {
                            return@MainBottomTabs
                        }
                        navController.navigate(destination) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(Destinations.Homepage) {
                                saveState = true
                            }
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destinations.Homepage,
            modifier = Modifier.fillMaxSize(),
        ) {
                composable(
                    route = Destinations.Homepage,
                    enterTransition = { mainTabEnterTransition() },
                    exitTransition = { mainTabExitTransition() },
                    popEnterTransition = { DSUAnimations.screenPopEnterAnimation },
                    popExitTransition = { DSUAnimations.screenPopExitAnimation },
                ) {
                    Box(modifier = Modifier.padding(bottom = if (showMainTabs) innerPadding.calculateBottomPadding() else 0.dp)) {
                        Home(navigate = { navigate(it) })
                    }
                }
                composable(
                    route = Destinations.Logs,
                    enterTransition = { mainTabEnterTransition() },
                    exitTransition = { mainTabExitTransition() },
                    popEnterTransition = { DSUAnimations.screenPopEnterAnimation },
                    popExitTransition = { DSUAnimations.screenPopExitAnimation },
                ) {
                    Box(modifier = Modifier.padding(bottom = if (showMainTabs) innerPadding.calculateBottomPadding() else 0.dp)) {
                        LogsScreen(navigate = { navigate(it) })
                    }
                }
                composable(
                    route = Destinations.Preferences,
                    enterTransition = { DSUAnimations.screenEnterAnimation },
                    exitTransition = { DSUAnimations.screenExitAnimation },
                    popEnterTransition = { DSUAnimations.screenPopEnterAnimation },
                    popExitTransition = { DSUAnimations.screenPopExitAnimation },
                ) { Settings(navigate = { navigate(it) }) }
                composable(
                    route = Destinations.ADBInstallation,
                    enterTransition = { DSUAnimations.screenEnterAnimation },
                    exitTransition = { DSUAnimations.screenExitAnimation },
                    popEnterTransition = { DSUAnimations.screenPopEnterAnimation },
                    popExitTransition = { DSUAnimations.screenPopExitAnimation },
                ) { AdbScreen(navigate = { navigate(it) }) }
                composable(
                    route = Destinations.About,
                    enterTransition = { DSUAnimations.screenEnterAnimation },
                    exitTransition = { DSUAnimations.screenExitAnimation },
                    popEnterTransition = { DSUAnimations.screenPopEnterAnimation },
                    popExitTransition = { DSUAnimations.screenPopExitAnimation },
                ) { AboutScreen(navigate = { navigate(it) }) }
                composable(
                    route = Destinations.Libraries,
                    enterTransition = { DSUAnimations.screenEnterAnimation },
                    exitTransition = { DSUAnimations.screenExitAnimation },
                    popEnterTransition = { DSUAnimations.screenPopEnterAnimation },
                    popExitTransition = { DSUAnimations.screenPopExitAnimation },
                ) { LibrariesScreen(navigate = { navigate(it) }) }
            }
        }
}

private data class MainTabItem(
    val route: String,
    val titleRes: Int,
    val icon: ImageVector,
)

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
private fun MainBottomTabs(
    currentRoute: String,
    onSelectRoute: (String) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val colorScheme = MaterialTheme.colorScheme
    val isOledBlackTheme = colorScheme.background == Color.Black && colorScheme.surface == Color.Black
    val tabs = listOf(
        MainTabItem(
            route = Destinations.Homepage,
            titleRes = R.string.installation,
            icon = Icons.Rounded.InstallMobile,
        ),
        MainTabItem(
            route = Destinations.Logs,
            titleRes = R.string.logs_tab_title,
            icon = Icons.Rounded.Description,
        ),
    )
    val selectedIndex = tabs.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    FlexibleBottomAppBar(
        containerColor = if (isOledBlackTheme) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
        expandedHeight = 74.dp,
        horizontalArrangement = BottomAppBarDefaults.FlexibleFixedHorizontalArrangement,
    ) {
        tabs.forEachIndexed { index, tab ->
            NavigationBarItem(
                selected = selectedIndex == index,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onSelectRoute(tab.route)
                },
                icon = { Icon(imageVector = tab.icon, contentDescription = null) },
                label = { Text(text = stringResource(id = tab.titleRes)) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
