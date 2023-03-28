package com.snowplowanalytics.snowplowdemocompose.ui

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.snowplowanalytics.snowplowdemocompose.R
import com.snowplowanalytics.snowplowdemocompose.data.SchemaUrlParts

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
            ListSchemaProperty(title = "Name", data = schema.name)
            ListSchemaProperty(title = "Vendor", data = schema.vendor)
            ListSchemaProperty(title = "Version", data = schema.version)
        }
        Column {
            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = stringResource(R.string.detail_button),
                tint = MaterialTheme.colors.primary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
    Divider(
        color = MaterialTheme.colors.primary
    )
}

@Composable
fun ListSchemaProperty(title: String, data: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.subtitle1
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        data,
        style = MaterialTheme.typography.body1,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    Spacer(modifier = Modifier.height(6.dp))
}
