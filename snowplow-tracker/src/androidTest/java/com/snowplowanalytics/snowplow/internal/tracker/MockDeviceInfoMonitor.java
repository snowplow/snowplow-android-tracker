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

import com.snowplowanalytics.snowplow.internal.utils.DeviceInfoMonitor;

import java.util.HashMap;
import java.util.Map;

public class MockDeviceInfoMonitor extends DeviceInfoMonitor {
    @NonNull private Map<String, Integer> methodAccessCounts = new HashMap<String, Integer>();
    @Nullable public String customIdfa = "XJKLJSALFKJ";

    @NonNull
    @Override
    public String getOsType() {
        increaseMethodAccessCount("getOsType");
        return "Android";
    }

    @NonNull
    @Override
    public String getOsVersion() {
        increaseMethodAccessCount("getOsVersion");
        return "13";
    }

    @NonNull
    @Override
    public String getDeviceModel() {
        increaseMethodAccessCount("getDeviceModel");
        return "Nexus";
    }

    @NonNull
    @Override
    public String getDeviceVendor() {
        increaseMethodAccessCount("getDeviceVendor");
        return "Google";
    }

    @Nullable
    @Override
    public String getCarrier(@NonNull Context context) {
        increaseMethodAccessCount("getCarrier");
        return "ATT";
    }

    @Nullable
    @Override
    public String getAndroidIdfa(@NonNull Context context) {
        increaseMethodAccessCount("getAndroidIdfa");
        return customIdfa;
    }

    @NonNull
    @Override
    public String getNetworkType(@Nullable NetworkInfo networkInfo) {
        increaseMethodAccessCount("getNetworkType");
        return "wifi";
    }

    @Nullable
    @Override
    public String getNetworkTechnology(@Nullable NetworkInfo networkInfo) {
        increaseMethodAccessCount("getNetworkTechnology");
        return "3g";
    }

    @Override
    public long getPhysicalMemory(@NonNull Context context) {
        increaseMethodAccessCount("getPhysicalMemory");
        return 10000;
    }

    @Override
    public long getSystemAvailableMemory(@NonNull Context context) {
        increaseMethodAccessCount("getSystemAvailableMemory");
        return 10;
    }

    @Override
    public Pair<String, Integer> getBatteryStateAndLevel(@NonNull Context context) {
        increaseMethodAccessCount("getBatteryStateAndLevel");
        return new Pair<>("charging", 20);
    }

    @Override
    public long getAvailableStorage() {
        increaseMethodAccessCount("getAvailableStorage");
        return 20000;
    }

    @Override
    public long getTotalStorage() {
        increaseMethodAccessCount("getTotalStorage");
        return 70000;
    }

    public int getMethodAccessCount(String methodName) {
        if (methodAccessCounts.containsKey(methodName)) {
            return methodAccessCounts.get(methodName);
        } else {
            return 0;
        }
    }

    private void increaseMethodAccessCount(String methodName) {
        methodAccessCounts.put(methodName, getMethodAccessCount(methodName) + 1);
    }

}
