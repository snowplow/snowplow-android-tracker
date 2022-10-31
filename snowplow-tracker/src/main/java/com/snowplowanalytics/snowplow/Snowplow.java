package com.snowplowanalytics.snowplow;

import android.content.Context;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.core.util.Pair;

import com.snowplowanalytics.snowplow.configuration.Configuration;
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration;
import com.snowplowanalytics.snowplow.configuration.RemoteConfiguration;
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration;
import com.snowplowanalytics.snowplow.controller.TrackerController;
import com.snowplowanalytics.snowplow.internal.remoteconfiguration.ConfigurationBundle;
import com.snowplowanalytics.snowplow.internal.remoteconfiguration.ConfigurationProvider;
import com.snowplowanalytics.snowplow.internal.remoteconfiguration.ConfigurationState;
import com.snowplowanalytics.snowplow.internal.remoteconfiguration.FetchedConfigurationBundle;
import com.snowplowanalytics.snowplow.internal.tracker.ServiceProvider;
import com.snowplowanalytics.snowplow.internal.tracker.TrackerWebViewInterface;
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
    @Nullable
    private static ConfigurationProvider configurationProvider;

    // Remote configuration

    /**
     * Setup a single or a set of tracker instances which will be used inside the app to track events.
     * The app can run multiple tracker instances which will be identified by string `namespaces`.
     * The trackers configuration is automatically download from the endpoint indicated in the `RemoteConfiguration`
     * passed as argument. For more details see `RemoteConfiguration`.
     *
     * The method is asynchronous and you can receive the list of the created trackers in the callbacks once the trackers are created.
     * The callback can be called multiple times in case a cached configuration is ready and later a fetched configuration is available.
     * You can also pass as argument a default configuration in case there isn't a cached configuration and it's not able to download
     * a new one. The downloaded configuration updates the cached one only if the configuration version is greater than the cached one.
     * Otherwise the cached one is kept and the callback is not called.
     *
     * IMPORTANT: The EventStore will persist all the events that have been tracked but not yet sent.
     * Those events are attached to the namespace.
     * If the tracker is removed or the app relaunched with a different namespace, those events can't
     * be sent to the collector and they remain in a zombie state inside the EventStore.
     * To remove all the zombie events you can an internal method `removeUnsentEventsExceptForNamespaces` on `SPSQLEventStore`
     * which will delete all the EventStores instanced with namespaces not listed in the passed list.
     *
     * @param context The Android app context.
     * @param remoteConfiguration The remote configuration used to indicate where to download the configuration from.
     * @param defaultBundles The default configuration passed by default in case there isn't a cached version and it's able to download a new one.
     * @param onSuccess The callback called when a configuration (cached or downloaded) is set.
     *                  It passes a pair object with the list of the namespaces associated
     *                  to the created trackers and the state of the configuration – whether it was
     *                  retrieved from cache or fetched over the network.
     */
    public static void setup(@NonNull Context context, @NonNull RemoteConfiguration remoteConfiguration, @Nullable List<ConfigurationBundle> defaultBundles, @Nullable Consumer<Pair<List<String>, ConfigurationState>> onSuccess) {
        configurationProvider = new ConfigurationProvider(remoteConfiguration, defaultBundles);
        configurationProvider.retrieveConfiguration(context, false, fetchedConfigurationPair -> {
            FetchedConfigurationBundle fetchedConfigurationBundle = fetchedConfigurationPair.first;
            ConfigurationState configurationState = fetchedConfigurationPair.second;
            List<ConfigurationBundle> bundles = fetchedConfigurationBundle.configurationBundle;
            List<String> namespaces = createTracker(context, bundles);
            if (onSuccess != null) {
                onSuccess.accept(new Pair<>(namespaces, configurationState));
            }
        });
    }

    /**
     * Reconfigure, create or delete the trackers based on the configuration downloaded remotely.
     * The trackers configuration is automatically download from the endpoint indicated in the `RemoteConfiguration`
     * previously used to setup the trackers.
     *
     * The method is asynchronous and you can receive the list of the created trackers in the callbacks once the trackers are created.
     * The downloaded configuration updates the cached one only if the configuration version is greater than the cached one.
     * Otherwise the cached one is kept and the callback is not called.
     *
     * IMPORTANT: The EventStore will persist all the events that have been tracked but not yet sent.
     * Those events are attached to the namespace.
     * If the tracker is removed or the app relaunched with a different namespace, those events can't
     * be sent to the collector and they remain in a zombie state inside the EventStore.
     * To remove all the zombie events you can an internal method `removeUnsentEventsExceptForNamespaces` on `SPSQLEventStore`
     * which will delete all the EventStores instanced with namespaces not listed in the passed list.
     *
     * @param context The Android app context.
     * @param onSuccess The callback called when a configuration (cached or downloaded) is set.
     *                  It passes a pair object with the list of the namespaces associated
     *                  to the created trackers and the state of the configuration – whether it was
     *                  retrieved from cache or fetched over the network.
     */
    public static void refresh(@NonNull Context context, @Nullable Consumer<Pair<List<String>, ConfigurationState>> onSuccess) {
        if (configurationProvider == null) return;
        configurationProvider.retrieveConfiguration(context, true, fetchedConfigurationPair -> {
            FetchedConfigurationBundle fetchedConfigurationBundle = fetchedConfigurationPair.first;
            ConfigurationState configurationState = fetchedConfigurationPair.second;
            List<ConfigurationBundle> bundles = fetchedConfigurationBundle.configurationBundle;
            List<String> namespaces = createTracker(context, bundles);
            if (onSuccess != null) {
                onSuccess.accept(new Pair<>(namespaces, configurationState));
            }
        });
    }

    // Standard configuration

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
        return serviceProvider.getOrMakeTrackerController();
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
        return serviceProvider == null ? null : serviceProvider.getOrMakeTrackerController();
    }

    /**
     * Using the namespace identifier is possible to get the trackerController if already instanced.
     *
     * @param namespace The namespace that identifies the tracker.
     * @return The tracker if it exist with that namespace.
     */
    @Nullable
    public synchronized static TrackerController getTracker(@NonNull String namespace) {
        ServiceProvider serviceProvider = serviceProviderInstances.get(namespace);
        if (serviceProvider == null) {
            return null;
        }
        return serviceProvider.getOrMakeTrackerController();
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

    /**
     * Add a JavaScript interface to the Web view that listens for events tracked using the Snowplow library for Web views.
     * @param webView Web view instance in which to subscribe for events
     */
    public static void subscribeToWebViewEvents(@NonNull WebView webView) {
        webView.addJavascriptInterface(new TrackerWebViewInterface(), TrackerWebViewInterface.TAG);
    }

    // Private methods

    @NonNull
    private synchronized static List<String> createTracker(@NonNull Context context, @NonNull List<ConfigurationBundle> bundles) {
        List<String> namespaces = new ArrayList<>();
        for (ConfigurationBundle bundle : bundles) {
            if (bundle.networkConfiguration == null) {
                // remove tracker if it exists
                TrackerController tracker = getTracker(bundle.namespace);
                if (tracker != null) {
                    removeTracker(tracker);
                }
            } else {
                List<Configuration> list = bundle.getConfigurations();
                Configuration[] array = list.toArray(new Configuration[list.size()]);
                createTracker(context, bundle.namespace, bundle.networkConfiguration, array);
                namespaces.add(bundle.namespace);
            }
        }
        return namespaces;
    }

    private synchronized static boolean registerInstance(@NonNull ServiceProvider serviceProvider) {
        String namespace = serviceProvider.getNamespace();
        boolean isOverriding = serviceProviderInstances.put(namespace, serviceProvider) != null;
        if (defaultServiceProvider == null) {
            defaultServiceProvider = serviceProvider;
        }
        return isOverriding;
    }

}
