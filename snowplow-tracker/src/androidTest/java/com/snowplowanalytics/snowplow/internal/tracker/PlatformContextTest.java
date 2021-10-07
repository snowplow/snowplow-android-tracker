/*
 * Copyright (c) 2015-2021 Snowplow Analytics Ltd. All rights reserved.
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

import android.test.AndroidTestCase;

import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.internal.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;

import java.util.Map;

public class PlatformContextTest extends AndroidTestCase {

    public void testMobileContextNonMocked() {
        PlatformContext platformContext = new PlatformContext(getContext());
        SelfDescribingJson sdj = platformContext.getMobileContext();
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

    public void testAddsAllMockedInfo() {
        MockDeviceInfoMonitor deviceInfoMonitor = new MockDeviceInfoMonitor();
        PlatformContext platformContext = new PlatformContext(0, 0, deviceInfoMonitor, getContext());
        SelfDescribingJson sdj = platformContext.getMobileContext();
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

    public void testUpdatesMobileInfo() {
        MockDeviceInfoMonitor deviceInfoMonitor = new MockDeviceInfoMonitor();
        PlatformContext platformContext = new PlatformContext(0, 0, deviceInfoMonitor, getContext());
        assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getSystemAvailableMemory"));
        assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getBatteryStateAndLevel"));
        platformContext.getMobileContext();
        assertEquals(2, deviceInfoMonitor.getMethodAccessCount("getSystemAvailableMemory"));
        assertEquals(2, deviceInfoMonitor.getMethodAccessCount("getBatteryStateAndLevel"));
    }

    public void testDoesntUpdateMobileInfoWithinUpdateWindow() {
        MockDeviceInfoMonitor deviceInfoMonitor = new MockDeviceInfoMonitor();
        PlatformContext platformContext = new PlatformContext(1000, 0, deviceInfoMonitor, getContext());
        assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getSystemAvailableMemory"));
        assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getBatteryStateAndLevel"));
        platformContext.getMobileContext();
        assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getSystemAvailableMemory"));
        assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getBatteryStateAndLevel"));
    }

    public void testUpdatesNetworkInfo() {
        MockDeviceInfoMonitor deviceInfoMonitor = new MockDeviceInfoMonitor();
        PlatformContext platformContext = new PlatformContext(0, 0, deviceInfoMonitor, getContext());
        assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getNetworkType"));
        assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getNetworkTechnology"));
        platformContext.getMobileContext();
        assertEquals(2, deviceInfoMonitor.getMethodAccessCount("getNetworkType"));
        assertEquals(2, deviceInfoMonitor.getMethodAccessCount("getNetworkTechnology"));
    }

    public void testDoesntUpdateNetworkInfoWithinUpdateWindow() {
        MockDeviceInfoMonitor deviceInfoMonitor = new MockDeviceInfoMonitor();
        PlatformContext platformContext = new PlatformContext(0, 1000, deviceInfoMonitor, getContext());
        assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getNetworkType"));
        assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getNetworkTechnology"));
        platformContext.getMobileContext();
        assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getNetworkType"));
        assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getNetworkTechnology"));
    }

    public void testDoesntUpdateNonEphemeralInfo() {
        MockDeviceInfoMonitor deviceInfoMonitor = new MockDeviceInfoMonitor();
        PlatformContext platformContext = new PlatformContext(0, 0, deviceInfoMonitor, getContext());
        assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getOsType"));
        assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getTotalStorage"));
        platformContext.getMobileContext();
        assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getOsType"));
        assertEquals(1, deviceInfoMonitor.getMethodAccessCount("getTotalStorage"));
    }

}
