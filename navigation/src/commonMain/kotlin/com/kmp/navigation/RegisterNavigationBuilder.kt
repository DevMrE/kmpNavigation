package com.kmp.navigation

import androidx.compose.runtime.Composable
import kotlin.reflect.KClass

/**
 * Top-level DSL builder used by [registerNavigation].
 *
 * Supports:
 * - Nested sections (multi-backstack)
 * - Screen roles (e.g. Detail for two-pane)
 */
@NavigationDsl
class RegisterNavigationBuilder @PublishedApi internal constructor(
    @PublishedApi internal val registerScreen: (
        KClass<out NavDestination>,
        @Composable (NavDestination) -> Unit
    ) -> Unit,
    @PublishedApi internal val registerDestinationSection: (KClass<out NavDestination>, NavSection) -> Unit,
    @PublishedApi internal val registerDestinationRole: (KClass<out NavDestination>, ScreenRole) -> Unit,
    @PublishedApi internal val registerSectionRoot: (NavSection, NavDestination) -> Unit,
    @PublishedApi internal val registerSectionParent: (NavSection, NavSection?) -> Unit,
) {

    /**
     * Declare a root section (parent = null).
     */
    inline fun <reified S : NavSection> section(
        section: S,
        root: NavDestination,
        noinline builder: SectionBuilder<S>.() -> Unit
    ) {
        registerSectionParent(section, null)
        registerSectionRoot(section, root)

        val sectionBuilder = SectionBuilder(
            section = section,
            registerScreen = registerScreen,
            registerDestinationSection = registerDestinationSection,
            registerDestinationRole = registerDestinationRole,
            registerSectionRoot = registerSectionRoot,
            registerSectionParent = registerSectionParent,
        )
        sectionBuilder.builder()
    }
}

@NavigationDsl
class SectionBuilder<S : NavSection> @PublishedApi internal constructor(
    @PublishedApi internal val section: S,
    @PublishedApi internal val registerScreen: (
        KClass<out NavDestination>,
        @Composable (NavDestination) -> Unit
    ) -> Unit,
    @PublishedApi internal val registerDestinationSection: (KClass<out NavDestination>, NavSection) -> Unit,
    @PublishedApi internal val registerDestinationRole: (KClass<out NavDestination>, ScreenRole) -> Unit,
    @PublishedApi internal val registerSectionRoot: (NavSection, NavDestination) -> Unit,
    @PublishedApi internal val registerSectionParent: (NavSection, NavSection?) -> Unit,
) {

    /**
     * Register a screen within this section.
     */
    inline fun <reified D : NavDestination> screen(
        role: ScreenRole = ScreenRole.Normal,
        noinline content: @Composable (D) -> Unit
    ) {
        val destKey = D::class
        registerDestinationSection(destKey, section)
        registerDestinationRole(destKey, role)
        registerScreen(destKey) { dest ->
            @Suppress("UNCHECKED_CAST")
            content(dest as D)
        }
    }

    /**
     * Register a nested section (child of this.section).
     */
    inline fun <reified C : NavSection> section(
        section: C,
        root: NavDestination,
        noinline builder: SectionBuilder<C>.() -> Unit
    ) {
        registerSectionParent(section, this.section)
        registerSectionRoot(section, root)

        val nestedBuilder = SectionBuilder(
            section = section,
            registerScreen = registerScreen,
            registerDestinationSection = registerDestinationSection,
            registerDestinationRole = registerDestinationRole,
            registerSectionRoot = registerSectionRoot,
            registerSectionParent = registerSectionParent,
        )
        nestedBuilder.builder()
    }
}
