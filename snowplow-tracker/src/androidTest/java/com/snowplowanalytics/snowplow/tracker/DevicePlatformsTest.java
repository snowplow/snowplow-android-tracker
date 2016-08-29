/*
 * Copyright (c) 2015 Snowplow Analytics Ltd. All rights reserved.
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

public class DevicePlatformsTest extends AndroidTestCase {

    public void testPlatforms() {
        assertEquals("web", DevicePlatforms.Web.getValue());
        assertEquals("mob", DevicePlatforms.Mobile.getValue());
        assertEquals("pc", DevicePlatforms.Desktop.getValue());
        assertEquals("srv", DevicePlatforms.ServerSideApp.getValue());
        assertEquals("app", DevicePlatforms.General.getValue());
        assertEquals("tv", DevicePlatforms.ConnectedTV.getValue());
        assertEquals("cnsl", DevicePlatforms.GameConsole.getValue());
        assertEquals("iot", DevicePlatforms.InternetOfThings.getValue());
    }
}
