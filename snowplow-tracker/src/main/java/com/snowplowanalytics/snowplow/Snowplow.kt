/*
 * Copyright (c) 2015-2023 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.snowplowanalytics.snowplow

import android.content.Context
import android.webkit.WebView
import androidx.core.util.Consumer
import androidx.core.util.Pair

import com.snowplowanalytics.core.remoteconfiguration.ConfigurationProvider
import com.snowplowanalytics.core.remoteconfiguration.FetchedConfigurationBundle
import com.snowplowanalytics.core.tracker.ServiceProvider
import com.snowplowanalytics.core.tracker.TrackerWebViewInterface
import com.snowplowanalytics.snowplow.configuration.*

import com.snowplowanalytics.snowplow.controller.TrackerController
import com.snowplowanalytics.snowplow.network.HttpMethod

import java.util.*

/**
 * Instance a new Snowplow tracker for local or remote configuration, and manage multiple trackers.
 */
object Snowplow {
    // Private properties
    private var defaultServiceProvider: ServiceProvider? = null
    private val serviceProviderInstances: MutableMap<String, ServiceProvider?> = HashMap()
    private var configurationProvider: ConfigurationProvider? = null

    /**
     * The default tracker instance is the first created in the app, but that can be overridden programmatically
     * using [Snowplow.setTrackerAsDefault].
     *
     * @return The default tracker instance, or `null` when the tracker has been removed or never initialized.
     */
    @JvmStatic
    val defaultTracker: TrackerController?
        get() {
            return defaultServiceProvider?.getOrMakeTrackerController()
        }

    /**
     * @return Set of namespace of the active trackers in the app.
     */
    @JvmStatic
    val instancedTrackerNamespaces: Set<String>
        get() = serviceProviderInstances.keys
    
    // Remote configuration
    
    /**
     * Set up a single or a set of tracker instances which will be used inside the app to track events.
     * The app can run multiple tracker instances which will be identified by string `namespaces`.
     * The trackers configuration is automatically downloaded from the endpoint indicated in the [RemoteConfiguration]
     * passed as argument. For more details see [RemoteConfiguration].
     *
     * The method is asynchronous and you can receive the list of the created trackers in the callbacks once the trackers are created.
     * The callback can be called multiple times in case a cached configuration is ready and later a fetched configuration is available.
     * You can also pass as argument a default configuration in case there isn't a cached configuration and it's not able to download
     * a new one. The downloaded configuration updates the cached one only if the configuration version is greater than the cached one.
     * Otherwise the cached one is kept and the callback is not called.
     *
     * IMPORTANT: The [SQLiteEventStore](com.snowplowanalytics.core.emitter.storage.SQLiteEventStore) will persist all the events that have been tracked but not yet sent.
     * Those events are attached to the namespace.
     * If the tracker is removed or the app relaunched with a different namespace, those events can't
     * be sent to the collector and they remain in a zombie state inside the EventStore.
     * To remove all the zombie events you can use the internal method 
     * [.removeUnsentEventsExceptForNamespaces](com.snowplowanalytics.core.emitter.storage.SQLiteEventStore.removeUnsentEventsExceptForNamespaces)
     * in [SQLiteEventStore](com.snowplowanalytics.core.emitter.storage.SQLiteEventStore)
     * which will delete all the EventStores instanced with namespaces not listed in the passed list.
     *
     * @param context The Android app context.
     * @param remoteConfiguration The remote configuration used to indicate where to download the configuration from.
     * @param defaultBundles The default configuration passed by default in case there isn't a cached version and it's not able to download a new one.
     * @param onSuccess The callback called when a configuration (cached or downloaded) is set.
     * It passes a pair object with the list of the namespaces associated
     * to the created trackers and the state of the configuration – whether it was
     * retrieved from cache or fetched over the network.
     */
    @JvmStatic
    fun setup(
        context: Context,
        remoteConfiguration: RemoteConfiguration,
        defaultBundles: List<ConfigurationBundle>?,
        onSuccess: Consumer<Pair<List<String>, ConfigurationState?>?>
    ) {
        configurationProvider = ConfigurationProvider(remoteConfiguration, defaultBundles)
        configurationProvider?.retrieveConfiguration(
            context,
            false
        ) { fetchedConfigurationPair: Pair<FetchedConfigurationBundle, ConfigurationState> ->
            val fetchedConfigurationBundle = fetchedConfigurationPair.first
            val configurationState = fetchedConfigurationPair.second
            val bundles = fetchedConfigurationBundle.configurationBundle
            val namespaces = createTracker(context, bundles)
            onSuccess.accept(Pair(namespaces, configurationState))
        }
    }

