/*
 * Copyright (c) 2015-2022 Snowplow Analytics Ltd. All rights reserved.
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

package com.snowplowanalytics.snowplow.internal.tracker;

import android.content.Context;
import android.net.NetworkInfo;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.internal.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.internal.utils.DeviceInfoMonitor;
import com.snowplowanalytics.snowplow.internal.utils.Util;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;

import java.util.HashMap;
import java.util.Map;

/**
 * PlatformContext manages device information that is sent as context along with events.
 * Some information is created during initialization and some ephemeral info is updated in predefined intervals as it is accessed.
 */
public class PlatformContext {
    private final Map<String, Object> pairs = new HashMap<>();
    private final @NonNull DeviceInfoMonitor deviceInfoMonitor;
    private final @NonNull Context context;
    private long lastUpdatedEphemeralPlatformDict;
    private long lastUpdatedEphemeralNetworkDict;
    private final long platformDictUpdateFrequency;
    private final long networkDictUpdateFrequency;

    /**
     * @param platformDictUpdateFrequency Minimal gap between subsequent updates of mobile platform information in milliseconds
     * @param networkDictUpdateFrequency Minimal gap between subsequent updates of network platform information in milliseconds
     * @param deviceInfoMonitor Device monitor for fetching platform information
     */
    PlatformContext(long platformDictUpdateFrequency, long networkDictUpdateFrequency, @NonNull DeviceInfoMonitor deviceInfoMonitor, @NonNull Context context) {
        this.platformDictUpdateFrequency = platformDictUpdateFrequency;
        this.networkDictUpdateFrequency = networkDictUpdateFrequency;
        this.deviceInfoMonitor = deviceInfoMonitor;
        this.context = context;

        setPlatformDict();
    }

    /**
     * Initializes PlatformContext with default update intervals – 0.1s for updating platform information and 10s for updating network information.
     *
     * @param context the Android context
     */
    PlatformContext(@NonNull Context context) {
        this(100, 10 * 1000, new DeviceInfoMonitor(), context);
    }

    @Nullable
    public SelfDescribingJson getMobileContext(boolean userAnonymisation) {
        updateEphemeralDictsIfNecessary();

        // If does not contain the required properties, return null
        if (!Util.mapHasKeys(pairs,
                Parameters.OS_TYPE,
                Parameters.OS_VERSION,
                Parameters.DEVICE_MANUFACTURER,
                Parameters.DEVICE_MODEL)) {
            return null;
        }

        // If user anonymisation is on, remove the IDFA value
        if (userAnonymisation && pairs.containsKey(Parameters.ANDROID_IDFA)) {
            Map<String, Object> copy = new HashMap<>(pairs);
            copy.remove(Parameters.ANDROID_IDFA);
            return new SelfDescribingJson(TrackerConstants.MOBILE_SCHEMA, copy);
        }

        return new SelfDescribingJson(TrackerConstants.MOBILE_SCHEMA, pairs);
    }

    // --- PRIVATE

    private synchronized void updateEphemeralDictsIfNecessary() {
        long now = System.currentTimeMillis();
        if (now - lastUpdatedEphemeralPlatformDict >= platformDictUpdateFrequency) {
            setEphemeralPlatformDict();
        }
        if (now - lastUpdatedEphemeralNetworkDict >= networkDictUpdateFrequency) {
            setEphemeralNetworkDict();
        }
    }

    private void setPlatformDict() {
        Util.addToMap(Parameters.OS_TYPE, deviceInfoMonitor.getOsType(), pairs);
        Util.addToMap(Parameters.OS_VERSION, deviceInfoMonitor.getOsVersion(), pairs);
        Util.addToMap(Parameters.DEVICE_MODEL, deviceInfoMonitor.getDeviceModel(), pairs);
        Util.addToMap(Parameters.DEVICE_MANUFACTURER, deviceInfoMonitor.getDeviceVendor(), pairs);
        Util.addToMap(Parameters.CARRIER, deviceInfoMonitor.getCarrier(context), pairs);
        Util.addToMap(Parameters.PHYSICAL_MEMORY, deviceInfoMonitor.getPhysicalMemory(context), pairs);
        Util.addToMap(Parameters.TOTAL_STORAGE, deviceInfoMonitor.getTotalStorage(), pairs);

        setEphemeralPlatformDict();
        setEphemeralNetworkDict();
    }

    private void setEphemeralPlatformDict() {
        lastUpdatedEphemeralPlatformDict = System.currentTimeMillis();

        Object currentIdfa = pairs.get(Parameters.ANDROID_IDFA);
        if (currentIdfa == null || currentIdfa.toString().isEmpty()) {
            Util.addToMap(Parameters.ANDROID_IDFA, deviceInfoMonitor.getAndroidIdfa(context), pairs);
        }

        Pair<String, Integer> batteryInfo = deviceInfoMonitor.getBatteryStateAndLevel(context);
        if (batteryInfo != null) {
            Util.addToMap(Parameters.BATTERY_STATE, batteryInfo.first, pairs);
            Util.addToMap(Parameters.BATTERY_LEVEL, batteryInfo.second, pairs);
        }
        Util.addToMap(Parameters.SYSTEM_AVAILABLE_MEMORY, deviceInfoMonitor.getSystemAvailableMemory(context), pairs);
        Util.addToMap(Parameters.AVAILABLE_STORAGE, deviceInfoMonitor.getAvailableStorage(), pairs);
    }

    private void setEphemeralNetworkDict() {
        lastUpdatedEphemeralNetworkDict = System.currentTimeMillis();

        NetworkInfo networkInfo = deviceInfoMonitor.getNetworkInfo(context);
        Util.addToMap(Parameters.NETWORK_TECHNOLOGY, deviceInfoMonitor.getNetworkTechnology(networkInfo), pairs);
        Util.addToMap(Parameters.NETWORK_TYPE, deviceInfoMonitor.getNetworkType(networkInfo), pairs);
    }

}
