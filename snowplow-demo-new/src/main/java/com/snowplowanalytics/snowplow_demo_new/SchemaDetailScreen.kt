package com.snowplowanalytics.snowplow_demo_new

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
        vm.getSchemaDescription(schemaUrl)
        vm.getSchemaJson(schemaUrl)
    })
    
    val schemaParts = vm.processSchemaUrl(schemaUrl)

    Column(modifier = Modifier.padding(all = 8.dp)) {
        Spacer(modifier = Modifier.width(40.dp))
        
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text("URL")
            Text(schemaUrl)
            Spacer(modifier = Modifier.width(20.dp))
            
            Text("Name")
            Text(schemaParts.name)
            Spacer(modifier = Modifier.width(20.dp))
            
            Text("Vendor")
            Text(schemaParts.vendor)
            Spacer(modifier = Modifier.width(20.dp))
            
            Text("Version")
            Text(schemaParts.version)
            Spacer(modifier = Modifier.width(20.dp))
            
            Text("Description")
            Text(vm.description.value ?: "No description found.")
            Spacer(modifier = Modifier.width(20.dp))

            Text("JSON Schema")
            Text(vm.json.value.toString(4).replace("\\", ""))
            Spacer(modifier = Modifier.width(20.dp))
        }
    }
}
