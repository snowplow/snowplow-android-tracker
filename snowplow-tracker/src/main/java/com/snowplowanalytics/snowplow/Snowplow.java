package com.snowplowanalytics.snowplow;

import android.content.Context;

import androidx.annotation.NonNull;

import com.snowplowanalytics.snowplow.configuration.Configuration;
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration;
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration;
import com.snowplowanalytics.snowplow.controller.TrackerController;
import com.snowplowanalytics.snowplow.internal.tracker.ServiceProvider;
import com.snowplowanalytics.snowplow.network.HttpMethod;

import java.util.Arrays;

public class Snowplow {

    @NonNull
    public static TrackerController createTracker(@NonNull Context context, @NonNull String namespace, @NonNull String endpoint, @NonNull HttpMethod method) {
        NetworkConfiguration network = new NetworkConfiguration(endpoint, method);
        TrackerConfiguration tracker = new TrackerConfiguration(context.getPackageName());
        return Snowplow.createTracker(context, namespace, network, tracker);
    }

    @NonNull
    public static TrackerController createTracker(@NonNull Context context, @NonNull String namespace, @NonNull NetworkConfiguration network, @NonNull Configuration... configurations) {
        ServiceProvider serviceProvider = new ServiceProvider(context, namespace, network, Arrays.asList(configurations));
        return serviceProvider.getTrackerController();
    }

}
