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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snowplowanalytics.snowplow.event.ScreenView
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow_demo_new.R
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
            Column(modifier = Modifier.padding(all = 8.dp)) {
                Spacer(modifier = Modifier.width(40.dp))

                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "URL",
                        modifier = Modifier.padding(4.dp),
                        style = TextStyle(fontSize = 12.sp)
                    )
                    Text(
                        schemaUrl,
                        style = TextStyle(fontSize = 14.sp),
                        modifier = Modifier.paddingFromBaseline(bottom = 4.dp),
                    )
                    Spacer(modifier = Modifier.width(30.dp))

                    Text(
                        text = "Name",
                        modifier = Modifier.padding(4.dp),
                        style = TextStyle(fontSize = 12.sp)
                    )
                    Text(
                        schemaParts.name,
                        style = TextStyle(fontSize = 14.sp),
                        modifier = Modifier.paddingFromBaseline(bottom = 4.dp),
                    )
                    Spacer(modifier = Modifier.width(30.dp))

                    Text(
                        text = "Vendor",
                        modifier = Modifier.padding(4.dp),
                        style = TextStyle(fontSize = 12.sp)
                    )
                    Text(
                        schemaParts.vendor,
                        style = TextStyle(fontSize = 14.sp),
                        modifier = Modifier.paddingFromBaseline(bottom = 4.dp),
                    )
                    Spacer(modifier = Modifier.width(30.dp))

                    Text(
                        text = "Version",
                        modifier = Modifier.padding(4.dp),
                        style = TextStyle(fontSize = 12.sp)
                    )
                    Text(
                        schemaParts.version,
                        style = TextStyle(fontSize = 14.sp),
                        modifier = Modifier.paddingFromBaseline(bottom = 4.dp),
                    )
                    Spacer(modifier = Modifier.width(20.dp))

                    Text(
                        text = "Description",
                        modifier = Modifier.padding(4.dp),
                        style = TextStyle(fontSize = 12.sp)
                    )
                    Text(
                        vm.description.value ?: "No description found.",
                        style = TextStyle(fontSize = 14.sp),
                        modifier = Modifier.paddingFromBaseline(bottom = 4.dp),
                    )
                    Spacer(modifier = Modifier.width(20.dp))

                    Text(
                        text = "JSON Schema",
                        modifier = Modifier.padding(4.dp),
                        style = TextStyle(fontSize = 12.sp)
                    )
                    Text(
                        vm.json.value.toString(4).replace("\\", ""),
                        style = TextStyle(fontSize = 14.sp),
                        modifier = Modifier.paddingFromBaseline(bottom = 4.dp),
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                }
            }
        }
    } 

    

    
}
