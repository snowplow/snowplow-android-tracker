package com.snowplowanalytics.snowplow_demo_new

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.snowplowanalytics.snowplow.event.ScreenView
import com.snowplowanalytics.snowplow_demo_new.data.SchemaUrlParts
import com.snowplowanalytics.snowplow_demo_new.data.Tracking
import com.snowplowanalytics.snowplow_demo_new.ui.SchemaListViewModel
import java.util.*

@Composable
fun SchemaListScreen(
    vm: SchemaListViewModel,
    onSchemaClicked: (String) -> Unit = {}
) {
    LaunchedEffect(Unit, block = {
        vm.getSchemaList()
    })

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
        horizontalArrangement = Arrangement.SpaceBetween
    
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp, 0.dp, 16.dp, 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {

                    Text("Name")
                    Text(
                        schema.name,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("Vendor")
                    Text(
                        schema.vendor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("Version")
                    Text(
                        schema.version,
                    )
                }
                Column(modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        imageVector = Icons.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.detail_button)
                    )
                }
            }
            
        }
        Spacer(modifier = Modifier.width(16.dp))
    }
    Divider()
}
