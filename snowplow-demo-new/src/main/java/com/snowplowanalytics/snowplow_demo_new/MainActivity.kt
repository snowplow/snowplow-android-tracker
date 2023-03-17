package com.snowplowanalytics.snowplow_demo_new

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.snowplowanalytics.snowplow_demo_new.ui.SchemaDetailViewModel
import com.snowplowanalytics.snowplow_demo_new.ui.SchemaListViewModel
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
