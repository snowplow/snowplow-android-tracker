package com.snowplowanalytics.snowplow_demo_new.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
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
import java.util.*

object Tracking {
    @Composable
    fun setup(namespace: String) : TrackerController {
        // Replace this collector endpoint with your own
        val networkConfig = NetworkConfiguration("https://cb8c-18-194-133-57.ngrok.io", HttpMethod.POST)
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
    fun TrackScreenView(screenName: String, 
                        screenId: UUID? = UUID.randomUUID(), 
                        entities: List<SelfDescribingJson>? = null,
    ) {
        LaunchedEffect(Unit, block = {
            val event = ScreenView(screenName, screenId).entities(entities)
            Snowplow.defaultTracker?.track(event)
        })
    }
}
