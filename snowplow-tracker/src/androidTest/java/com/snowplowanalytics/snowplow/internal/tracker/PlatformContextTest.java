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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.internal.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class PlatformContextTest {

    // --- TESTS

    @Test
    public void addsNotMockedMobileContext() {
        PlatformContext platformContext = new PlatformContext(getContext());
        SelfDescribingJson sdj = platformContext.getMobileContext(false);
        assertNotNull(sdj);

        Map<String, Object> sdjMap = sdj.getMap();
        assertEquals((String) sdjMap.get("schema"), TrackerConstants.MOBILE_SCHEMA);

        Map sdjData = (Map) sdjMap.get("data");
        assertEquals((String) sdjData.get(Parameters.OS_TYPE), "android");
        assertTrue(sdjData.containsKey(Parameters.OS_VERSION));
        assertTrue(sdjData.containsKey(Parameters.DEVICE_MODEL));
        assertTrue(sdjData.containsKey(Parameters.DEVICE_MANUFACTURER));
        assertTrue(sdjData.containsKey(Parameters.CARRIER));
        assertTrue(sdjData.containsKey(Parameters.NETWORK_TYPE));
    }

    @Test
    public void addsAllMockedInfo() {
        MockDeviceInfoMonitor deviceInfoMonitor = new MockDeviceInfoMonitor();
        PlatformContext platformContext = new PlatformContext(0, 0, deviceInfoMonitor, getContext());
        SelfDescribingJson sdj = platformContext.getMobileContext(false);
        Map<String, Object> sdjMap = sdj.getMap();
        Map sdjData = (Map) sdjMap.get("data");
        assertEquals("Android", sdjData.get(Parameters.OS_TYPE));
        assertEquals("13", sdjData.get(Parameters.OS_VERSION));
        assertEquals("Nexus", sdjData.get(Parameters.DEVICE_MODEL));
        assertEquals("Google", sdjData.get(Parameters.DEVICE_MANUFACTURER));
        assertEquals("ATT", sdjData.get(Parameters.CARRIER));
        assertEquals("XJKLJSALFKJ", sdjData.get(Parameters.ANDROID_IDFA));
        assertEquals("wifi", sdjData.get(Parameters.NETWORK_TYPE));
        assertEquals("3g", sdjData.get(Parameters.NETWORK_TECHNOLOGY));
        assertEquals(10000L, (long) sdjData.get(Parameters.PHYSICAL_MEMORY));
        assertEquals(10L, (long) sdjData.get(Parameters.SYSTEM_AVAILABLE_MEMORY));
        assertEquals(20, (int) sdjData.get(Parameters.BATTERY_LEVEL));
        assertEquals("charging", sdjData.get(Parameters.BATTERY_STATE));
        assertEquals(20000L, (long) sdjData.get(Parameters.AVAILABLE_STORAGE));
        assertEquals(70000L, (long) sdjData.get(Parameters.TOTAL_STORAGE));
    }

    @Test
    public void updatesMobileInfo() {
        MockDeviceInfoMonitor deviceInfoMonitor = new MockDeviceInfoMonitor();
        PlatformContext platformContext = new PlatformContext(0, 0, deviceInfoMonitor, getContext());
        assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getSystemAvailableMemory"));
        assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getBatteryStateAndLevel"));
        platformContext.getMobileContext(false);
        assertEquals(2, deviceInfoMonitor.getMethodAccessCount("getSystemAvailableMemory"));
        assertEquals(2, deviceInfoMonitor.getMethodAccessCount("getBatteryStateAndLevel"));
    }

    @Test
    public void doesntUpdateMobileInfoWithinUpdateWindow() {
        MockDeviceInfoMonitor deviceInfoMonitor = new MockDeviceInfoMonitor();
        PlatformContext platformContext = new PlatformContext(1000, 0, deviceInfoMonitor, getContext());
        assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getSystemAvailableMemory"));
        assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getBatteryStateAndLevel"));
        platformContext.getMobileContext(false);
        assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getSystemAvailableMemory"));
        assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getBatteryStateAndLevel"));
    }

    @Test
    public void updatesNetworkInfo() {
        MockDeviceInfoMonitor deviceInfoMonitor = new MockDeviceInfoMonitor();
        PlatformContext platformContext = new PlatformContext(0, 0, deviceInfoMonitor, getContext());
        assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getNetworkType"));
        assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getNetworkTechnology"));
        platformContext.getMobileContext(false);
        assertEquals(2, deviceInfoMonitor.getMethodAccessCount("getNetworkType"));
        assertEquals(2, deviceInfoMonitor.getMethodAccessCount("getNetworkTechnology"));
    }

    @Test
    public void doesntUpdateNetworkInfoWithinUpdateWindow() {
        MockDeviceInfoMonitor deviceInfoMonitor = new MockDeviceInfoMonitor();
        PlatformContext platformContext = new PlatformContext(0, 1000, deviceInfoMonitor, getContext());
        assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getNetworkType"));
        assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getNetworkTechnology"));
        platformContext.getMobileContext(false);
        assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getNetworkType"));
        assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getNetworkTechnology"));
    }

    @Test
    public void doesntUpdateNonEphemeralInfo() {
        MockDeviceInfoMonitor deviceInfoMonitor = new MockDeviceInfoMonitor();
        PlatformContext platformContext = new PlatformContext(0, 0, deviceInfoMonitor, getContext());
        assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getOsType"));
        assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getTotalStorage"));
        platformContext.getMobileContext(false);
        assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getOsType"));
        assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getTotalStorage"));
    }

    @Test
    public void doesntUpdateIdfaIfNotNull() {
        MockDeviceInfoMonitor deviceInfoMonitor = new MockDeviceInfoMonitor();
        PlatformContext platformContext = new PlatformContext(0, 1, deviceInfoMonitor, getContext());
        assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getAndroidIdfa"));
        platformContext.getMobileContext(false);
        assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getAndroidIdfa"));
    }

    @Test
    public void updatesIdfaIfEmptyOrNull() {
        MockDeviceInfoMonitor deviceInfoMonitor = new MockDeviceInfoMonitor();
        deviceInfoMonitor.customIdfa = "";
        PlatformContext platformContext = new PlatformContext(0, 1, deviceInfoMonitor, getContext());
        assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getAndroidIdfa"));
        deviceInfoMonitor.customIdfa = null;
        platformContext.getMobileContext(false);
        assertEquals(2, deviceInfoMonitor.getMethodAccessCount("getAndroidIdfa"));
        platformContext.getMobileContext(false);
        assertEquals(3, deviceInfoMonitor.getMethodAccessCount("getAndroidIdfa"));
    }

    @Test
    public void anonymisesUserIdentifiers() {
        MockDeviceInfoMonitor deviceInfoMonitor = new MockDeviceInfoMonitor();
        PlatformContext platformContext = new PlatformContext(0, 0, deviceInfoMonitor, getContext());
        SelfDescribingJson sdj = platformContext.getMobileContext(true);
        Map<String, Object> sdjMap = sdj.getMap();
        Map sdjData = (Map) sdjMap.get("data");

        assertEquals("Android", sdjData.get(Parameters.OS_TYPE));
        assertNull(sdjData.get(Parameters.ANDROID_IDFA));
    }

    // --- PRIVATE

    private Context getContext() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

}
