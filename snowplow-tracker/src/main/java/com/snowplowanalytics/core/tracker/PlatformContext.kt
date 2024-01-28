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
package com.snowplowanalytics.core.tracker

import android.content.Context
import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.core.emitter.Executor
import com.snowplowanalytics.core.utils.DeviceInfoMonitor
import com.snowplowanalytics.core.utils.Util.addToMap
import com.snowplowanalytics.core.utils.Util.mapHasKeys
import com.snowplowanalytics.snowplow.configuration.PlatformContextProperty
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow.tracker.PlatformContextRetriever

/**
 * PlatformContext manages device information that is sent as context along with events.
 * Some information is created during initialization and some ephemeral info is updated in predefined intervals as it is accessed.
 * 
 * @param platformDictUpdateFrequency Minimal gap between subsequent updates of mobile platform information in milliseconds
 * @param networkDictUpdateFrequency Minimal gap between subsequent updates of network platform information in milliseconds
 * @param deviceInfoMonitor Device monitor for fetching platform information
 * @param properties List of properties of the platform context to track
 * @param retriever Overrides for retrieving property values
 */
class PlatformContext(
    private val platformDictUpdateFrequency: Long = 1000,
    private val networkDictUpdateFrequency: Long = (10 * 1000).toLong(),
    private val deviceInfoMonitor: DeviceInfoMonitor = DeviceInfoMonitor(),
    private val properties: List<PlatformContextProperty>? = null,
    private val retriever: PlatformContextRetriever = PlatformContextRetriever(),
    private val context: Context,
) {
    private val pairs: MutableMap<String, Any> = HashMap()
    private var lastUpdatedEphemeralPlatformDict: Long = 0
    private var lastUpdatedEphemeralNetworkDict: Long = 0
    
    init {
        setPlatformDict()
    }

    fun getMobileContext(userAnonymisation: Boolean): SelfDescribingJson? {
        updateEphemeralDictsIfNecessary()

        // If does not contain the required properties, return null
        if (!mapHasKeys(
                pairs,
                Parameters.OS_TYPE,
                Parameters.OS_VERSION,
                Parameters.DEVICE_MANUFACTURER,
                Parameters.DEVICE_MODEL
            )
        ) {
            return null
        }

        // If user anonymisation is on, remove the IDFA value
        if (userAnonymisation && pairs.containsKey(Parameters.ANDROID_IDFA)) {
            val copy: MutableMap<String, Any> = HashMap(pairs)
            copy.remove(Parameters.ANDROID_IDFA)
            return SelfDescribingJson(TrackerConstants.MOBILE_SCHEMA, copy)
        }
        return SelfDescribingJson(TrackerConstants.MOBILE_SCHEMA, pairs)
    }

    // --- PRIVATE
    @Synchronized
    private fun updateEphemeralDictsIfNecessary() {
        val now = System.currentTimeMillis()
        if (now - lastUpdatedEphemeralPlatformDict >= platformDictUpdateFrequency) {
            setEphemeralPlatformDict()
        }
        if (now - lastUpdatedEphemeralNetworkDict >= networkDictUpdateFrequency) {
            setEphemeralNetworkDict()
        }
    }

    private fun setPlatformDict() {
        addToMap(Parameters.OS_TYPE, fromRetrieverOr(retriever.osType) { deviceInfoMonitor.osType }, pairs)
        addToMap(Parameters.OS_VERSION, fromRetrieverOr(retriever.osVersion) { deviceInfoMonitor.osVersion }, pairs)
        addToMap(Parameters.DEVICE_MODEL, fromRetrieverOr(retriever.deviceModel) { deviceInfoMonitor.deviceModel }, pairs)
        addToMap(Parameters.DEVICE_MANUFACTURER, fromRetrieverOr(retriever.deviceVendor) { deviceInfoMonitor.deviceVendor }, pairs)
        // Carrier
        if (shouldTrack(PlatformContextProperty.CARRIER)) {
            addToMap(
                Parameters.CARRIER, fromRetrieverOr(retriever.carrier) { deviceInfoMonitor.getCarrier(context) },
                pairs
            )
        }
        // Physical memory
        if (shouldTrack(PlatformContextProperty.PHYSICAL_MEMORY)) {
            addToMap(
                Parameters.PHYSICAL_MEMORY,
                fromRetrieverOr(retriever.physicalMemory) { deviceInfoMonitor.getPhysicalMemory(context) },
                pairs
            )
        }
        // Total storage
        if (shouldTrack(PlatformContextProperty.TOTAL_STORAGE)) {
            addToMap(Parameters.TOTAL_STORAGE, fromRetrieverOr(retriever.totalStorage) { deviceInfoMonitor.totalStorage }, pairs)
        }
        // Resolution
        if (shouldTrack(PlatformContextProperty.RESOLUTION)) {
            addToMap(Parameters.MOBILE_RESOLUTION, fromRetrieverOr(retriever.resolution) { deviceInfoMonitor.getResolution(context) }, pairs)
        }
        // Scale
        if (shouldTrack(PlatformContextProperty.SCALE)) {
            addToMap(Parameters.MOBILE_SCALE, fromRetrieverOr(retriever.scale) { deviceInfoMonitor.getScale(context) }, pairs)
        }
        // Language
        if (shouldTrack(PlatformContextProperty.LANGUAGE)) {
            addToMap(Parameters.MOBILE_LANGUAGE, (fromRetrieverOr(retriever.language) { deviceInfoMonitor.language })?.take(8), pairs)
        }

        setEphemeralPlatformDict()
        setEphemeralNetworkDict()
        setAppSetId()
    }

    private fun setEphemeralPlatformDict() {
        lastUpdatedEphemeralPlatformDict = System.currentTimeMillis()

        // IDFA
        if (shouldTrack(PlatformContextProperty.ANDROID_IDFA)) {
            val currentIdfa = pairs[Parameters.ANDROID_IDFA]
            if (currentIdfa == null || currentIdfa.toString().isEmpty()) {
                addToMap(
                    Parameters.ANDROID_IDFA,
                    fromRetrieverOr(retriever.androidIdfa) { deviceInfoMonitor.getAndroidIdfa(context) },
                    pairs
                )
            }
        }
        // Battery
        val trackBatState = shouldTrack(PlatformContextProperty.BATTERY_STATE)
        val trackBatLevel = shouldTrack(PlatformContextProperty.BATTERY_LEVEL)
        if (trackBatState || trackBatLevel) {
            val batteryInfo = deviceInfoMonitor.getBatteryStateAndLevel(context)
            if (trackBatState) { addToMap(Parameters.BATTERY_STATE, fromRetrieverOr(retriever.batteryState) { batteryInfo?.first }, pairs) }
            if (trackBatLevel) { addToMap(Parameters.BATTERY_LEVEL, fromRetrieverOr(retriever.batteryLevel) { batteryInfo?.second }, pairs) }
        }
        // Memory
        if (shouldTrack(PlatformContextProperty.SYSTEM_AVAILABLE_MEMORY)) {
            addToMap(
                Parameters.SYSTEM_AVAILABLE_MEMORY,
                fromRetrieverOr(retriever.systemAvailableMemory) { deviceInfoMonitor.getSystemAvailableMemory(context) },
                pairs
            )
        }
        // Storage
        if (shouldTrack(PlatformContextProperty.AVAILABLE_STORAGE)) {
            addToMap(Parameters.AVAILABLE_STORAGE, fromRetrieverOr(retriever.availableStorage) { deviceInfoMonitor.availableStorage }, pairs)
        }
        // Is portrait
        if (shouldTrack(PlatformContextProperty.IS_PORTRAIT)) {
            addToMap(Parameters.IS_PORTRAIT, fromRetrieverOr(retriever.isPortrait) { deviceInfoMonitor.getIsPortrait(context) }, pairs)
        }
    }

    private fun setEphemeralNetworkDict() {
        lastUpdatedEphemeralNetworkDict = System.currentTimeMillis()

        val trackType = shouldTrack(PlatformContextProperty.NETWORK_TYPE)
        val trackTech = shouldTrack(PlatformContextProperty.NETWORK_TECHNOLOGY)
        if (!trackType && !trackTech) { return }

        val networkInfo = deviceInfoMonitor.getNetworkInfo(context)
        if (trackType) {
            addToMap(
                Parameters.NETWORK_TYPE,
                fromRetrieverOr(retriever.networkType) { deviceInfoMonitor.getNetworkType(networkInfo) },
                pairs
            )
        }
        if (trackTech) {
            addToMap(
                Parameters.NETWORK_TECHNOLOGY,
                fromRetrieverOr(retriever.networkTechnology) { deviceInfoMonitor.getNetworkTechnology(networkInfo) },
                pairs
            )
        }
    }

    /**
     * Sets the app set information.
     * The info has to be read on a background thread which often means that the first few
     * tracked events will miss the info. To prevent that happening on the second start-up
     * of the app, the info is saved in general prefs and read from there.
     */
    private fun setAppSetId() {
        val trackId = shouldTrack(PlatformContextProperty.APP_SET_ID)
        val trackScope = shouldTrack(PlatformContextProperty.APP_SET_ID_SCOPE)
        if (!trackId && !trackScope) { return }

        val generalPref = context.getSharedPreferences(
            TrackerConstants.SNOWPLOW_GENERAL_VARS,
            Context.MODE_PRIVATE
        )
        val appSetId = fromRetrieverOr(retriever.appSetId) { generalPref.getString(Parameters.APP_SET_ID, null) }
        val appSetIdScope = fromRetrieverOr(retriever.appSetIdScope) { generalPref.getString(Parameters.APP_SET_ID_SCOPE, null) }

        if (appSetId != null && appSetIdScope != null) {
            if (trackId) { addToMap(Parameters.APP_SET_ID, appSetId, pairs) }
            if (trackScope) { addToMap(Parameters.APP_SET_ID_SCOPE, appSetIdScope, pairs) }
        } else {
            Executor.execute(TAG) {
                val preferences = generalPref.edit()
                var edited = false

                val appSetIdAndScope = deviceInfoMonitor.getAppSetIdAndScope(context)
                val id = fromRetrieverOr(retriever.appSetId) {
                    val id = appSetIdAndScope?.first
                    preferences.putString(Parameters.APP_SET_ID, id)
                    edited = true
                    id
                }
                val scope = fromRetrieverOr(retriever.appSetIdScope) {
                    val scope = appSetIdAndScope?.second
                    preferences.putString(Parameters.APP_SET_ID_SCOPE, scope)
                    edited = true
                    scope
                }

                if (trackId) { addToMap(Parameters.APP_SET_ID, id, pairs) }
                if (trackScope) { addToMap(Parameters.APP_SET_ID_SCOPE, scope, pairs) }

                if (edited) { preferences.apply() }
            }
        }
    }

    private fun shouldTrack(property: PlatformContextProperty): Boolean {
        return properties?.contains(property) ?: true
    }

    private fun <T> fromRetrieverOr(f1: (() -> T)?, f2: () -> T): T {
        return if (f1 == null) { f2() } else { f1.invoke() }
    }

    companion object {
        private val TAG = PlatformContext::class.java.simpleName
    }
}
