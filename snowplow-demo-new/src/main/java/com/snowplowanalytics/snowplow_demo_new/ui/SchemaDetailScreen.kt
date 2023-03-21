package com.snowplowanalytics.snowplow_demo_new.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.snowplowanalytics.snowplow.event.ScreenView
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow_demo_new.R
import com.snowplowanalytics.snowplow_demo_new.data.SchemaUrlParts
import com.snowplowanalytics.snowplow_demo_new.data.Tracking
import java.util.*


@Composable
fun SchemaDetailScreen(
    vm: SchemaDetailViewModel,
    schemaUrl: String,
    onBackButtonClicked: () -> Unit = {}
) {
    LaunchedEffect(Unit, block = {
        vm.getSchemaDescription(schemaUrl)
        vm.getSchemaJson(schemaUrl)
    })
    val schemaParts = vm.processSchemaUrl(schemaUrl)
    
    val entity = SelfDescribingJson(
        "iglu:com.snowplowanalytics.iglu/anything-a/jsonschema/1-0-0", 
        hashMapOf("name" to schemaParts.name, "vendor" to schemaParts.vendor)
    )
    val event = ScreenView("detail", UUID.randomUUID()).entities(listOf(entity))

    // Tracks a ScreenView with attached context entity.
    // This entity records information about the specific schema being viewed
    Tracking.tracker()?.track(event)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(schemaParts.name) },
                navigationIcon = {
                    IconButton(onClick = onBackButtonClicked) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_button)
                        )
                    }
                }
            )
        }
    ) { contentPadding ->
        Box(modifier = Modifier.padding(contentPadding)) {
            Column(modifier = Modifier.padding(all = 12.dp)) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    DetailsSchemaProperty(title = "URL", data = schemaUrl)
                    DetailsSchemaProperty(title = "Name", data = schemaParts.name)
                    DetailsSchemaProperty(title = "Vendor", data = schemaParts.vendor)
                    DetailsSchemaProperty(title = "Version", data = schemaParts.version)
                    DetailsSchemaProperty(
                        title = "Description", 
                        data = vm.description.value ?: "null"
                    )
                    DetailsSchemaProperty(
                        title = "JSON Schema", 
                        data = vm.json.value.toString(4).replace("\\", "")
                    )
                }
            }
        }
    }
}

@Composable
fun DetailsSchemaProperty(title: String, data: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.subtitle1
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        data,
        style = MaterialTheme.typography.body1,
    )
    Spacer(modifier = Modifier.height(16.dp))
}
