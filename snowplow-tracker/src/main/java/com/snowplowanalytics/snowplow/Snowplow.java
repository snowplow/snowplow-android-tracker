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

    private static volatile Snowplow singleton;

    // Private properties

    @Nullable
    private ServiceProvider defaultServiceProvider;
    @NonNull
    private final Map<String, ServiceProvider> serviceProviderInstances = new HashMap<>();

    // Singleton

    private Snowplow() {
        if (singleton != null) {
            throw new RuntimeException("Use `getInstance` method to create this class.");
        }
    }

    private static Snowplow getInstance() {
        if (singleton == null) {
            synchronized (Snowplow.class) {
                if (singleton == null) {
                    singleton = new Snowplow();
                }
            }
        }
        return singleton;
    }

    // Public methods

    @NonNull
    public static TrackerController createTracker(@NonNull Context context, @NonNull String namespace, @NonNull String endpoint, @NonNull HttpMethod method) {
        NetworkConfiguration network = new NetworkConfiguration(endpoint, method);
        TrackerConfiguration tracker = new TrackerConfiguration(context.getPackageName());
        return Snowplow.createTracker(context, namespace, network, tracker);
    }

    @NonNull
    public static TrackerController createTracker(@NonNull Context context, @NonNull String namespace, @NonNull NetworkConfiguration network, @NonNull Configuration... configurations) {
        Snowplow snowplow = Snowplow.getInstance();
        ServiceProvider serviceProvider = snowplow.serviceProviderInstances.get(namespace);
        if (serviceProvider != null) {
            List<Configuration> configList = new ArrayList<>(Arrays.asList(configurations));
            configList.add(network);
            serviceProvider.reset(configList);
        } else {
            serviceProvider = new ServiceProvider(context, namespace, network, Arrays.asList(configurations));
            snowplow.registerInstance(serviceProvider);
        }
        return serviceProvider.getTrackerController();
    }

    @Nullable
    public static TrackerController getDefaultTracker() {
        ServiceProvider serviceProvider = Snowplow.getInstance().defaultServiceProvider;
        return serviceProvider == null ? null : serviceProvider.getTrackerController();
    }

    public static boolean setTrackerAsDefault(@NonNull TrackerController trackerController) {
        Snowplow snowplow = Snowplow.getInstance();
        synchronized (snowplow) {
            ServiceProvider serviceProvider = snowplow.serviceProviderInstances.get(trackerController.getNamespace());
            if (serviceProvider != null) {
                snowplow.defaultServiceProvider = serviceProvider;
                return true;
            }
            return false;
        }
    }

    public static boolean removeTracker(@NonNull TrackerController trackerController) {
        Snowplow snowplow = Snowplow.getInstance();
        synchronized (snowplow) {
            String namespace = trackerController.getNamespace();
            ServiceProvider serviceProvider = snowplow.serviceProviderInstances.get(namespace);
            if (serviceProvider != null) {
                serviceProvider.shutdown();
                snowplow.serviceProviderInstances.remove(namespace);
                if (serviceProvider == snowplow.defaultServiceProvider) {
                    snowplow.defaultServiceProvider = null;
                }
                return true;
            }
            return false;
        }
    }

    public static void removeAllTrackers() {
        Snowplow snowplow = Snowplow.getInstance();
        synchronized (snowplow) {
            snowplow.defaultServiceProvider = null;
            Collection<ServiceProvider> serviceProviders = snowplow.serviceProviderInstances.values();
            snowplow.serviceProviderInstances.clear();
            for (ServiceProvider sp : serviceProviders) {
                sp.shutdown();
            }
        }
    }

    @NonNull
    public static Set<String> getInstancedTrackerNamespaces() {
        return Snowplow.getInstance().serviceProviderInstances.keySet();
    }

    // Private methods

    private synchronized boolean registerInstance(@NonNull ServiceProvider serviceProvider) {
        String namespace = serviceProvider.namespace;
        boolean isOverriding = serviceProviderInstances.put(namespace, serviceProvider) != null;
        if (defaultServiceProvider == null) {
            defaultServiceProvider = serviceProvider;
        }
        return isOverriding;
    }

}
