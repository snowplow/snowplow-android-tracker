package com.snowplowanalytics.core;

import androidx.annotation.NonNull;

import com.snowplowanalytics.core.tracker.ServiceProviderInterface;

public class Controller {

    public final ServiceProviderInterface serviceProvider;

    public Controller(@NonNull ServiceProviderInterface serviceProvider) {
        this.serviceProvider = serviceProvider;
    }
}
