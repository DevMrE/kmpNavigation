package com.kmp.navigation

import androidx.compose.runtime.Composable
import kotlin.reflect.KClass

@NavigationDsl
class TypedGraphBuilder @PublishedApi internal constructor(
    @PublishedApi internal val registerScreen: (KClass<out NavDestination>, @Composable (NavDestination) -> Unit) -> Unit,
    @PublishedApi internal val graphPath: List<KClass<out NavSection>> = emptyList()
) {

    fun screen(
        destination: KClass<out NavDestination>,
        content: @Composable (NavDestination) -> Unit
    ) {
        registerScreen(destination, content)
    }

    inline fun <reified G : NavSection> section(
        noinline builder: TypedGraphBuilder.() -> Unit
    ) {
        val nestedPath = graphPath + G::class
        val nestedBuilder = TypedGraphBuilder(
            registerScreen = registerScreen,
            graphPath = nestedPath
        )
        nestedBuilder.builder()
    }
}

/**
 * Für `screen<MyDestination> { ... }`
 */
inline fun <reified D : NavDestination> TypedGraphBuilder.screen(
    noinline content: @Composable (D) -> Unit
) {
    screen(D::class) { dest ->
        @Suppress("UNCHECKED_CAST")
        content(dest as D)
    }
}
