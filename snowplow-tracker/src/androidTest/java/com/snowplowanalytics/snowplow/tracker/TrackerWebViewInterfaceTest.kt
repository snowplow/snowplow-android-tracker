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

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.snowplowanalytics.core.tracker.TrackerWebViewInterface
import com.snowplowanalytics.core.utils.Util.objectMapToString
import com.snowplowanalytics.snowplow.Snowplow.createTracker
import com.snowplowanalytics.snowplow.Snowplow.removeAllTrackers
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration
import com.snowplowanalytics.snowplow.network.HttpMethod
import junit.framework.TestCase
import org.json.JSONException
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackerWebViewInterfaceTest {
    private var webInterface: TrackerWebViewInterface? = null
    private var networkConnection: MockNetworkConnection? = null
    @Before
    @Throws(Exception::class)
    fun setUp() {
        webInterface = TrackerWebViewInterface()
        networkConnection = MockNetworkConnection(HttpMethod.GET, 200)
        val networkConfig = NetworkConfiguration(networkConnection!!)
        val trackerConfig = TrackerConfiguration("app1")
        trackerConfig.sessionContext = false
        trackerConfig.platformContext = false
        trackerConfig.base64encoding = false
        val trackerNamespace = Math.random().toString()
        removeAllTrackers()
        createTracker(context, trackerNamespace, networkConfig, trackerConfig)
    }

    @After
    fun tearDown() {
        removeAllTrackers()
    }

    // --- TESTS
    @Test
    @Throws(JSONException::class, InterruptedException::class)
    fun tracksStructuredEventWithAllProperties() {
        webInterface!!.trackStructEvent(
            "cat", "act", "lbl", "prop", 10.0, null, null
        )
        var i = 0
        while (i < 10 && networkConnection!!.countRequests() == 0) {
            Thread.sleep(1000)
            i++
        }
        TestCase.assertEquals(1, networkConnection!!.countRequests())
        val request = networkConnection!!.allRequests[0]
        val payload = objectMapToString(request.payload.map)
        TestCase.assertEquals("cat", payload["se_ca"])
        TestCase.assertEquals("act", payload["se_ac"])
        TestCase.assertEquals("prop", payload["se_pr"])
        TestCase.assertEquals("lbl", payload["se_la"])
        TestCase.assertEquals("10.0", payload["se_va"])
    }

    @Test
    @Throws(JSONException::class, InterruptedException::class)
    fun tracksEventWithCorrectTracker() {
        // create the second tracker
        val networkConnection2 = MockNetworkConnection(HttpMethod.GET, 200)
        val networkConfig = NetworkConfiguration(networkConnection2)
        createTracker(context, "ns2", networkConfig)

        // track an event using the second tracker
        webInterface!!.trackPageView("http://localhost", null, null, null, arrayOf("ns2"))

        // wait and check for the event
        var i = 0
        while (i < 10 && networkConnection2.countRequests() == 0) {
            Thread.sleep(1000)
            i++
        }
        TestCase.assertEquals(0, networkConnection!!.countRequests())
        TestCase.assertEquals(1, networkConnection2.countRequests())
    }

    @Test
    @Throws(JSONException::class, InterruptedException::class)
    fun tracksEventWithContext() {
        webInterface!!.trackSelfDescribingEvent(
            "http://schema.com",
            "{\"key\":\"val\"}",
            "[{\"schema\":\"http://context-schema.com\",\"data\":{\"a\":\"b\"}}]",
            null
        )
        var i = 0
        while (i < 10 && networkConnection!!.countRequests() == 0) {
            Thread.sleep(1000)
            i++
        }
        TestCase.assertEquals(1, networkConnection!!.countRequests())
        val request = networkConnection!!.allRequests[0]
        val parsedEntity = JSONObject(request.payload.map["co"] as String)
            .getJSONArray("data")
            .getJSONObject(0)
        TestCase.assertEquals("http://context-schema.com", parsedEntity.getString("schema"))
        TestCase.assertEquals("b", parsedEntity.getJSONObject("data").getString("a"))
    }

    // --- PRIVATE
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext
}
