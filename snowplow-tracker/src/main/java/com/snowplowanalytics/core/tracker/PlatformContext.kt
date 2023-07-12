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

/**
 * PlatformContext manages device information that is sent as context along with events.
 * Some information is created during initialization and some ephemeral info is updated in predefined intervals as it is accessed.
 * 
 * @param platformDictUpdateFrequency Minimal gap between subsequent updates of mobile platform information in milliseconds
 * @param networkDictUpdateFrequency Minimal gap between subsequent updates of network platform information in milliseconds
 * @param deviceInfoMonitor Device monitor for fetching platform information
 */
class PlatformContext(
    private val platformDictUpdateFrequency: Long,
    private val networkDictUpdateFrequency: Long,
    private val deviceInfoMonitor: DeviceInfoMonitor,
    private val platformContextProperties: List<PlatformContextProperty>?,
    private val context: Context
) {
    private val pairs: MutableMap<String, Any> = HashMap()
    private var lastUpdatedEphemeralPlatformDict: Long = 0
    private var lastUpdatedEphemeralNetworkDict: Long = 0
    
    init {
        setPlatformDict()
    }

    /**
     * Initializes PlatformContext with default update intervals – 1s for updating platform information and 10s for updating network information.
     *
     * @param context the Android context
     */
    constructor(platformContextProperties: List<PlatformContextProperty>?,
                context: Context) : this(1000, (10 * 1000).toLong(), DeviceInfoMonitor(), platformContextProperties, context)

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
        addToMap(Parameters.OS_TYPE, deviceInfoMonitor.osType, pairs)
        addToMap(Parameters.OS_VERSION, deviceInfoMonitor.osVersion, pairs)
        addToMap(Parameters.DEVICE_MODEL, deviceInfoMonitor.deviceModel, pairs)
        addToMap(Parameters.DEVICE_MANUFACTURER, deviceInfoMonitor.deviceVendor, pairs)
        // Carrier
        if (shouldTrack(PlatformContextProperty.CARRIER)) {
            addToMap(
                Parameters.CARRIER, deviceInfoMonitor.getCarrier(
                    context
                ), pairs
            )
        }
        // Physical memory
        if (shouldTrack(PlatformContextProperty.PHYSICAL_MEMORY)) {
            addToMap(
                Parameters.PHYSICAL_MEMORY, deviceInfoMonitor.getPhysicalMemory(
                    context
                ), pairs
            )
        }
        // Total storage
        if (shouldTrack(PlatformContextProperty.TOTAL_STORAGE)) {
            addToMap(Parameters.TOTAL_STORAGE, deviceInfoMonitor.totalStorage, pairs)
        }
        // Resolution
        if (shouldTrack(PlatformContextProperty.RESOLUTION)) {
            addToMap(Parameters.MOBILE_RESOLUTION, deviceInfoMonitor.getResolution(context), pairs)
        }
        // Scale
        if (shouldTrack(PlatformContextProperty.SCALE)) {
            addToMap(Parameters.MOBILE_SCALE, deviceInfoMonitor.getScale(context), pairs)
        }
        // Language
        if (shouldTrack(PlatformContextProperty.LANGUAGE)) {
            addToMap(Parameters.MOBILE_LANGUAGE, deviceInfoMonitor.language?.take(8), pairs)
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
                    Parameters.ANDROID_IDFA, deviceInfoMonitor.getAndroidIdfa(
                        context
                    ), pairs
                )
            }
        }
        // Battery
        val trackBatState = shouldTrack(PlatformContextProperty.BATTERY_STATE)
        val trackBatLevel = shouldTrack(PlatformContextProperty.BATTERY_LEVEL)
        if (trackBatState || trackBatLevel) {
            val batteryInfo = deviceInfoMonitor.getBatteryStateAndLevel(
                context
            )
            if (batteryInfo != null) {
                if (trackBatState) { addToMap(Parameters.BATTERY_STATE, batteryInfo.first, pairs) }
                if (trackBatLevel) { addToMap(Parameters.BATTERY_LEVEL, batteryInfo.second, pairs) }
            }
        }
        // Memory
        if (shouldTrack(PlatformContextProperty.SYSTEM_AVAILABLE_MEMORY)) {
            addToMap(
                Parameters.SYSTEM_AVAILABLE_MEMORY, deviceInfoMonitor.getSystemAvailableMemory(
                    context
                ), pairs
            )
        }
        // Storage
        if (shouldTrack(PlatformContextProperty.AVAILABLE_STORAGE)) {
            addToMap(Parameters.AVAILABLE_STORAGE, deviceInfoMonitor.availableStorage, pairs)
        }
        // Is portrait
        if (shouldTrack(PlatformContextProperty.IS_PORTRAIT)) {
            addToMap(Parameters.IS_PORTRAIT, deviceInfoMonitor.getIsPortrait(context), pairs)
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
                Parameters.NETWORK_TECHNOLOGY,
                deviceInfoMonitor.getNetworkTechnology(networkInfo),
                pairs
            )
        }
        if (trackTech) {
            addToMap(
                Parameters.NETWORK_TYPE,
                deviceInfoMonitor.getNetworkType(networkInfo),
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
        val appSetId = generalPref.getString(Parameters.APP_SET_ID, null)
        val appSetIdScope = generalPref.getString(Parameters.APP_SET_ID_SCOPE, null)

        if (appSetId != null && appSetIdScope != null) {
            if (trackId) { addToMap(Parameters.APP_SET_ID, appSetId, pairs) }
            if (trackScope) { addToMap(Parameters.APP_SET_ID_SCOPE, appSetIdScope, pairs) }
        } else {
            Executor.execute(TAG) {
                deviceInfoMonitor.getAppSetIdAndScope(context)?.let {
                    if (trackId) { addToMap(Parameters.APP_SET_ID, it.first, pairs) }
                    if (trackScope) { addToMap(Parameters.APP_SET_ID_SCOPE, it.second, pairs) }

                    generalPref.edit()
                        .putString(Parameters.APP_SET_ID, it.first)
                        .putString(Parameters.APP_SET_ID_SCOPE, it.second)
                        .apply()
                }
            }
        }
    }

    private fun shouldTrack(property: PlatformContextProperty): Boolean {
        return platformContextProperties?.contains(property) ?: true
    }

    companion object {
        private val TAG = PlatformContext::class.java.simpleName
    }
}
