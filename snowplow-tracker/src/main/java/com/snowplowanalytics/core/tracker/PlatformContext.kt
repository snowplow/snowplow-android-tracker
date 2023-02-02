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
import com.snowplowanalytics.core.utils.DeviceInfoMonitor
import com.snowplowanalytics.core.utils.Util.addToMap
import com.snowplowanalytics.core.utils.Util.mapHasKeys
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
    private val context: Context
) {
    private val pairs: MutableMap<String, Any> = HashMap()
    private var lastUpdatedEphemeralPlatformDict: Long = 0
    private var lastUpdatedEphemeralNetworkDict: Long = 0
    
    init {
        setPlatformDict()
    }

    /**
     * Initializes PlatformContext with default update intervals – 0.1s for updating platform information and 10s for updating network information.
     *
     * @param context the Android context
     */
    constructor(context: Context) : this(100, (10 * 1000).toLong(), DeviceInfoMonitor(), context)

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
        addToMap(
            Parameters.CARRIER, deviceInfoMonitor.getCarrier(
                context
            ), pairs
        )
        addToMap(
            Parameters.PHYSICAL_MEMORY, deviceInfoMonitor.getPhysicalMemory(
                context
            ), pairs
        )
        addToMap(Parameters.TOTAL_STORAGE, deviceInfoMonitor.totalStorage, pairs)
        setEphemeralPlatformDict()
        setEphemeralNetworkDict()
    }

    private fun setEphemeralPlatformDict() {
        lastUpdatedEphemeralPlatformDict = System.currentTimeMillis()
        val currentIdfa = pairs[Parameters.ANDROID_IDFA]
        if (currentIdfa == null || currentIdfa.toString().isEmpty()) {
            addToMap(
                Parameters.ANDROID_IDFA, deviceInfoMonitor.getAndroidIdfa(
                    context
                ), pairs
            )
        }
        val batteryInfo = deviceInfoMonitor.getBatteryStateAndLevel(
            context
        )
        if (batteryInfo != null) {
            addToMap(Parameters.BATTERY_STATE, batteryInfo.first, pairs)
            addToMap(Parameters.BATTERY_LEVEL, batteryInfo.second, pairs)
        }
        addToMap(
            Parameters.SYSTEM_AVAILABLE_MEMORY, deviceInfoMonitor.getSystemAvailableMemory(
                context
            ), pairs
        )
        addToMap(Parameters.AVAILABLE_STORAGE, deviceInfoMonitor.availableStorage, pairs)
    }

    private fun setEphemeralNetworkDict() {
        lastUpdatedEphemeralNetworkDict = System.currentTimeMillis()
        val networkInfo = deviceInfoMonitor.getNetworkInfo(context)
        addToMap(
            Parameters.NETWORK_TECHNOLOGY,
            deviceInfoMonitor.getNetworkTechnology(networkInfo),
            pairs
        )
        addToMap(Parameters.NETWORK_TYPE, deviceInfoMonitor.getNetworkType(networkInfo), pairs)
    }
}
