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
package com.snowplowanalytics.snowplow.internal.tracker

import android.content.Context
import android.net.NetworkInfo
import android.util.Pair
import com.snowplowanalytics.core.utils.DeviceInfoMonitor

class MockDeviceInfoMonitor : DeviceInfoMonitor() {
    private val methodAccessCounts: MutableMap<String, Int> = HashMap()
    var customIdfa: String? = "XJKLJSALFKJ"
    override val osType: String
        get() {
            increaseMethodAccessCount("getOsType")
            return "Android"
        }
    override val osVersion: String
        get() {
            increaseMethodAccessCount("getOsVersion")
            return "13"
        }
    override val deviceModel: String
        get() {
            increaseMethodAccessCount("getDeviceModel")
            return "Nexus"
        }
    override val deviceVendor: String
        get() {
            increaseMethodAccessCount("getDeviceVendor")
            return "Google"
        }

    override fun getCarrier(context: Context): String {
        increaseMethodAccessCount("getCarrier")
        return "ATT"
    }

    override fun getAndroidIdfa(context: Context): String? {
        increaseMethodAccessCount("getAndroidIdfa")
        return customIdfa
    }

    override fun getNetworkType(networkInfo: NetworkInfo?): String {
        increaseMethodAccessCount("getNetworkType")
        return "wifi"
    }

    override fun getNetworkTechnology(networkInfo: NetworkInfo?): String? {
        increaseMethodAccessCount("getNetworkTechnology")
        return "3g"
    }

    override fun getPhysicalMemory(context: Context): Long {
        increaseMethodAccessCount("getPhysicalMemory")
        return 10000
    }

    override fun getSystemAvailableMemory(context: Context): Long {
        increaseMethodAccessCount("getSystemAvailableMemory")
        return 10
    }

    override fun getBatteryStateAndLevel(context: Context): Pair<String?, Int>? {
        increaseMethodAccessCount("getBatteryStateAndLevel")
        return Pair("charging", 20)
    }

    override val availableStorage: Long
        get() {
            increaseMethodAccessCount("getAvailableStorage")
            return 20000
        }
    override val totalStorage: Long
        get() {
            increaseMethodAccessCount("getTotalStorage")
            return 70000
        }

    private var _language: String? = "sk"
    override var language: String?
        get() {
            increaseMethodAccessCount("language")
            return _language
        }
        set(value) {
            _language = value
        }

    override fun getResolution(context: Context): String? {
        increaseMethodAccessCount("getResolution")
        return "1024x768"
    }

    override fun getScale(context: Context): Float? {
        increaseMethodAccessCount("getScale")
        return 2.0f
    }

    override fun getIsPortrait(context: Context): Boolean? {
        increaseMethodAccessCount("getIsPortrait")
        return true
    }

    override fun getAppSetIdAndScope(context: Context): Pair<String, String>? {
        increaseMethodAccessCount("getAppSetIdAndScope")
        return Pair("XXX", "app")
    }

    fun getMethodAccessCount(methodName: String): Int {
        return if (methodAccessCounts.containsKey(methodName)) {
            methodAccessCounts[methodName]!!
        } else {
            0
        }
    }

    private fun increaseMethodAccessCount(methodName: String) {
        methodAccessCounts[methodName] = getMethodAccessCount(methodName) + 1
    }
}
