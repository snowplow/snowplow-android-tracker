package com.snowplowanalytics.snowplow_demo_new

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.snowplowanalytics.snowplow_demo_new.ui.theme.ComposeDemoTheme

class MainActivity : ComponentActivity() {
    private val listvm = SchemaListViewModel()
    private val detailvm = SchemaDetailViewModel()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeDemoTheme {
                ComposeDemoApp(
                    listViewModel = listvm, 
                    detailViewModel = detailvm
                )
            }
        }
    }
}
//
//@Composable
//fun TodoView(vm: TodoViewModel) {
//    LaunchedEffect(Unit, block = {
//        vm.getTodoList()
//    })
//    
//    Scaffold(topBar = {
//        TopAppBar (
//            title = {
//                Row {
//                    Text("Todos")
//                }
//            })
//
//    }) { contentPadding ->
//        if (vm.errorMessage.isEmpty()) {
//            Column(modifier = Modifier.padding(contentPadding)) {
//                LazyColumn(modifier = Modifier.fillMaxHeight()) {
//                    items(vm.todoList) { todo ->
//                        Column {
//                            Row(
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .padding(16.dp),
//                                horizontalArrangement = Arrangement.SpaceBetween
//                            ) {
//                                Box(
//                                    modifier = Modifier
//                                        .fillMaxWidth()
//                                        .padding(0.dp, 0.dp, 16.dp, 0.dp)
//                                ) {
//                                    Text(
//                                        todo.title,
//                                        maxLines = 1,
//                                        overflow = TextOverflow.Ellipsis
//                                    )
//                                }
//                                Spacer(modifier = Modifier.width(16.dp))
//                            }
//                            Divider()
//                        }
//                    }
//                }
//            }
//        } else {
//            Text(vm.errorMessage)
//        }
//        
//        
//    }
//}
//



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
