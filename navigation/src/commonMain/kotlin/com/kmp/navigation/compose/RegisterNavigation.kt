package com.kmp.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.kmp.navigation.DestinationRegistry
import com.kmp.navigation.LocalDestinationRegistry
import com.kmp.navigation.LocalNavigation
import com.kmp.navigation.NavDestination
import com.kmp.navigation.Navigation
import com.kmp.navigation.provideNavigationInstance
import com.kmp.navigation.rememberNavigation
import com.kmp.navigation.rememberNavDestination
import kotlin.reflect.KClass

/**
 * Entry point for your navigation tree.
 *
 * It wires together:
 *
 * * A [Navigation] instance (shared with your ViewModels via DI if available).
 * * The screen registry built from the [builder] DSL.
 * * CompositionLocals for [rememberNavigation] and [rememberNavDestination].
 *
 * In most apps you will wrap your root `Scaffold` inside [RegisterNavigation]
 * and place a [NavigationHost] in the content lambda:
 *
 * ```kotlin
 * @Composable
 * fun AppScreen() {
 *     RegisterNavigation(
 *         startDestination = HomeScreenDestination,
 *         builder = {
 *             section<HomeSection, HomeScreenDestination> {
 *                 screen<HomeScreenDestination> { HomeScreen() }
 *                 screen<SettingsScreenDestination> { SettingsScreen() }
 *             }
 *             section<AuthSection, LoginDestination> {
 *                 screen<LoginDestination> { LoginScreen() }
 *                 screen<RegisterDestination> { RegisterScreen() }
 *             }
 *         }
 *     ) {
 *         Scaffold(
 *             topBar = { TopAppBarComponent() },
 *             bottomBar = { BottomBarComponent() }
 *         ) { padding ->
 *             NavigationHost(modifier = Modifier.padding(padding))
 *         }
 *     }
 * }
 * ```
 *
 * If you do not need a Scaffold at the top level you can alternatively use
 * the overload that calls [NavigationHost] for you.
 */
@Composable
fun RegisterNavigation(
    startDestination: NavDestination,
    builder: RegisterNavigationBuilder.() -> Unit,
    content: @Composable () -> Unit
) {
    val navigation: Navigation = provideNavigationInstance()
    val controller = navigation as? NavigationController
        ?: error("RegisterNavigation works only with NavigationFactory.create() implementation.")

    // Build the destination -> Composable map once per navigation tree
    val registry = remember {
        val screens =
            mutableMapOf<KClass<out NavDestination>, @Composable (NavDestination) -> Unit>()

        val navBuilder = RegisterNavigationBuilder(
            registerScreen = { key, screen ->
                if (screens.put(key, screen) != null) {
                    error("Destination $key is already registered.")
                }
            }
        )
        navBuilder.builder()

        DestinationRegistry(screens.toMap())
    }

    // Set the initial destination only once when the controller is still empty
    LaunchedEffect(controller) {
        if (controller.state.value.currentDestination == null) {
            navigation.navigateTo(startDestination) {
                clearStack()
            }
        }
    }

    CompositionLocalProvider(
        LocalNavigation provides navigation,
        LocalDestinationRegistry provides registry
    ) {
        content()
    }
}

/**
 * Convenience overload that automatically hosts the current destination.
 *
 * This is useful when you do not need a top-level Scaffold:
 *
 * ```kotlin
 * @Composable
 * fun AppScreen() {
 *     RegisterNavigation(
 *         startDestination = HomeScreenDestination,
 *         builder = {
 *             section<HomeSection, HomeScreenDestination> {
 *                 screen<HomeScreenDestination> { HomeScreen() }
 *             }
 *         }
 *     )
 * }
 * ```
 */
@Composable
fun RegisterNavigation(
    startDestination: NavDestination,
    builder: RegisterNavigationBuilder.() -> Unit
) {
    RegisterNavigation(
        startDestination = startDestination,
        builder = builder
    ) {
        NavigationHost()
    }
}
