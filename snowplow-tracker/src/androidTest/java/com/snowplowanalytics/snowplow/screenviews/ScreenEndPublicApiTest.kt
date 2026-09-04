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
package com.snowplowanalytics.snowplow.tracker

import androidx.test.ext.junit.runners.AndroidJUnit4
// Explicit single-class import from the public event package only – no wildcard, no core.*
// This test fails to compile if ScreenEnd stops being public API.
import com.snowplowanalytics.snowplow.event.ScreenEnd
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pins that [ScreenEnd] is reachable as public API from the `com.snowplowanalytics.snowplow.event`
 * package, so that integrators can manually end a screen (e.g. when a WebView is presented over a
 * native screen). See AISP-1670.
 */
@RunWith(AndroidJUnit4::class)
class ScreenEndPublicApiTest {

    @Test
    fun screenEndIsConstructibleFromThePublicEventPackage() {
        val event = ScreenEnd()

        Assert.assertEquals(
            "iglu:com.snowplowanalytics.mobile/screen_end/jsonschema/1-0-0",
            event.schema
        )
        Assert.assertTrue(event.dataPayload.isEmpty())
    }
}
