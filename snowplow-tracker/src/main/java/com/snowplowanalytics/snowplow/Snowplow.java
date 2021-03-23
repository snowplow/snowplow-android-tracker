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

/**
 * Entry point to instance a new Snowplow tracker.
 */
public class Snowplow {

    // Private properties

    @Nullable
    private static ServiceProvider defaultServiceProvider;
    @NonNull
    private final static Map<String, ServiceProvider> serviceProviderInstances = new HashMap<>();

    // Public methods

    /**
     * Create a new tracker instance which will be used inside the app to track events.
     * The app can run multiple tracker instances which will be identified by string `namespaces`.
     * The tracker will be configured with default setting and only the collector endpoint URL need
     * to be passed for the configuration.
     * For the default configuration of the tracker see {@link TrackerConfiguration#TrackerConfiguration(String)}
     *
     * To configure tracker with more details see {@link #createTracker(Context, String, NetworkConfiguration, Configuration...)}
     * To use the tracker as singleton see {@link #getDefaultTracker()}
     *
     * @apiNote IMPORTANT: The EventStore will persist all the events that have been tracked but not yet sent.
     * Those events are attached to the namespace.
     * If the tracker is removed or the app relaunched with a different namespace, those events can't
     * be sent to the collector and they remain in a zombie state inside the EventStore.
     * To remove all the zombie events you can an internal method {@link com.snowplowanalytics.snowplow.internal.emitter.storage.EventStoreHelper#removeUnsentEventsExceptForNamespaces(Context, List) removeUnsentEventsExceptForNamespaces}
     * which will delete all the EventStores instanced with namespaces not listed in the passed list.
     *
     * @param context The Android app context.
     * @param namespace The namespace used to identify the current tracker among the possible
     *                  multiple tracker instances.
     * @param endpoint The URL of the collector.
     * @param method The method for the requests to the collector (GET or POST).
     * @return The tracker instance created.
     */
    @NonNull
    public static TrackerController createTracker(@NonNull Context context, @NonNull String namespace, @NonNull String endpoint, @NonNull HttpMethod method) {
        NetworkConfiguration network = new NetworkConfiguration(endpoint, method);
        TrackerConfiguration tracker = new TrackerConfiguration(context.getPackageName());
        return Snowplow.createTracker(context, namespace, network, tracker);
    }

    /**
     * Create a new tracker instance which will be used inside the app to track events.
     * The app can run multiple tracker instances which will be identified by string `namespaces`.
     * Each tracker can be configured by various configuration objects implementing the
     * {@link Configuration} interface.
     * For the default configuration of the tracker see {@link TrackerConfiguration#TrackerConfiguration(String)}
     *
     * The configurations are only for the setup of the tracker and *any change to the configuration
     * object properties will NOT change the tracker setup at runtime*.
     *
     * To use the tracker as singleton see {@link #getDefaultTracker()}
     *
     * @apiNote IMPORTANT: The EventStore will persist all the events that have been tracked but not yet sent.
     * Those events are attached to the namespace.
     * If the tracker is removed or the app relaunched with a different namespace, those events can't
     * be sent to the collector and they remain in a zombie state inside the EventStore.
     * To remove all the zombie events you can an internal method {@link com.snowplowanalytics.snowplow.internal.emitter.storage.EventStoreHelper#removeUnsentEventsExceptForNamespaces(Context, List) removeUnsentEventsExceptForNamespaces}
     * which will delete all the EventStores instanced with namespaces not listed in the passed list.
     *
     * @param context The Android app context.
     * @param namespace The namespace used to identify the current tracker among the possible
     *                  multiple tracker instances.
     * @param network The NetworkConfiguration object with settings for the communication with the
     *                collector.
     * @param configurations All the configuration objects with the details about the fine tuning of
     *                       the tracker.
     * @return The tracker instance created.
     */
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

    /**
     * The default tracker instance is the first created in the app, but that can be overridden programmatically
     * calling {@link #setTrackerAsDefault(TrackerController)}.
     *
     * @return The default tracker instance or `null` whether the tracker has been removed or never initialized.
     */
    @Nullable
    public static TrackerController getDefaultTracker() {
        ServiceProvider serviceProvider = defaultServiceProvider;
        return serviceProvider == null ? null : serviceProvider.getTrackerController();
    }

    /**
     * Set the passed tracker as default tracker if it's registered as an active tracker in the app.
     * If the passed instance is of a tracker which is already removed (see {@link #removeTracker(TrackerController) removeTracker})
     * then it can't become the new default tracker and the operation fails.
     *
     * @param trackerController The new default tracker.
     * @return Whether the tracker passed is registered among the active trackers of the app.
     */
  public synchronized static boolean setTrackerAsDefault(@NonNull TrackerController trackerController) {
        ServiceProvider serviceProvider = serviceProviderInstances.get(trackerController.getNamespace());
        if (serviceProvider != null) {
            defaultServiceProvider = serviceProvider;
            return true;
        }
        return false;
    }

    /**
     * A tracker can be removed from the active trackers of the app.
     * Once it has been removed it can't be added again or set as default.
     * The unique way to resume a removed tracker is creating a new tracker with same namespace and
     * same configurations.
     * The removed tracker is always stopped.
     *
     * @param trackerController The tracker controller to remove.
     * @return Whether it has been able to remove it.
     */
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

    /**
     * Remove all the trackers.
     * The removed tracker is always stopped.
     * @see #removeTracker(TrackerController)
     */
    public synchronized static void removeAllTrackers() {
        defaultServiceProvider = null;
        Collection<ServiceProvider> serviceProviders = serviceProviderInstances.values();
        serviceProviderInstances.clear();
        for (ServiceProvider sp : serviceProviders) {
            sp.shutdown();
        }
    }

    /**
     * @return Set of namespace of the active trackers in the app.
     */
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
