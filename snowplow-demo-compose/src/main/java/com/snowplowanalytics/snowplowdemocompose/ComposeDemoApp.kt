package com.snowplowanalytics.snowplowdemocompose

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import com.snowplowanalytics.snowplowdemocompose.data.Tracking
import com.snowplowanalytics.snowplowdemocompose.ui.SchemaDetailScreen
import com.snowplowanalytics.snowplowdemocompose.ui.SchemaDetailViewModel
import com.snowplowanalytics.snowplowdemocompose.ui.SchemaListScreen
import com.snowplowanalytics.snowplowdemocompose.ui.SchemaListViewModel
import java.util.*

object Destinations {
    const val SCHEMA_LIST_ROUTE = "list"
    const val SCHEMA_DETAIL_ROUTE = "detail/{schema}"
}

@Composable
fun ComposeDemoApp(
    listViewModel: SchemaListViewModel,
    detailViewModel: SchemaDetailViewModel
) {
    val navController = rememberNavController()
    
    // Initialises the Snowplow tracker and sets up screen view autotracking
    // A ScreenView event will be generated every time the navigation destination changes
    Tracking.setup("compose_demo")
    Tracking.AutoTrackScreenView(navController = navController)
    
    NavHost(
        navController = navController, 
        startDestination = Destinations.SCHEMA_LIST_ROUTE
    ) {
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
                schemaUrl = String(Base64.getDecoder().decode(schemaUrl)).drop(5),
                onBackButtonClicked = { navController.navigate(Destinations.SCHEMA_LIST_ROUTE) }
            )
        }
    }
}
