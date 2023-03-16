package com.snowplowanalytics.snowplow_demo_new

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.snowplowanalytics.snowplow_demo_new.data.SchemaUrlParts

@Composable
fun SchemaListScreen(
    vm: SchemaListViewModel, 
    onSchemaClicked: (String) -> Unit = {}
) {
    LaunchedEffect(Unit, block = {
        vm.getSchemaList()
    })

    Scaffold(topBar = {
        TopAppBar (
            title = {
                Row {
                    Text("Schemas")
                }
            })

    }) { contentPadding ->
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

@Composable
fun SchemaCard(schema: SchemaUrlParts, onClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { schema.url.let { onClick.invoke(it) } },
        horizontalArrangement = Arrangement.SpaceBetween
    
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp, 0.dp, 16.dp, 0.dp)
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
        }
        Spacer(modifier = Modifier.width(16.dp))
    }
    Divider()
}
