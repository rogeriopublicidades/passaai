package app.esperai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { EsperaiTheme { App() } }
    }
}

private fun NavGraphBuilder.withId(route: String, content: @Composable (Long) -> Unit) {
    composable(route, arguments = listOf(navArgument("id") { type = NavType.LongType })) {
        content(it.arguments!!.getLong("id"))
    }
}

@Composable
fun App() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "lines") {
        composable("lines") { LinesScreen(nav) }
        withId("line/{id}") { LineScreen(nav, it) }
        withId("stop/{id}") { StopScreen(nav, it) }
        if (BuildConfig.ADMIN) {
            composable("admin") { AdminHome(nav) }
            composable("admin/lines") { AdminLines(nav) }
            composable("admin/stops") { AdminStops(nav) }
            withId("admin/line/{id}") { LineForm(nav, it) }
            withId("admin/stop/{id}") { StopForm(nav, it) }
            withId("admin/order/{id}") { OrderScreen(nav, it) }
            withId("admin/times/{id}") { TimesScreen(nav, it) }
        }
    }
}
