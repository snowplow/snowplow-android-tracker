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
package com.snowplowanalytics.snowplow.event

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.snowplowanalytics.core.constants.Parameters
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class ScreenViewTest {
    @Test
    fun testExpectedForm() {
        var screenView = ScreenView("name")
        var data: Map<String, Any?> = screenView.dataPayload
        Assert.assertNotNull(data)
        Assert.assertEquals("name", data[Parameters.SV_NAME])
        Assert.assertTrue(data.containsKey(Parameters.SV_ID))
        val id = UUID.randomUUID()
        screenView = ScreenView("name", id)
        data = screenView.dataPayload
        Assert.assertNotNull(data)
        Assert.assertEquals(id.toString(), data[Parameters.SV_ID])
        Assert.assertEquals("name", data[Parameters.SV_NAME])
    }
}
