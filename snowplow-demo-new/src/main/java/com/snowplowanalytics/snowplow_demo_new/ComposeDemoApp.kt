package com.snowplowanalytics.snowplow_demo_new



import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import com.snowplowanalytics.snowplow.event.Structured

@Composable
fun ComposeDemoApp() {
    val navController = rememberNavController()
    val tracker = Tracking.setup()
    
    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(
                onNextButtonClicked = { navController.navigate("second") },
                onTrackButtonClicked = { tracker.track(Structured("button", "press")) }
            )
        }

        composable("second") {
            SecondScreen()
        }
    }
    
    
}
