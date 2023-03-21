package com.snowplowanalytics.snowplowdemocompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.snowplowanalytics.snowplowdemocompose.ui.SchemaDetailViewModel
import com.snowplowanalytics.snowplowdemocompose.ui.SchemaListViewModel
import com.snowplowanalytics.snowplowdemocompose.ui.theme.ComposeDemoTheme

class MainActivity : ComponentActivity() {
    private val listVm = SchemaListViewModel()
    private val detailVm = SchemaDetailViewModel()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeDemoTheme {
                ComposeDemoApp(
                    listViewModel = listVm, 
                    detailViewModel = detailVm
                )
            }
        }
    }
}
