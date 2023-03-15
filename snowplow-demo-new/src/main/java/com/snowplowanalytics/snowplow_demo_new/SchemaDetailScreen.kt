package com.snowplowanalytics.snowplow_demo_new

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SchemaDetailScreen(
    vm: SchemaDetailViewModel, 
    schemaUrl: String
) {
    LaunchedEffect(Unit, block = {
        vm.getSchemaDetails(schemaUrl)
    })
    
    val schemaParts = vm.processSchemaUrl(schemaUrl)

    Column(modifier = Modifier.padding(all = 8.dp)) {
        Text("This is the second screen.")
        Text("Schema is: $schemaUrl")


        Spacer(modifier = Modifier.width(20.dp))
        
        Column {
            Text("URL")
            Text(
                schemaUrl
            )
            Text("Name")
            Text(
                schemaParts.name
            )
            Text("Vendor")
            Text(
                schemaParts.vendor
            )
            Text("Version")
            Text(
                schemaParts.version
            )
            Text("Description")
            Text(vm.description.value ?: "No description found.")
            
        }
    }
}
