package uk.motobay.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.BoxWithConstraints
import uk.motobay.app.ui.theme.auroraPalette
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import uk.motobay.app.MotoBayApplication
import uk.motobay.app.data.ModerationStatus
import uk.motobay.app.ui.account.MySubmissionsScreen
import uk.motobay.app.ui.account.SignInDialog
import uk.motobay.app.ui.bay.BayDetailRoute
import uk.motobay.app.ui.explore.ExploreScreen
import uk.motobay.app.ui.review.ReviewScreen
import uk.motobay.app.ui.saved.SavedScreen
import uk.motobay.app.ui.settings.DisclaimerDialog
import uk.motobay.app.ui.settings.LegalScreen
import uk.motobay.app.ui.settings.Policy
import uk.motobay.app.ui.settings.PolicyScreen
import uk.motobay.app.ui.settings.SettingsScreen
import kotlin.reflect.KClass

@Serializable object ExploreDest
@Serializable object SavedDest
@Serializable object ReviewDest
@Serializable object SettingsDest
@Serializable object MySubmissionsDest
@Serializable object LegalDest
@Serializable data class PolicyDest(val policy: String)
@Serializable data class BayDest(val id: String)

val LocalApp = staticCompositionLocalOf<MotoBayApplication> { error("No app") }
val LocalSnackbar = staticCompositionLocalOf<SnackbarHostState> { error("No snackbar host") }

/**
 * Runs the action if a rider is signed in; otherwise asks them to sign in first.
 * Photos, comments and bay changes all go through this.
 */
val LocalRequireSignIn = staticCompositionLocalOf<(() -> Unit) -> Unit> { { it() } }

private data class TopLevel(
    val route: Any,
    val routeClass: KClass<*>,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

/**
 * App frame. NavigationSuiteScaffold applies the M3 adaptive navigation rule:
 * a Navigation bar on compact windows and a Navigation rail on medium and
 * expanded windows (tablets, foldables, landscape).
 */
@Composable
fun MotoBayApp(app: MotoBayApplication) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val settings by app.user.settings.collectAsStateWithLifecycle()
    val account by app.auth.user.collectAsStateWithLifecycle()
    var askToSignIn by remember { mutableStateOf(false) }
    val requireSignIn: (() -> Unit) -> Unit = { action -> if (account != null) action() else askToSignIn = true }
    val community by app.community.data.collectAsStateWithLifecycle()
    val pending = community.photos.count { it.status == ModerationStatus.Pending } +
        community.comments.count { it.status == ModerationStatus.Pending } +
        community.bays.count { it.status == ModerationStatus.Pending }

    val destinations = buildList {
        add(TopLevel(ExploreDest, ExploreDest::class, "Explore", Icons.Outlined.Explore, Icons.Filled.Explore))
        add(TopLevel(SavedDest, SavedDest::class, "Saved", Icons.Outlined.StarBorder, Icons.Filled.Star))
        if (account?.isModerator == true) add(TopLevel(ReviewDest, ReviewDest::class, "Review", Icons.Outlined.RateReview, Icons.Filled.RateReview))
        add(TopLevel(SettingsDest, SettingsDest::class, "Settings", Icons.Outlined.Settings, Icons.Filled.Settings))
    }
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination
    // For crash reports: which screen the rider was on.
    LaunchedEffect(current) { current?.route?.let { app.crashLog.noteScreen(it.substringAfterLast('.').substringBefore('/')) } }

    val palette = auroraPalette()
    CompositionLocalProvider(LocalApp provides app, LocalSnackbar provides snackbar, LocalRequireSignIn provides requireSignIn) {
    val onPick: (TopLevel) -> Unit = { d ->
        nav.navigate(d.route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val isSelected: (TopLevel) -> Boolean = { d ->
        current?.hierarchy?.any { it.hasRoute(d.routeClass) } == true ||
            (d.route == ExploreDest && current?.hasRoute(BayDest::class) == true)
    }
    val content: @Composable () -> Unit = {
            Box(Modifier.fillMaxSize()) {
            NavHost(nav, startDestination = ExploreDest) {
                composable<ExploreDest> {
                    ExploreScreen(onOpenBay = { nav.navigate(BayDest(it)) })
                }
                composable<BayDest> { entry ->
                    BayDetailRoute(id = entry.toRoute<BayDest>().id, onBack = { nav.popBackStack() })
                }
                composable<SavedDest> { SavedScreen(onOpenBay = { nav.navigate(BayDest(it)) }) }
                composable<ReviewDest> { ReviewScreen(onOpenBay = { nav.navigate(BayDest(it)) }) }
                composable<SettingsDest> {
                    SettingsScreen(
                        onOpenSubmissions = { nav.navigate(MySubmissionsDest) },
                        onOpenLegal = { nav.navigate(LegalDest) },
                        onOpenPolicy = { nav.navigate(PolicyDest(it.name)) },
                    )
                }
                composable<LegalDest> { LegalScreen(onBack = { nav.popBackStack() }, onOpenPolicy = { nav.navigate(PolicyDest(it.name)) }) }
                composable<PolicyDest> { entry ->
                    PolicyScreen(policy = Policy.valueOf(entry.toRoute<PolicyDest>().policy), onBack = { nav.popBackStack() })
                }
                composable<MySubmissionsDest> {
                    MySubmissionsScreen(onBack = { nav.popBackStack() }, onOpenBay = { nav.navigate(BayDest(it)) })
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp))
            }
    }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            // M3 adaptive rule: a bar on compact windows (phones, or anything short), a rail otherwise.
            if (maxWidth < 600.dp || maxHeight < 480.dp) {
                Scaffold(
                    bottomBar = {
                        AuroraNavigationBar(palette) {
                            destinations.forEach { d ->
                                val selected = isSelected(d)
                                AuroraNavigationItem(selected = selected, label = d.label, palette = palette, onClick = { onPick(d) }) { tint ->
                                    val icon = if (selected) d.selectedIcon else d.icon
                                    if (d.route == ReviewDest && pending > 0) {
                                        BadgedBox(badge = { Badge { Text("$pending") } }) { Icon(icon, contentDescription = null, Modifier.size(20.dp), tint = tint) }
                                    } else {
                                        Icon(icon, contentDescription = null, Modifier.size(20.dp), tint = tint)
                                    }
                                }
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.background,
                    // Full screen: the map and each screen's own app bar run under the transparent status bar.
                    contentWindowInsets = WindowInsets(0),
                ) { inner -> Box(Modifier.padding(inner)) { content() } }
            } else {
                NavigationSuiteScaffold(
                    navigationSuiteItems = {
                        destinations.forEach { d ->
                            val selected = isSelected(d)
                            item(
                                selected = selected,
                                onClick = { onPick(d) },
                                icon = {
                                    val icon = if (selected) d.selectedIcon else d.icon
                                    if (d.route == ReviewDest && pending > 0) {
                                        BadgedBox(badge = { Badge { Text("$pending") } }) { Icon(icon, contentDescription = null) }
                                    } else {
                                        Icon(icon, contentDescription = null)
                                    }
                                },
                                label = { Text(d.label) },
                            )
                        }
                    },
                ) { content() }
            }
        }
        if (askToSignIn) SignInDialog(onDismiss = { askToSignIn = false })
        if (!settings.disclaimerAccepted) DisclaimerDialog(onAccept = { app.user.updateSettings { it.copy(disclaimerAccepted = true) } })
    }
}
