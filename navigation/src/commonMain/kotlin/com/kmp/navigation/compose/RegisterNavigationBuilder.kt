package com.kmp.navigation.compose

import androidx.compose.runtime.Composable
import com.kmp.navigation.NavDestination
import com.kmp.navigation.NavSection
import com.kmp.navigation.NavigationDsl
import kotlin.reflect.KClass

@DslMarker
annotation class NavigationDsl

@NavigationDsl
class RegisterNavigationBuilder @PublishedApi internal constructor(
    @PublishedApi internal val registerScreen: (KClass<out NavDestination>, @Composable (NavDestination) -> Unit) -> Unit,
    @PublishedApi internal val registerSectionRoot: (KClass<out NavSection>, KClass<out NavDestination>) -> Unit,
    @PublishedApi internal val registerDestinationSection: (KClass<out NavDestination>, KClass<out NavSection>) -> Unit
) {

    inline fun <reified S : NavSection, reified Root : NavDestination> section(
        noinline builder: SectionBuilder<S, Root>.() -> Unit
    ) {
        registerSectionRoot(S::class, Root::class)

        val sectionBuilder = SectionBuilder(
            sectionKey = S::class,
            rootKey = Root::class,
            registerScreen = registerScreen,
            registerDestinationSection = registerDestinationSection
        )
        sectionBuilder.builder()
    }
}

@NavigationDsl
class SectionBuilder<S : NavSection, Root : NavDestination> @PublishedApi internal constructor(
    @PublishedApi internal val sectionKey: KClass<S>,
    @PublishedApi internal val rootKey: KClass<Root>,
    @PublishedApi internal val registerScreen: (KClass<out NavDestination>, @Composable (NavDestination) -> Unit) -> Unit,
    @PublishedApi internal val registerDestinationSection: (KClass<out NavDestination>, KClass<out NavSection>) -> Unit
) {

    inline fun <reified D : NavDestination> screen(
        noinline content: @Composable (D) -> Unit
    ) {
        val destKey = D::class
        registerDestinationSection(destKey, sectionKey)
        registerScreen(destKey) { dest ->
            @Suppress("UNCHECKED_CAST")
            content(dest as D)
        }
    }
}
