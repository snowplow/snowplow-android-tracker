/*
 * Copyright (c) 2015-present Snowplow Analytics Ltd. All rights reserved.
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
import com.snowplowanalytics.snowplow.configuration.PlatformContextProperty
import com.snowplowanalytics.snowplow.tracker.PlatformContextRetriever
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class PlatformContextTest {
    // --- TESTS
    @Test
    fun addsNotMockedMobileContext() {
        val platformContext = PlatformContext(context = context)
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
        Assert.assertTrue(sdjData.containsKey(Parameters.MOBILE_LANGUAGE))
    }

    @Test
    fun addsAllMockedInfo() {
        val deviceInfoMonitor = MockDeviceInfoMonitor()
        val platformContext = PlatformContext(0, 0, deviceInfoMonitor, context = context)
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
        Assert.assertEquals("sk", sdjData[Parameters.MOBILE_LANGUAGE])
        Assert.assertEquals("1024x768", sdjData[Parameters.MOBILE_RESOLUTION])
        Assert.assertEquals(2.0f, sdjData[Parameters.MOBILE_SCALE] as Float)
        Assert.assertEquals(true, sdjData[Parameters.IS_PORTRAIT] as Boolean)
        Assert.assertEquals("XXX", sdjData[Parameters.APP_SET_ID])
        Assert.assertEquals("app", sdjData[Parameters.APP_SET_ID_SCOPE])
    }

    @Test
    fun updatesMobileInfo() {
        val deviceInfoMonitor = MockDeviceInfoMonitor()
        val platformContext = PlatformContext(0, 0, deviceInfoMonitor, context = context)
        platformContext.getMobileContext(false)
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
    fun doesntFetchPropertiesIfNotRequested() {
        val deviceInfoMonitor = MockDeviceInfoMonitor()
        PlatformContext(1000, 0, deviceInfoMonitor, context = context)
        Assert.assertEquals(
            0,
            deviceInfoMonitor.getMethodAccessCount("getSystemAvailableMemory").toLong()
        )
        Assert.assertEquals(
            0,
            deviceInfoMonitor.getMethodAccessCount("getBatteryStateAndLevel").toLong()
        )
    }

    @Test
    fun doesntUpdateMobileInfoWithinUpdateWindow() {
        val deviceInfoMonitor = MockDeviceInfoMonitor()
        val platformContext = PlatformContext(1000, 0, deviceInfoMonitor, context = context)
        platformContext.getMobileContext(false)
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
        val platformContext = PlatformContext(0, 0, deviceInfoMonitor, context = context)
        platformContext.getMobileContext(false)
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
        val platformContext = PlatformContext(0, 1000, deviceInfoMonitor, context = context)
        platformContext.getMobileContext(false)
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
        val platformContext = PlatformContext(0, 0, deviceInfoMonitor, context = context)
        platformContext.getMobileContext(false)
        Assert.assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getOsType").toLong())
        Assert.assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getTotalStorage").toLong())
        platformContext.getMobileContext(false)
        Assert.assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getOsType").toLong())
        Assert.assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getTotalStorage").toLong())
    }

    @Test
    fun doesntUpdateIdfaIfNotNull() {
        val deviceInfoMonitor = MockDeviceInfoMonitor()
        val platformContext = PlatformContext(0, 1, deviceInfoMonitor, context = context)
        platformContext.getMobileContext(false)
        Assert.assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getAndroidIdfa").toLong())
        platformContext.getMobileContext(false)
        Assert.assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getAndroidIdfa").toLong())
    }

    @Test
    fun anonymisesUserIdentifiers() {
        val deviceInfoMonitor = MockDeviceInfoMonitor()
        val platformContext = PlatformContext(0, 0, deviceInfoMonitor, context = context)
        val sdj = platformContext.getMobileContext(true)
        val sdjMap = sdj!!.map
        val sdjData = sdjMap["data"] as Map<*, *>?
        Assert.assertEquals("Android", sdjData!![Parameters.OS_TYPE])
        Assert.assertNull(sdjData[Parameters.ANDROID_IDFA])
    }

    @Test
    fun readsAppSetInfoSynchronouslyFromGeneralPrefsSecondTime() {
        val deviceInfoMonitor = MockDeviceInfoMonitor()
        PlatformContext(0, 0, deviceInfoMonitor, context = context)

        Thread.sleep(100)

        val secondPlatformContext = PlatformContext(0, 0, deviceInfoMonitor, context = context)
        val sdj = secondPlatformContext.getMobileContext(true)
        val sdjData = sdj!!.map["data"] as Map<*, *>?

        Assert.assertEquals("XXX", sdjData!![Parameters.APP_SET_ID])
        Assert.assertEquals("app", sdjData[Parameters.APP_SET_ID_SCOPE])
    }

    @Test
    fun onlyAddsRequestedProperties() {
        val deviceInfoMonitor = MockDeviceInfoMonitor()
        val platformContext = PlatformContext(
            0, 0, deviceInfoMonitor,
            listOf(PlatformContextProperty.ANDROID_IDFA, PlatformContextProperty.BATTERY_LEVEL),
            context = context
        )

        val sdj = platformContext.getMobileContext(false)
        Assert.assertNotNull(sdj)
        val sdjData = sdj!!.map["data"] as Map<*, *>

        Assert.assertEquals("Android", sdjData[Parameters.OS_TYPE])
        Assert.assertEquals("XJKLJSALFKJ", sdjData[Parameters.ANDROID_IDFA])
        Assert.assertEquals(20, sdjData[Parameters.BATTERY_LEVEL] as Int)
        Assert.assertFalse(sdjData.containsKey(Parameters.APP_SET_ID))
        Assert.assertFalse(sdjData.containsKey(Parameters.NETWORK_TYPE))
        Assert.assertFalse(sdjData.containsKey(Parameters.IS_PORTRAIT))
    }

    @Test
    fun truncatesLanguageToMax8Chars() {
        val deviceInfoMonitor = MockDeviceInfoMonitor()
        deviceInfoMonitor.language = "1234567890"
        val platformContext = PlatformContext(0, 0, deviceInfoMonitor, context = context)

        val sdj = platformContext.getMobileContext(false)
        Assert.assertNotNull(sdj)
        val sdjData = sdj!!.map["data"] as Map<*, *>

        Assert.assertEquals("12345678", sdjData[Parameters.MOBILE_LANGUAGE])
    }

    @Test
    fun invalidLocaleLanguageIsNullNoMocking() {
        val defaultLocale = Locale.getDefault()

        // set locale to an ISO-639 invalid 2-letter code
        Locale.setDefault(Locale("dk", "example"))
        
        val platformContext = PlatformContext(context = context)
        val sdj = platformContext.getMobileContext(false)
        Assert.assertNotNull(sdj)
        val sdjData = sdj!!.map["data"] as Map<*, *>

        Assert.assertFalse(sdjData.containsKey(Parameters.MOBILE_LANGUAGE))
        
        // restore original locale
        Locale.setDefault(defaultLocale)        
    }

    @Test
    fun doesntSetTheNetworkTechIfNotRequested() {
        val platformContext = PlatformContext(properties = listOf(
            PlatformContextProperty.NETWORK_TYPE
        ), context = context)
        val sdj = platformContext.getMobileContext(false)
        Assert.assertNotNull(sdj)
        val sdjData = sdj!!.map["data"] as Map<*, *>

        Assert.assertTrue(sdjData.containsKey(Parameters.NETWORK_TYPE))
        Assert.assertFalse(sdjData.containsKey(Parameters.NETWORK_TECHNOLOGY))
    }

    @Test
    fun PlatformContextRetrieverOverridesProperties() {
        val retriever = PlatformContextRetriever(
            osType = { "r1" },
            osVersion = { "r2" },
            deviceVendor = { "r3" },
            deviceModel = { "r4" },
            carrier = { "r5" },
            networkType = { "r6" },
            networkTechnology = { "r7" },
            androidIdfa = { "r8" },
            availableStorage = { 100 },
            totalStorage = { 101 },
            physicalMemory = { 102 },
            systemAvailableMemory = { 103 },
            batteryLevel = { 104 },
            batteryState = { "r9" },
            isPortrait = { false },
            resolution = { "r10" },
            scale = { 105f },
            language = { "r11" },
            appSetId = { "r12" },
            appSetIdScope = { "r13" },
        )
        val platformContext = PlatformContext(
            retriever = retriever,
            context = context
        )
        Thread.sleep(100)

        val sdj = platformContext.getMobileContext(false)
        Assert.assertNotNull(sdj)
        val sdjData = sdj!!.map["data"] as Map<*, *>

        Assert.assertEquals("r1", sdjData[Parameters.OS_TYPE])
        Assert.assertEquals("r2", sdjData[Parameters.OS_VERSION])
        Assert.assertEquals("r3", sdjData[Parameters.DEVICE_MANUFACTURER])
        Assert.assertEquals("r4", sdjData[Parameters.DEVICE_MODEL])
        Assert.assertEquals("r5", sdjData[Parameters.CARRIER])
        Assert.assertEquals("r6", sdjData[Parameters.NETWORK_TYPE])
        Assert.assertEquals("r7", sdjData[Parameters.NETWORK_TECHNOLOGY])
        Assert.assertEquals("r8", sdjData[Parameters.ANDROID_IDFA])
        Assert.assertEquals(100L, sdjData[Parameters.AVAILABLE_STORAGE])
        Assert.assertEquals(101L, sdjData[Parameters.TOTAL_STORAGE])
        Assert.assertEquals(102L, sdjData[Parameters.PHYSICAL_MEMORY])
        Assert.assertEquals(103L, sdjData[Parameters.SYSTEM_AVAILABLE_MEMORY])
        Assert.assertEquals(104, sdjData[Parameters.BATTERY_LEVEL])
        Assert.assertEquals("r9", sdjData[Parameters.BATTERY_STATE])
        Assert.assertEquals(false, sdjData[Parameters.IS_PORTRAIT])
        Assert.assertEquals("r10", sdjData[Parameters.MOBILE_RESOLUTION])
        Assert.assertEquals(105f, sdjData[Parameters.MOBILE_SCALE])
        Assert.assertEquals("r11", sdjData[Parameters.MOBILE_LANGUAGE])
        Assert.assertEquals("r12", sdjData[Parameters.APP_SET_ID])
        Assert.assertEquals("r13", sdjData[Parameters.APP_SET_ID_SCOPE])
    }

    @Test
    fun appSetIdNotAddedIfEmpty() {
        val deviceInfoMonitor = object : MockDeviceInfoMonitor() {
            override fun getAppSetIdAndScope(context: Context): android.util.Pair<String, String>? {
                // Simulate what happens when AppSetIdInfo returns empty string
                return android.util.Pair("", "app")
            }
        }
        val platformContext = PlatformContext(0, 0, deviceInfoMonitor, context = context)

        val sdj = platformContext.getMobileContext(false)
        Assert.assertNotNull(sdj)
        val sdjData = sdj!!.map["data"] as Map<*, *>

        Assert.assertFalse(sdjData.containsKey(Parameters.APP_SET_ID))
        Assert.assertTrue(sdjData.containsKey(Parameters.APP_SET_ID_SCOPE))
        Assert.assertEquals("app", sdjData[Parameters.APP_SET_ID_SCOPE])
    }

    @Test
    fun batteryLevelNotTrackedIfNegative() {
        val deviceInfoMonitor = MockDeviceInfoMonitor()
        deviceInfoMonitor.batteryLevel = -1
        val platformContext = PlatformContext(0, 0, deviceInfoMonitor, context = context)

        val sdj = platformContext.getMobileContext(false)
        Assert.assertNotNull(sdj)
        val sdjData = sdj!!.map["data"] as Map<*, *>

        Assert.assertEquals("charging", sdjData[Parameters.BATTERY_STATE])
        Assert.assertFalse(sdjData.containsKey(Parameters.BATTERY_LEVEL))
    }

    // --- PRIVATE
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext
}