    /**
     * Reconfigure, create or delete the trackers based on the configuration downloaded remotely.
     * The trackers configuration is automatically downloaded from the endpoint indicated in the [RemoteConfiguration]
     * previously used to set up the trackers.
     *
     * The method is asynchronous and you can receive the list of the created trackers in the callbacks once the trackers are created.
     * The downloaded configuration updates the cached one only if the configuration version is greater than the cached one.
     * Otherwise the cached one is kept and the callback is not called.
     *
     * IMPORTANT: The [SQLiteEventStore](com.snowplowanalytics.core.emitter.storage.SQLiteEventStore) will persist all the events that have been tracked but not yet sent.
     * Those events are attached to the namespace.
     * If the tracker is removed or the app relaunched with a different namespace, those events can't
     * be sent to the collector and they remain in a zombie state inside the EventStore.
     * To remove all the zombie events you can use the internal method
     * [.removeUnsentEventsExceptForNamespaces](com.snowplowanalytics.core.emitter.storage.SQLiteEventStore.removeUnsentEventsExceptForNamespaces)
     * in [SQLiteEventStore](com.snowplowanalytics.core.emitter.storage.SQLiteEventStore)
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
        configurationProvider?.let { it.retrieveConfiguration(
            context,
            true
        ) { fetchedConfigurationPair: Pair<FetchedConfigurationBundle, ConfigurationState> ->
            val fetchedConfigurationBundle = fetchedConfigurationPair.first
            val configurationState = fetchedConfigurationPair.second
            val bundles = fetchedConfigurationBundle.configurationBundle
            val namespaces = createTracker(context, bundles)
            onSuccess?.accept(Pair(namespaces, configurationState))
        } }
    }
    
    // Standard configuration
    
    /**
     * Create a new tracker instance which will be used inside the app to track events.
     * The app can run multiple tracker instances which will be identified by string `namespaces`.
     * The tracker will be configured with default settings and only the collector endpoint URL needs
     * to be passed for the configuration.
     * By default, the tracker will send events by POST; pass [HttpMethod.GET] here to change that.
     * For the default configuration of the tracker see [TrackerConfiguration]
     *
     * To use the tracker as singleton see [Snowplow.defaultTracker]
     *
     * IMPORTANT: The [SQLiteEventStore](com.snowplowanalytics.core.emitter.storage.SQLiteEventStore) will persist all the events that have been tracked but not yet sent.
     * Those events are attached to the namespace.
     * If the tracker is removed or the app relaunched with a different namespace, those events can't
     * be sent to the collector and they remain in a zombie state inside the EventStore.
     * To remove all the zombie events you can use the internal method
     * [.removeUnsentEventsExceptForNamespaces](com.snowplowanalytics.core.emitter.storage.SQLiteEventStore.removeUnsentEventsExceptForNamespaces)
     * in [SQLiteEventStore](com.snowplowanalytics.core.emitter.storage.SQLiteEventStore)
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
        method: HttpMethod = HttpMethod.POST
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
     * For the default configuration of the tracker see [TrackerConfiguration]
     *
     * The configurations are only for the setting up of the tracker and **any change to the configuration
     * object properties will NOT change the tracker setup at runtime**.
     *
     * To use the tracker as singleton see [Snowplow.defaultTracker]
     *
     * IMPORTANT: The [SQLiteEventStore](com.snowplowanalytics.core.emitter.storage.SQLiteEventStore) will persist all the events that have been tracked but not yet sent.
     * Those events are attached to the namespace.
     * If the tracker is removed or the app relaunched with a different namespace, those events can't
     * be sent to the collector and they remain in a zombie state inside the EventStore.
     * To remove all the zombie events you can use the internal method
     * [.removeUnsentEventsExceptForNamespaces](com.snowplowanalytics.core.emitter.storage.SQLiteEventStore.removeUnsentEventsExceptForNamespaces)
     * in [SQLiteEventStore](com.snowplowanalytics.core.emitter.storage.SQLiteEventStore)
     * which will delete all the EventStores instanced with namespaces not listed in the passed list.
     *
     * @param context The Android app context.
     * @param namespace The namespace used to identify the current tracker among the possible
     * multiple tracker instances.
     * @param network The NetworkConfiguration object with settings for the communication with the
     * collector.
     * @param configurations All the other configuration objects with the details about the fine tuning of
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
        return serviceProvider.getOrMakeTrackerController()
    }

    /**
     * Using the namespace identifier it is possible to get the tracker if already instanced.
     *
     * @param namespace The namespace that identifies the tracker.
     * @return The tracker if one exists with that namespace.
     */
    @JvmStatic
    @Synchronized
    fun getTracker(namespace: String): TrackerController? {
        val serviceProvider = serviceProviderInstances[namespace] ?: return null
        return serviceProvider.getOrMakeTrackerController()
    }

    /**
     * Set the passed tracker as default. The tracker must be active: one that has been removed 
     * (see [Snowplow.removeTracker]) cannot become the new default tracker, and the operation fails.
     *
     * @param trackerController The tracker to use as the new default.
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
     * The removed tracker is always stopped, and cannot be added again or set as default.
     * To "resume" a removed tracker, create a new tracker with the same namespace and configuration.
     *
     * @param trackerController The tracker to remove.
     * @return Whether it was possible to remove it.
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
     * Remove all the trackers. A removed tracker is always stopped.
     * @see [Snowplow.removeTracker]
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
     * Add a JavaScript interface to the WebView that listens for events tracked using 
     * the [Snowplow library for WebViews](https://docs.snowplow.io/docs/collecting-data/collecting-from-own-applications/webview-tracker/).
     * @param webView WebView instance to subscribe to for events
     */
    @JvmStatic
    fun subscribeToWebViewEvents(webView: WebView) {
        webView.addJavascriptInterface(TrackerWebViewInterface(), TrackerWebViewInterface.TAG)
    }

    // Private methods

    @JvmStatic
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
                bundle.networkConfiguration?.let { createTracker(context, bundle.namespace, it, *array) }
                namespaces.add(bundle.namespace)
            }
        }
        return namespaces
    }

    @JvmStatic
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
