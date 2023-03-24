package com.snowplowanalytics.snowplowdemocompose.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.snowplowanalytics.snowplow.Snowplow
import com.snowplowanalytics.snowplow.configuration.EmitterConfiguration
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration
import com.snowplowanalytics.snowplow.controller.TrackerController
import com.snowplowanalytics.snowplow.emitter.BufferOption
import com.snowplowanalytics.snowplow.event.ScreenView
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow.tracker.LogLevel

object Tracking {
    @Composable
    fun setup(namespace: String) : TrackerController {
        // Replace this collector endpoint with your own
        val networkConfig = NetworkConfiguration("https://23a6-82-26-43-253.ngrok.io", HttpMethod.POST)
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
    fun AutoTrackScreenView(navController: NavController) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            Snowplow.defaultTracker?.track(ScreenView(destination.route ?: "null"))
        }
    }
    
    @Composable
    fun ManuallyTrackScreenView(screenName: String,
                                entities: List<SelfDescribingJson>? = null,
    ) {
        LaunchedEffect(Unit, block = {
            val event = ScreenView(screenName).entities(entities)
            Snowplow.defaultTracker?.track(event)
        })
    }
}
