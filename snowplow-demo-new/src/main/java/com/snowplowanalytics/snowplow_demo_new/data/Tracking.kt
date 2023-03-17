package com.snowplowanalytics.snowplow_demo_new.data

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.snowplowanalytics.snowplow.Snowplow
import com.snowplowanalytics.snowplow.configuration.EmitterConfiguration
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration
import com.snowplowanalytics.snowplow.controller.TrackerController
import com.snowplowanalytics.snowplow.emitter.BufferOption
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.tracker.LogLevel

object Tracking {
    const val namespace = "compose_demo"
    
    @Composable
    fun setup() : TrackerController {
        // Replace this collector endpoint with your own
        val networkConfig = NetworkConfiguration("https://7211-82-26-43-253.ngrok.io", HttpMethod.POST)
        val trackerConfig = TrackerConfiguration("appID").logLevel(LogLevel.DEBUG)
        val emitterConfig = EmitterConfiguration().bufferOption(BufferOption.Single)
        
        return Snowplow.createTracker(
            LocalContext.current, 
            namespace,
            networkConfig,
            trackerConfig,
            emitterConfig
        )
    }
    
    @Composable
    fun tracker(namespace: String = this.namespace) : TrackerController? {
        println("❗️ accessed tracker!")
        return Snowplow.getTracker(namespace)
    }
}
