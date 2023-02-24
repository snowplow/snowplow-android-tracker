package com.snowplowanalytics.snowplow_demo_new

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.snowplowanalytics.snowplow.event.ScreenView
import java.util.*

@Composable
fun MainScreen(
    onNextButtonClicked: () -> Unit = {},
    onTrackButtonClicked: () -> Unit = {}
) {
    Tracking.tracker()?.track(ScreenView("main", UUID.randomUUID()))
    
    Column(modifier = Modifier.padding(all = 8.dp)) {
        Button(onClick = onNextButtonClicked) {
            Text("Next page")
        }
        
        Spacer(modifier = Modifier.width(20.dp))
        
        Button(onClick = onTrackButtonClicked) {
            Text("Track an event")
        }
    }
}
