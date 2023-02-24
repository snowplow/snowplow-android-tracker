package com.snowplowanalytics.snowplow_demo_new

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.snowplowanalytics.snowplow_demo_new.ui.theme.ComposeDemoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeDemoTheme {
                ComposeDemoApp()
            }
        }
    }
}

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
