import com.kmp.navigation.Navigation
import com.kmp.navigation.compose.MutableComposeNavigation
import com.kmp.navigation.compose.NavigationImpl

object NavigationFactory {

    internal var mutableInstance: MutableComposeNavigation? = null
        set

    fun create(): Navigation {
        val impl = NavigationImpl()
        mutableInstance = impl
        return impl
    }
}
