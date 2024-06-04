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
import com.snowplowanalytics.snowplow.event.ListItemView
import com.snowplowanalytics.snowplow.event.ScreenView
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow.tracker.LogLevel

object Tracking {
    /**
     * Replace this with the URI of your Snowplow collector
     * (e.g., Micro, Mini, or BDP).
     */
    private const val collectorEndpoint = "placeholder"
    
    @Composable
    fun setup(namespace: String) : TrackerController {
        val networkConfig = NetworkConfiguration(collectorEndpoint, HttpMethod.POST)
        val trackerConfig = TrackerConfiguration("appID")
            .logLevel(LogLevel.DEBUG)
            .screenViewAutotracking(false)
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
            val event = ScreenView(screenName)
            entities?.let { event.entities(it) }
            Snowplow.defaultTracker?.track(event)
        })
    }

    @Composable
    fun TrackListItemView(index: Int, itemsCount: Int?) {
        LaunchedEffect(Unit, block = {
            val event = ListItemView(index, itemsCount)
            Snowplow.defaultTracker?.track(event)
        })
    }
}
