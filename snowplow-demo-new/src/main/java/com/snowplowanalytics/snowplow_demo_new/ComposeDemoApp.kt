package com.snowplowanalytics.snowplow_demo_new



import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.snowplowanalytics.snowplow.event.Structured

@Composable
fun ComposeDemoApp() {
    val navController = rememberNavController()
    val tracker = Tracking.setup()
    
    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(
                onNextButtonClicked = { navController.navigate("detail") },
                onTrackButtonClicked = { tracker.track(Structured("button", "press")) },
                onSchemaClicked = { schema -> navController.navigate("detail/$schema") }
            )
        }

        composable(
            route = "detail/{schema}", 
            arguments = listOf(navArgument("schema") { type = NavType.StringType })
        ) { backStackEntry ->
            backStackEntry.arguments?.getString("schema")?.let { SchemaDetail(it) }
        }
    }
}
