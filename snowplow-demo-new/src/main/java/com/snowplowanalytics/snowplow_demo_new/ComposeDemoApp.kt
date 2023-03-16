package com.snowplowanalytics.snowplow_demo_new



import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import java.util.*

object Destinations {
//    const val MAIN_ROUTE = "main"
    const val SCHEMA_LIST_ROUTE = "list"
    const val SCHEMA_DETAIL_ROUTE = "detail/{schema}"
}

@Composable
fun ComposeDemoApp(
    listViewModel: SchemaListViewModel,
    detailViewModel: SchemaDetailViewModel
) {
    val navController = rememberNavController()
//    val tracker = Tracking.setup()
    
    NavHost(
        navController = navController, 
        startDestination = Destinations.SCHEMA_LIST_ROUTE
    ) {
//        composable(Destinations.MAIN_ROUTE) {
//            MainScreen(
//                onNextButtonClicked = { navController.navigate("detail/helloworld") },
////                onTrackButtonClicked = { tracker.track(Structured("button", "press")) },
//                onSchemaClicked = { 
//                    val encoded = Base64.getEncoder().encodeToString(it.toByteArray())
//                    navController.navigate("detail/$encoded")
//                }
//            )
//        }
        
        composable(Destinations.SCHEMA_LIST_ROUTE) {
            SchemaListScreen(
                vm = listViewModel, 
                onSchemaClicked = {
                    val encoded = Base64.getEncoder().encodeToString(it.toByteArray())
                    navController.navigate("detail/$encoded")
                }
            )
            
        }

        composable(Destinations.SCHEMA_DETAIL_ROUTE) { 
            val schemaUrl = it.arguments?.getString("schema")

            SchemaDetailScreen(
                vm = detailViewModel,
                schemaUrl = String(Base64.getDecoder().decode(schemaUrl)).drop(5))
        }
    }
}
