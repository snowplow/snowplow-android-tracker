package com.snowplowanalytics.snowplow_demo_new

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.snowplowanalytics.snowplow.Snowplow
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration
import com.snowplowanalytics.snowplow.controller.TrackerController
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.tracker.LogLevel

object Tracking {
    @Composable
    fun setup() : TrackerController {
        println("❗️ tracker setup")
        
        val trackerConfig = TrackerConfiguration("appID").logLevel(LogLevel.VERBOSE)
        val networkConfig = NetworkConfiguration("https://984a-82-26-43-253.ngrok.io", HttpMethod.POST)
        
        return Snowplow.createTracker(
            LocalContext.current, 
            "compose_demo",
            networkConfig,
            trackerConfig
        )
    }
    
    @Composable
    fun tracker() : TrackerController? {
        println("❗" + Snowplow.getTracker("compose_demo"))
        return Snowplow.getTracker("compose_demo")
    }
}
