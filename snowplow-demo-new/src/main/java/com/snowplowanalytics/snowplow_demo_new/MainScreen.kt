package com.snowplowanalytics.snowplow_demo_new

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.snowplowanalytics.snowplow.event.ScreenView
import java.util.*

@Composable
fun MainScreen(
    onNextButtonClicked: () -> Unit = {},
    onTrackButtonClicked: () -> Unit = {},
    onSchemaClicked: (String) -> Unit = {}
) {
//    var selectedSchema by remember { mutableStateOf("") }
    
//    Tracking.tracker()?.track(ScreenView("main", UUID.randomUUID()))
//    IgluCentralAPI.schemas()
    
    Column(modifier = Modifier.padding(all = 8.dp)) {
        Button(onClick = onNextButtonClicked) {
            Text("Next page")
        }
        
        Spacer(modifier = Modifier.width(20.dp))
        
        Button(onClick = onTrackButtonClicked) {
            Text("Track an event")
        }
        
        Spacer(modifier = Modifier.width(20.dp))

        LazyColumn {
            items(schemasTemp()) {
                    schema -> SchemaTitleCard(schema = schema, onClick = onSchemaClicked)
            }
        }
    }


    
}

fun schemasTemp() : List<String> {
    return arrayListOf(
        "iglu:com.sendgrid/group_resubscribe/jsonschema/1-0-0",
        "iglu:com.amazon.aws.lambda/java_context/jsonschema/1-0-0",
        "iglu:com.snowplowanalytics.snowplow.input-adapters/segment_webhook_config/jsonschema/1-0-0",
        "iglu:com.snowplowanalytics.snowplow/media_player/jsonschema/1-0-0",
        "iglu:com.snowplowanalytics.monitoring.kinesis/app_warning/jsonschema/1-0-0",
        "iglu:com.google.analytics.measurement-protocol/content_experiment/jsonschema/1-0-0",
        "iglu:com.snowplowanalytics.mobile/screen_view/jsonschema/1-0-0",
        "iglu:com.snowplowanalytics.factotum/factfile/jsonschema/1-0-0",
        "iglu:com.getvero/updated/jsonschema/1-0-0",
        "iglu:com.clearbit.enrichment/company/jsonschema/1-0-0",
        "iglu:com.snowplowanalytics.snowplow/mobile_context/jsonschema/1-0-0",
        "iglu:com.snowplowanalytics.snowplow/mobile_context/jsonschema/1-0-1",
        "iglu:com.snowplowanalytics.snowplow/mobile_context/jsonschema/1-0-2",
        "iglu:com.mailgun/recipient_unsubscribed/jsonschema/1-0-0",
        "iglu:com.google.tag-manager.server-side/purchase/jsonschema/1-0-0",
        "iglu:com.segment/screen/jsonschema/1-0-0",
        "iglu:com.snowplowanalytics.snowplow.badrows/loader_iglu_error/jsonschema/2-0-0",
        "iglu:com.sendgrid/bounce/jsonschema/1-0-0",
        "iglu:com.google.ga4.enhanced-measurement/video_complete/jsonschema/1-0-0",
        "iglu:com.snowplowanalytics.snowplow.ecommerce/page/jsonschema/1-0-0"
    )
}

//@Composable
//fun SchemaList(schemas: List<String>) {
//    LazyColumn {
//        items(schemas) {
//            schema -> SchemaTitleCard(schema = schema, onClick = {  })
//        }
//    }
//}

@Composable
fun SchemaTitleCard(schema: String, onClick: (String) -> Unit) {
    Row(modifier = Modifier
        .padding(all = 8.dp)
        .border(width = 2.dp, color = MaterialTheme.colors.secondary)
        .clickable { onClick.invoke(schema) }
    ) {
        Text(text = schema, modifier = Modifier.padding(all = 8.dp))
    }
}

//@Preview(showBackground = true)
//@Composable
//private fun DefaultPreview() {
//    SchemaList(schemasTemp())
//}

//
//@Composable
//fun Buttons() {
//    Column(modifier = Modifier.padding(all = 8.dp)) {
//        Button(onClick = { /*TODO*/ }) {
//            Text("Next page")
//        }
//        Spacer(modifier = Modifier.width(20.dp))
//        Button(onClick = { /*TODO*/ }) {
//            Text("Track an event")
//        }
//    }
//}
//
//@Preview
//@Composable
//fun PreviewButtons() {
//    Buttons()
//}
//
//
//data class Message(val author: String, val body: String)
//
//@Composable
//fun Conversation(messages: List<Message>) {
//    LazyColumn {
//        items(messages) {
//            message -> MessageCard(msg = message)
//        }
//    }
//}
//
//@Composable
//fun MessageCard(msg: Message) {
//    Row(modifier = Modifier.padding(all = 8.dp)) {
//        Image(
//            painter = painterResource(id = R.drawable.ic_launcher_snowplow_web),
//            contentDescription = "Contact profile picture",
//            modifier = Modifier
//                .size(40.dp)
//                .clip(CircleShape)
//                .border(1.5.dp, MaterialTheme.colors.secondary, CircleShape)
//        )
//        
//        Spacer(modifier = Modifier.width(8.dp))
//        
//        var isExpanded by remember { mutableStateOf(false)}
//        val surfaceColour by animateColorAsState(
//            targetValue = if (isExpanded) MaterialTheme.colors.primary else MaterialTheme.colors.surface
//        )
//        
//        Column(modifier = Modifier.clickable { isExpanded = !isExpanded }) {
//            Text(text = msg.author, 
//                color = MaterialTheme.colors.secondaryVariant,
//                style = MaterialTheme.typography.subtitle2
//            )
//            Spacer(modifier = Modifier.width(4.dp))
//            
//            Surface(
//                shape = MaterialTheme.shapes.medium, 
//                elevation = 1.dp,
//                color = surfaceColour,
//                modifier = Modifier
//                    .animateContentSize()
//                    .padding(1.dp)
//            ) {
//                Text(text = msg.body,
//                    modifier = Modifier.padding(all = 4.dp),
//                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
//                    style = MaterialTheme.typography.body2
//                )
//            }
//        }
//    }
//}


//@Preview
//@Composable
//fun PreviewConversation() {
//    ComposeDemoTheme {
//        Conversation(messages = SampleData.conversationSample)
//    }
//}
