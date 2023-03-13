package com.snowplowanalytics.snowplow_demo_new



import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.snowplowanalytics.snowplow.event.Structured
import java.util.*

object Destinations {
    const val MAIN_ROUTE = "main"
    const val SCHEMA_DETAIL_ROUTE = "detail/{schema}"
    const val JUST_DETAIL_ROUTE = "detail"
}

@Composable
fun ComposeDemoApp() {
    val navController = rememberNavController()
//    val tracker = Tracking.setup()
    
    NavHost(
        navController = navController, 
        startDestination = Destinations.MAIN_ROUTE
    ) {
        composable(Destinations.MAIN_ROUTE) {
            MainScreen(
                onNextButtonClicked = { navController.navigate("detail") },
//                onTrackButtonClicked = { tracker.track(Structured("button", "press")) },
                onSchemaClicked = { 
                    val encoded = Base64.getEncoder().encodeToString(it.toByteArray())
                    navController.navigate("detail/$encoded")
                }
            )
        }

        composable(Destinations.SCHEMA_DETAIL_ROUTE) { 
            val schemaUrl = it.arguments?.getString("schema")
            SchemaDetail(String(Base64.getDecoder().decode(schemaUrl)))
        }

        composable(Destinations.JUST_DETAIL_ROUTE) {
            SchemaDetail("hello details")
        }
    }
}
