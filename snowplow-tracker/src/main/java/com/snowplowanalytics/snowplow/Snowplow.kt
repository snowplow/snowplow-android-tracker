package com.snowplowanalytics.snowplow

import android.content.Context
import android.webkit.WebView
import androidx.core.util.Consumer
import androidx.core.util.Pair

import com.snowplowanalytics.snowplow.configuration.ConfigurationBundle
import com.snowplowanalytics.core.remoteconfiguration.ConfigurationProvider
import com.snowplowanalytics.snowplow.configuration.ConfigurationState
import com.snowplowanalytics.core.remoteconfiguration.FetchedConfigurationBundle
import com.snowplowanalytics.core.tracker.ServiceProvider
import com.snowplowanalytics.core.tracker.TrackerWebViewInterface

import com.snowplowanalytics.snowplow.configuration.Configuration
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration
import com.snowplowanalytics.snowplow.configuration.RemoteConfiguration
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration
import com.snowplowanalytics.snowplow.controller.TrackerController
import com.snowplowanalytics.snowplow.network.HttpMethod

import java.util.*

/**
 * Entry point to instance a new Snowplow tracker.
 */
object Snowplow {
    // Private properties
    private var defaultServiceProvider: ServiceProvider? = null
    private val serviceProviderInstances: MutableMap<String, ServiceProvider?> = HashMap()
    private var configurationProvider: ConfigurationProvider? = null
    
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
     * It passes a pair object with the list of the namespaces associated
     * to the created trackers and the state of the configuration – whether it was
     * retrieved from cache or fetched over the network.
     */
    @JvmStatic
    fun setup(
        context: Context,
        remoteConfiguration: RemoteConfiguration,
        defaultBundles: List<ConfigurationBundle?>?,
        onSuccess: Consumer<Pair<List<String>, ConfigurationState?>?>
    ) {
        configurationProvider = ConfigurationProvider(remoteConfiguration, defaultBundles)
        configurationProvider!!.retrieveConfiguration(
            context,
            false
        ) { fetchedConfigurationPair: Pair<FetchedConfigurationBundle, ConfigurationState> ->
            val fetchedConfigurationBundle = fetchedConfigurationPair.first
            val configurationState = fetchedConfigurationPair.second
            val bundles = fetchedConfigurationBundle.configurationBundle
            val namespaces = createTracker(context, bundles)
            onSuccess?.accept(Pair(namespaces, configurationState))
        }
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
     * It passes a pair object with the list of the namespaces associated
     * to the created trackers and the state of the configuration – whether it was
     * retrieved from cache or fetched over the network.
     */
    @JvmStatic
    fun refresh(context: Context, onSuccess: Consumer<Pair<List<String>, ConfigurationState>?>?) {
        if (configurationProvider == null) return
        
        configurationProvider!!.retrieveConfiguration(
            context,
            true
        ) { fetchedConfigurationPair: Pair<FetchedConfigurationBundle, ConfigurationState> ->
            val fetchedConfigurationBundle = fetchedConfigurationPair.first
            val configurationState = fetchedConfigurationPair.second
            val bundles = fetchedConfigurationBundle.configurationBundle
            val namespaces = createTracker(context, bundles)
            onSuccess?.accept(Pair(namespaces, configurationState))
        }
    }
    
    // Standard configuration
    
    /**
     * Create a new tracker instance which will be used inside the app to track events.
     * The app can run multiple tracker instances which will be identified by string `namespaces`.
     * The tracker will be configured with default setting and only the collector endpoint URL need
     * to be passed for the configuration.
     * For the default configuration of the tracker see [TrackerConfiguration]
     *
     * To configure tracker with more details see [.createTracker]
     * To use the tracker as singleton see [.getDefaultTracker]
     *
     * @apiNote IMPORTANT: The EventStore will persist all the events that have been tracked but not yet sent.
     * Those events are attached to the namespace.
     * If the tracker is removed or the app relaunched with a different namespace, those events can't
     * be sent to the collector and they remain in a zombie state inside the EventStore.
     * To remove all the zombie events you can an internal method [removeUnsentEventsExceptForNamespaces][EventStoreHelper]
     * which will delete all the EventStores instanced with namespaces not listed in the passed list.
     *
     * @param context The Android app context.
     * @param namespace The namespace used to identify the current tracker among the possible
     * multiple tracker instances.
     * @param endpoint The URL of the collector.
     * @param method The method for the requests to the collector (GET or POST).
     * @return The tracker instance created.
     */
    @JvmStatic
    fun createTracker(
        context: Context,
        namespace: String,
        endpoint: String,
        method: HttpMethod
    ): TrackerController {
        val network = NetworkConfiguration(endpoint, method)
        val tracker = TrackerConfiguration(context.packageName)
        return createTracker(context, namespace, network, tracker)
    }

    /**
     * Create a new tracker instance which will be used inside the app to track events.
     * The app can run multiple tracker instances which will be identified by string `namespaces`.
     * Each tracker can be configured by various configuration objects implementing the
     * [Configuration] interface.
     * For the default configuration of the tracker see [TrackerConfiguration.TrackerConfiguration]
     *
     * The configurations are only for the setup of the tracker and *any change to the configuration
     * object properties will NOT change the tracker setup at runtime*.
     *
     * To use the tracker as singleton see [.getDefaultTracker]
     *
     * @apiNote IMPORTANT: The EventStore will persist all the events that have been tracked but not yet sent.
     * Those events are attached to the namespace.
     * If the tracker is removed or the app relaunched with a different namespace, those events can't
     * be sent to the collector and they remain in a zombie state inside the EventStore.
     * To remove all the zombie events you can an internal method [removeUnsentEventsExceptForNamespaces][EventStoreHelper.removeUnsentEventsExceptForNamespaces]
     * which will delete all the EventStores instanced with namespaces not listed in the passed list.
     *
     * @param context The Android app context.
     * @param namespace The namespace used to identify the current tracker among the possible
     * multiple tracker instances.
     * @param network The NetworkConfiguration object with settings for the communication with the
     * collector.
     * @param configurations All the configuration objects with the details about the fine tuning of
     * the tracker.
     * @return The tracker instance created.
     */
    @JvmStatic
    fun createTracker(
        context: Context,
        namespace: String,
        network: NetworkConfiguration,
        vararg configurations: Configuration
    ): TrackerController {
        var serviceProvider = serviceProviderInstances[namespace]
        if (serviceProvider != null) {
            val configList: MutableList<Configuration> = ArrayList(listOf(*configurations))
            configList.add(network)
            serviceProvider.reset(configList)
        } else {
            serviceProvider =
                ServiceProvider(context, namespace, network, listOf(*configurations))
            registerInstance(serviceProvider)
        }
        return serviceProvider.orMakeTrackerController
    }

    /**
     * The default tracker instance is the first created in the app, but that can be overridden programmatically
     * calling [.setTrackerAsDefault].
     *
     * @return The default tracker instance or `null` whether the tracker has been removed or never initialized.
     */
    @JvmStatic
    val defaultTracker: TrackerController?
        get() {
            return defaultServiceProvider?.orMakeTrackerController
        }

    /**
     * Using the namespace identifier is possible to get the trackerController if already instanced.
     *
     * @param namespace The namespace that identifies the tracker.
     * @return The tracker if it exist with that namespace.
     */
    @JvmStatic
    @Synchronized
    fun getTracker(namespace: String): TrackerController? {
        val serviceProvider = serviceProviderInstances[namespace] ?: return null
        return serviceProvider.orMakeTrackerController
    }

    /**
     * Set the passed tracker as default tracker if it's registered as an active tracker in the app.
     * If the passed instance is of a tracker which is already removed (see [removeTracker][.removeTracker])
     * then it can't become the new default tracker and the operation fails.
     *
     * @param trackerController The new default tracker.
     * @return Whether the tracker passed is registered among the active trackers of the app.
     */
    @JvmStatic
    @Synchronized
    fun setTrackerAsDefault(trackerController: TrackerController): Boolean {
        val serviceProvider = serviceProviderInstances[trackerController.namespace]
        if (serviceProvider != null) {
            defaultServiceProvider = serviceProvider
            return true
        }
        return false
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
    @JvmStatic
    @Synchronized
    fun removeTracker(trackerController: TrackerController): Boolean {
        val namespace = trackerController.namespace
        val serviceProvider = serviceProviderInstances[namespace]
        if (serviceProvider != null) {
            serviceProvider.shutdown()
            serviceProviderInstances.remove(namespace)
            if (serviceProvider === defaultServiceProvider) {
                defaultServiceProvider = null
            }
            return true
        }
        return false
    }

    /**
     * Remove all the trackers.
     * The removed tracker is always stopped.
     * @see .removeTracker
     */
    @JvmStatic
    @Synchronized
    fun removeAllTrackers() {
        defaultServiceProvider = null
        val serviceProviders: Collection<ServiceProvider?> = serviceProviderInstances.values
        serviceProviderInstances.clear()
        for (sp in serviceProviders) {
            sp?.shutdown()
        }
    }

    /**
     * @return Set of namespace of the active trackers in the app.
     */
    @JvmStatic
    val instancedTrackerNamespaces: Set<String>
        get() = serviceProviderInstances.keys

    /**
     * Add a JavaScript interface to the Web view that listens for events tracked using 
     * the Snowplow library for Web views.
     * @param webView Web view instance in which to subscribe for events
     */
    @JvmStatic
    fun subscribeToWebViewEvents(webView: WebView) {
        webView.addJavascriptInterface(TrackerWebViewInterface(), TrackerWebViewInterface.TAG)
    }

    // Private methods
    
    @Synchronized
    private fun createTracker(context: Context, bundles: List<ConfigurationBundle>): List<String> {
        val namespaces: MutableList<String> = ArrayList()
        for (bundle in bundles) {
            if (bundle.networkConfiguration == null) {
                // remove tracker if it exists
                val tracker = getTracker(bundle.namespace)
                tracker?.let { removeTracker(tracker) }
            } else {
                val list = bundle.configurations
                val array = list.toTypedArray()
                createTracker(context, bundle.namespace, bundle.networkConfiguration!!, *array)
                namespaces.add(bundle.namespace)
            }
        }
        return namespaces
    }

    @Synchronized
    private fun registerInstance(serviceProvider: ServiceProvider): Boolean {
        val namespace = serviceProvider.namespace
        val isOverriding = serviceProviderInstances.put(namespace, serviceProvider) != null
        if (defaultServiceProvider == null) {
            defaultServiceProvider = serviceProvider
        }
        return isOverriding
    }
}
