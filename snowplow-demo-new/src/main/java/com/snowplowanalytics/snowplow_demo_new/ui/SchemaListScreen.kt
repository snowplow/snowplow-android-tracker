package com.snowplowanalytics.snowplow_demo_new.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snowplowanalytics.snowplow.event.ScreenView
import com.snowplowanalytics.snowplow_demo_new.R
import com.snowplowanalytics.snowplow_demo_new.data.SchemaUrlParts
import com.snowplowanalytics.snowplow_demo_new.data.Tracking
import java.util.*

@Composable
fun SchemaListScreen(
    vm: SchemaListViewModel,
    onSchemaClicked: (String) -> Unit = {}
) {
    LaunchedEffect(Unit, block = {
        vm.getSchemaList()
    })

    // Tracks a ScreenView event
    Tracking.tracker()?.track(ScreenView("list", UUID.randomUUID()))

    Scaffold(topBar = {
        TopAppBar (
            title = {
                Row {
                    Text("Schemas")
                }
            })

    }) { contentPadding ->
        Box(modifier = Modifier.padding(contentPadding)) {
            if (vm.errorMessage.isEmpty()) {
                Column(modifier = Modifier.padding(contentPadding)) {
                    LazyColumn(modifier = Modifier.fillMaxHeight()) {
                        items(vm.schemaPartsList) { schema ->
                            SchemaCard(schema = schema, onClick = onSchemaClicked)
                        }
                    }
                }
            } else {
                Text(vm.errorMessage)
            }
        }

    }
}

@Composable
fun SchemaCard(schema: SchemaUrlParts, onClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { onClick.invoke(schema.url) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,


        ) {
        
        Column(modifier = Modifier.fillMaxWidth(0.9F)) {
            Text(
                text = "Name", 
                modifier = Modifier.padding(4.dp), 
                style = TextStyle(fontSize = 12.sp)
            )
            Text(
                schema.name,
                style = TextStyle(fontSize = 14.sp),
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.paddingFromBaseline(bottom = 4.dp),
            )
            Text(
                text = "Vendor",
                modifier = Modifier.padding(4.dp),
                style = TextStyle(fontSize = 12.sp)
            )
            Text(
                schema.vendor,
                maxLines = 1,
                style = TextStyle(fontSize = 14.sp),
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.paddingFromBaseline(bottom = 4.dp),
            )
            Text(
                text = "Version",
                modifier = Modifier.padding(4.dp),
                style = TextStyle(fontSize = 12.sp)
            )
            Text(
                schema.version,
                style = TextStyle(fontSize = 14.sp),
                modifier = Modifier.paddingFromBaseline(bottom = 4.dp),
            )
        }
        Column {
            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = stringResource(R.string.detail_button)
            )
        }
        
        
        
        Spacer(modifier = Modifier.width(16.dp))
    }
    Divider()
}

@Preview
@Composable
fun PreviewCard() {
    SchemaCard(
        schema = SchemaUrlParts(
            "url",
            "namenamename",
            "vendorvendorvendor.vendorvendorvendor.vendor.vendorvendorvendor",
            "1.0.0"
        ), onClick = {}
    )
}
