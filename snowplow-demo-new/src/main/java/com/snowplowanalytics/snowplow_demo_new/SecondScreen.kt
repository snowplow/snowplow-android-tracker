package com.snowplowanalytics.snowplow_demo_new

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.snowplowanalytics.snowplow.event.ScreenView
import java.util.*

@Composable
fun SecondScreen() {
    Tracking.tracker()?.track(ScreenView("second", UUID.randomUUID()))
    
    Column(modifier = Modifier.padding(all = 8.dp)) {
        Text("This is the second screen.")
    }
}
