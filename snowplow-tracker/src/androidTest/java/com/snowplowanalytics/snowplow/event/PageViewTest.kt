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
package com.snowplowanalytics.snowplow.event

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.snowplowanalytics.core.constants.Parameters
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PageViewTest {
    @Test
    fun testExpectedForm() {
        var pageView = PageView("http://com.acme/foo/bar")
        Assert.assertEquals("pv", pageView.name)
        var data: Map<*, *> = pageView.dataPayload
        Assert.assertNotNull(data)
        Assert.assertEquals("http://com.acme/foo/bar", data[Parameters.PAGE_URL])
        Assert.assertFalse(data.containsKey(Parameters.PAGE_TITLE))
        Assert.assertFalse(data.containsKey(Parameters.PAGE_REFR))
        pageView = PageView("http://com.acme/foo/bar")
            .pageTitle("Page Title")
            .referrer("http://refr.com")
        data = pageView.dataPayload
        Assert.assertNotNull(data)
        Assert.assertEquals("http://com.acme/foo/bar", data[Parameters.PAGE_URL])
        Assert.assertEquals("Page Title", data[Parameters.PAGE_TITLE])
        Assert.assertEquals("http://refr.com", data[Parameters.PAGE_REFR])
    }

    @Test
    fun testBuilderFailures() {
        var exception = false
        try {
            PageView("")
        } catch (e: Exception) {
            Assert.assertEquals("pageUrl cannot be empty", e.message)
            exception = true
        }
        Assert.assertTrue(exception)
    }
}
