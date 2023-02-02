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
package com.snowplowanalytics.snowplow.tracker

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DevicePlatformTest {
    @Test
    fun testPlatforms() {
        Assert.assertEquals("web", DevicePlatform.Web.value)
        Assert.assertEquals("mob", DevicePlatform.Mobile.value)
        Assert.assertEquals("pc", DevicePlatform.Desktop.value)
        Assert.assertEquals("srv", DevicePlatform.ServerSideApp.value)
        Assert.assertEquals("app", DevicePlatform.General.value)
        Assert.assertEquals("tv", DevicePlatform.ConnectedTV.value)
        Assert.assertEquals("cnsl", DevicePlatform.GameConsole.value)
        Assert.assertEquals("iot", DevicePlatform.InternetOfThings.value)
    }
}
