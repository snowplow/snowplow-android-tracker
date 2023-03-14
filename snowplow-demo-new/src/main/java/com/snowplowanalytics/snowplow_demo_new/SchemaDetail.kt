package com.snowplowanalytics.snowplow_demo_new

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun SchemaDetail(schema: String) {
//    val composableScope = rememberCoroutineScope()
//    composableScope.launch { IgluAPIService.singleSchemaBody(schema) }

    Column(modifier = Modifier.padding(all = 8.dp)) {
        Text("This is the second screen.")
        Text("Schema is: $schema")
//        val testString = IgluAPIService.schema(schema)

        Spacer(modifier = Modifier.width(20.dp))
        
//        Text(testString)
    }
}
