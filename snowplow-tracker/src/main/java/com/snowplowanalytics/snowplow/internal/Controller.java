package com.snowplowanalytics.snowplow.internal;

import androidx.annotation.NonNull;

import com.snowplowanalytics.snowplow.internal.tracker.ServiceProviderInterface;

public class Controller {

    public final ServiceProviderInterface serviceProvider;

    public Controller(@NonNull ServiceProviderInterface serviceProvider) {
        this.serviceProvider = serviceProvider;
    }
}
