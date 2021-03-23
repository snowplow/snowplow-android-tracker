package com.snowplowanalytics.snowplow;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.configuration.Configuration;
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration;
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration;
import com.snowplowanalytics.snowplow.controller.TrackerController;
import com.snowplowanalytics.snowplow.internal.tracker.ServiceProvider;
import com.snowplowanalytics.snowplow.network.HttpMethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Snowplow {

    // Private properties

    @Nullable
    private static ServiceProvider defaultServiceProvider;
    @NonNull
    private final static Map<String, ServiceProvider> serviceProviderInstances = new HashMap<>();

    // Public methods

    @NonNull
    public static TrackerController createTracker(@NonNull Context context, @NonNull String namespace, @NonNull String endpoint, @NonNull HttpMethod method) {
        NetworkConfiguration network = new NetworkConfiguration(endpoint, method);
        TrackerConfiguration tracker = new TrackerConfiguration(context.getPackageName());
        return Snowplow.createTracker(context, namespace, network, tracker);
    }

    @NonNull
    public static TrackerController createTracker(@NonNull Context context, @NonNull String namespace, @NonNull NetworkConfiguration network, @NonNull Configuration... configurations) {
        ServiceProvider serviceProvider = serviceProviderInstances.get(namespace);
        if (serviceProvider != null) {
            List<Configuration> configList = new ArrayList<>(Arrays.asList(configurations));
            configList.add(network);
            serviceProvider.reset(configList);
        } else {
            serviceProvider = new ServiceProvider(context, namespace, network, Arrays.asList(configurations));
            registerInstance(serviceProvider);
        }
        return serviceProvider.getTrackerController();
    }

    @Nullable
    public static TrackerController getDefaultTracker() {
        ServiceProvider serviceProvider = defaultServiceProvider;
        return serviceProvider == null ? null : serviceProvider.getTrackerController();
    }

    public synchronized static boolean setTrackerAsDefault(@NonNull TrackerController trackerController) {
        ServiceProvider serviceProvider = serviceProviderInstances.get(trackerController.getNamespace());
        if (serviceProvider != null) {
            defaultServiceProvider = serviceProvider;
            return true;
        }
        return false;
    }

    public synchronized static boolean removeTracker(@NonNull TrackerController trackerController) {
        String namespace = trackerController.getNamespace();
        ServiceProvider serviceProvider = serviceProviderInstances.get(namespace);
        if (serviceProvider != null) {
            serviceProvider.shutdown();
            serviceProviderInstances.remove(namespace);
            if (serviceProvider == defaultServiceProvider) {
                defaultServiceProvider = null;
            }
            return true;
        }
        return false;
    }

    public synchronized static void removeAllTrackers() {
        defaultServiceProvider = null;
        Collection<ServiceProvider> serviceProviders = serviceProviderInstances.values();
        serviceProviderInstances.clear();
        for (ServiceProvider sp : serviceProviders) {
            sp.shutdown();
        }
    }

    @NonNull
    public static Set<String> getInstancedTrackerNamespaces() {
        return serviceProviderInstances.keySet();
    }

    // Private methods

    private synchronized static boolean registerInstance(@NonNull ServiceProvider serviceProvider) {
        String namespace = serviceProvider.namespace;
        boolean isOverriding = serviceProviderInstances.put(namespace, serviceProvider) != null;
        if (defaultServiceProvider == null) {
            defaultServiceProvider = serviceProvider;
        }
        return isOverriding;
    }

}
