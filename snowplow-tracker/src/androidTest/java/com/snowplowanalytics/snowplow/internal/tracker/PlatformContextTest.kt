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
package com.snowplowanalytics.snowplow.internal.tracker

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.core.tracker.PlatformContext
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlatformContextTest {
    // --- TESTS
    @Test
    fun addsNotMockedMobileContext() {
        val platformContext = PlatformContext(context)
        val sdj = platformContext.getMobileContext(false)
        Assert.assertNotNull(sdj)
        val sdjMap = sdj!!.map
        Assert.assertEquals(sdjMap["schema"] as String?, TrackerConstants.MOBILE_SCHEMA)
        val sdjData = sdjMap["data"] as Map<*, *>?
        Assert.assertEquals(sdjData!![Parameters.OS_TYPE] as String?, "android")
        Assert.assertTrue(sdjData.containsKey(Parameters.OS_VERSION))
        Assert.assertTrue(sdjData.containsKey(Parameters.DEVICE_MODEL))
        Assert.assertTrue(sdjData.containsKey(Parameters.DEVICE_MANUFACTURER))
        Assert.assertTrue(sdjData.containsKey(Parameters.CARRIER))
        Assert.assertTrue(sdjData.containsKey(Parameters.NETWORK_TYPE))
    }

    @Test
    fun addsAllMockedInfo() {
        val deviceInfoMonitor = MockDeviceInfoMonitor()
        val platformContext = PlatformContext(0, 0, deviceInfoMonitor, context)
        val sdj = platformContext.getMobileContext(false)
        val sdjMap = sdj!!.map
        val sdjData = sdjMap["data"] as Map<*, *>?
        Assert.assertEquals("Android", sdjData!![Parameters.OS_TYPE])
        Assert.assertEquals("13", sdjData[Parameters.OS_VERSION])
        Assert.assertEquals("Nexus", sdjData[Parameters.DEVICE_MODEL])
        Assert.assertEquals("Google", sdjData[Parameters.DEVICE_MANUFACTURER])
        Assert.assertEquals("ATT", sdjData[Parameters.CARRIER])
        Assert.assertEquals("XJKLJSALFKJ", sdjData[Parameters.ANDROID_IDFA])
        Assert.assertEquals("wifi", sdjData[Parameters.NETWORK_TYPE])
        Assert.assertEquals("3g", sdjData[Parameters.NETWORK_TECHNOLOGY])
        Assert.assertEquals(10000L, sdjData[Parameters.PHYSICAL_MEMORY] as Long)
        Assert.assertEquals(10L, sdjData[Parameters.SYSTEM_AVAILABLE_MEMORY] as Long)
        Assert.assertEquals(20, sdjData[Parameters.BATTERY_LEVEL] as Int)
        Assert.assertEquals("charging", sdjData[Parameters.BATTERY_STATE])
        Assert.assertEquals(20000L, sdjData[Parameters.AVAILABLE_STORAGE] as Long)
        Assert.assertEquals(70000L, sdjData[Parameters.TOTAL_STORAGE] as Long)
    }

    @Test
    fun updatesMobileInfo() {
        val deviceInfoMonitor = MockDeviceInfoMonitor()
        val platformContext = PlatformContext(0, 0, deviceInfoMonitor, context)
        Assert.assertEquals(
            1,
            deviceInfoMonitor.getMethodAccessCount("getSystemAvailableMemory").toLong()
        )
        Assert.assertEquals(
            1,
            deviceInfoMonitor.getMethodAccessCount("getBatteryStateAndLevel").toLong()
        )
        platformContext.getMobileContext(false)
        Assert.assertEquals(
            2,
            deviceInfoMonitor.getMethodAccessCount("getSystemAvailableMemory").toLong()
        )
        Assert.assertEquals(
            2,
            deviceInfoMonitor.getMethodAccessCount("getBatteryStateAndLevel").toLong()
        )
    }

    @Test
    fun doesntUpdateMobileInfoWithinUpdateWindow() {
        val deviceInfoMonitor = MockDeviceInfoMonitor()
        val platformContext = PlatformContext(1000, 0, deviceInfoMonitor, context)
        Assert.assertEquals(
            1,
            deviceInfoMonitor.getMethodAccessCount("getSystemAvailableMemory").toLong()
        )
        Assert.assertEquals(
            1,
            deviceInfoMonitor.getMethodAccessCount("getBatteryStateAndLevel").toLong()
        )
        platformContext.getMobileContext(false)
        Assert.assertEquals(
            1,
            deviceInfoMonitor.getMethodAccessCount("getSystemAvailableMemory").toLong()
        )
        Assert.assertEquals(
            1,
            deviceInfoMonitor.getMethodAccessCount("getBatteryStateAndLevel").toLong()
        )
    }

    @Test
    fun updatesNetworkInfo() {
        val deviceInfoMonitor = MockDeviceInfoMonitor()
        val platformContext = PlatformContext(0, 0, deviceInfoMonitor, context)
        Assert.assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getNetworkType").toLong())
        Assert.assertEquals(
            1,
            deviceInfoMonitor.getMethodAccessCount("getNetworkTechnology").toLong()
        )
        platformContext.getMobileContext(false)
        Assert.assertEquals(2, deviceInfoMonitor.getMethodAccessCount("getNetworkType").toLong())
        Assert.assertEquals(
            2,
            deviceInfoMonitor.getMethodAccessCount("getNetworkTechnology").toLong()
        )
    }

    @Test
    fun doesntUpdateNetworkInfoWithinUpdateWindow() {
        val deviceInfoMonitor = MockDeviceInfoMonitor()
        val platformContext = PlatformContext(0, 1000, deviceInfoMonitor, context)
        Assert.assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getNetworkType").toLong())
        Assert.assertEquals(
            1,
            deviceInfoMonitor.getMethodAccessCount("getNetworkTechnology").toLong()
        )
        platformContext.getMobileContext(false)
        Assert.assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getNetworkType").toLong())
        Assert.assertEquals(
            1,
            deviceInfoMonitor.getMethodAccessCount("getNetworkTechnology").toLong()
        )
    }

    @Test
    fun doesntUpdateNonEphemeralInfo() {
        val deviceInfoMonitor = MockDeviceInfoMonitor()
        val platformContext = PlatformContext(0, 0, deviceInfoMonitor, context)
        Assert.assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getOsType").toLong())
        Assert.assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getTotalStorage").toLong())
        platformContext.getMobileContext(false)
        Assert.assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getOsType").toLong())
        Assert.assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getTotalStorage").toLong())
    }

    @Test
    fun doesntUpdateIdfaIfNotNull() {
        val deviceInfoMonitor = MockDeviceInfoMonitor()
        val platformContext = PlatformContext(0, 1, deviceInfoMonitor, context)
        Assert.assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getAndroidIdfa").toLong())
        platformContext.getMobileContext(false)
        Assert.assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getAndroidIdfa").toLong())
    }

    @Test
    fun updatesIdfaIfEmptyOrNull() {
        val deviceInfoMonitor = MockDeviceInfoMonitor()
        deviceInfoMonitor.customIdfa = ""
        val platformContext = PlatformContext(0, 1, deviceInfoMonitor, context)
        Assert.assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getAndroidIdfa").toLong())
        deviceInfoMonitor.customIdfa = null
        platformContext.getMobileContext(false)
        Assert.assertEquals(2, deviceInfoMonitor.getMethodAccessCount("getAndroidIdfa").toLong())
        platformContext.getMobileContext(false)
        Assert.assertEquals(3, deviceInfoMonitor.getMethodAccessCount("getAndroidIdfa").toLong())
    }

    @Test
    fun anonymisesUserIdentifiers() {
        val deviceInfoMonitor = MockDeviceInfoMonitor()
        val platformContext = PlatformContext(0, 0, deviceInfoMonitor, context)
        val sdj = platformContext.getMobileContext(true)
        val sdjMap = sdj!!.map
        val sdjData = sdjMap["data"] as Map<*, *>?
        Assert.assertEquals("Android", sdjData!![Parameters.OS_TYPE])
        Assert.assertNull(sdjData[Parameters.ANDROID_IDFA])
    }

    // --- PRIVATE
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext
}
