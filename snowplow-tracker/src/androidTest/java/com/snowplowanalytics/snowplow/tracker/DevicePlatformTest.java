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

package com.snowplowanalytics.snowplow.tracker;

import android.test.AndroidTestCase;

public class DevicePlatformTest extends AndroidTestCase {

    public void testPlatforms() {
        assertEquals("web", DevicePlatform.Web.getValue());
        assertEquals("mob", DevicePlatform.Mobile.getValue());
        assertEquals("pc", DevicePlatform.Desktop.getValue());
        assertEquals("srv", DevicePlatform.ServerSideApp.getValue());
        assertEquals("app", DevicePlatform.General.getValue());
        assertEquals("tv", DevicePlatform.ConnectedTV.getValue());
        assertEquals("cnsl", DevicePlatform.GameConsole.getValue());
        assertEquals("iot", DevicePlatform.InternetOfThings.getValue());
    }
}
