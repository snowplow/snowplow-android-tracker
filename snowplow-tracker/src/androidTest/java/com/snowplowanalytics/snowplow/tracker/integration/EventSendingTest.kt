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
package com.snowplowanalytics.snowplow.tracker.integration

import android.annotation.SuppressLint
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.snowplowanalytics.core.emitter.Emitter
import com.snowplowanalytics.core.emitter.storage.SQLiteEventStore
import com.snowplowanalytics.core.tracker.Subject
import com.snowplowanalytics.core.tracker.Tracker
import com.snowplowanalytics.snowplow.util.TestUtils.createSessionSharedPreferences
import com.snowplowanalytics.snowplow.emitter.BufferOption
import com.snowplowanalytics.snowplow.emitter.EventStore
import com.snowplowanalytics.snowplow.event.*
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.network.Protocol
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow.tracker.BuildConfig
import com.snowplowanalytics.snowplow.tracker.LogLevel
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.net.URLDecoder
import java.util.*
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class EventSendingTest {
    @Before
    @Throws(Exception::class)
    fun setUp() {
        try {
            if (tracker == null) return
            val emitter = tracker!!.emitter
            tracker!!.close()
            val isClean = emitter.eventStore.removeAllEvents()
            Log.i("TrackerTest", "Tracker closed - EventStore cleaned: $isClean")
            Log.i("TrackerTest", "Events in the store: " + emitter.eventStore.size())
        } catch (e: IllegalStateException) {
            Log.i("TrackerTest", "Tracker already closed.")
        }
    }

    // Test Setup
    @Throws(IOException::class)
    private fun getMockServer(count: Int): MockWebServer {
        val eventStore: EventStore =
            SQLiteEventStore(InstrumentationRegistry.getInstrumentation().targetContext, "namespace")
        eventStore.removeAllEvents()
        val mockServer = MockWebServer()
        mockServer.start()
        val mockResponse = MockResponse().setResponseCode(200)
        for (i in 0 until count) {
            mockServer.enqueue(mockResponse)
        }
        return mockServer
    }

    @Throws(IOException::class)
    fun killMockServer(mockServer: MockWebServer) {
        mockServer.shutdown()
    }

    // Tests
    @Test
    @Throws(Exception::class)
    fun testSendGet() {
        val mockServer = getMockServer(14)
        val tracker = getTracker("myNamespace", getMockServerURI(mockServer), HttpMethod.GET)
        trackStructuredEvent(tracker)
        trackUnstructuredEvent(tracker)
        trackPageView(tracker)
        trackTimings(tracker)
        trackScreenView(tracker)
        trackEcommerceEvent(tracker)
        trackConsentGranted(tracker)
        trackConsentWithdrawn(tracker)
        waitForTracker(tracker)
        checkGetRequest(getRequests(mockServer, 14))
        killMockServer(mockServer)
    }

    @Test
    @Throws(Exception::class)
    fun testSendPost() {
        val mockServer = getMockServer(14)
        val tracker = getTracker("myNamespacePost", getMockServerURI(mockServer), HttpMethod.POST)
        trackStructuredEvent(tracker)
        trackUnstructuredEvent(tracker)
        trackPageView(tracker)
        trackTimings(tracker)
        trackScreenView(tracker)
        trackEcommerceEvent(tracker)
        trackConsentGranted(tracker)
        trackConsentWithdrawn(tracker)
        waitForTracker(tracker)
        checkPostRequest(getRequests(mockServer, 14))
        killMockServer(mockServer)
    }

    @Test
    @Throws(Exception::class)
    fun testSessionContext() {
        val mockServer = getMockServer(14)
        val tracker =
            getTracker("namespaceSessionTest", getMockServerURI(mockServer), HttpMethod.POST)
        
        tracker.track(ScreenView("screenName_1"))
        tracker.track(Structured("category_1", "action_1"))
        tracker.startNewSession()
        Thread.sleep(1000)
        tracker.track(Structured("category_2", "action_2"))
        tracker.track(Structured("category_3", "action_3"))
        
        waitForTracker(tracker)
        
        val requests = getRequests(mockServer, 4)
        var screenViewSessionData: JSONObject? = null
        var structSessionData1: JSONObject? = null
        var structSessionData2: JSONObject? = null
        var structSessionData3: JSONObject? = null
        for (request in requests) {
            val data = JSONObject(request!!.body.readUtf8()).getJSONArray("data").getJSONObject(0)
            val eventType = data["e"] as String
            val contexts = JSONObject(data["co"] as String).getJSONArray("data")
            when (eventType) {
                "ue" -> screenViewSessionData = getSessionData(contexts)
                "se" -> {
                    when (data["se_ca"] as String) {
                        "category_1" -> structSessionData1 = getSessionData(contexts)
                        "category_2" -> structSessionData2 = getSessionData(contexts)
                        "category_3" -> structSessionData3 = getSessionData(contexts)
                    }
                }
                else -> {}
            }
        }
        Assert.assertEquals(1, screenViewSessionData!!["sessionIndex"])
        Assert.assertEquals(2, structSessionData2!!["sessionIndex"])
        Assert.assertEquals(
            screenViewSessionData["firstEventId"],
            structSessionData1!!["firstEventId"]
        )
        Assert.assertNotEquals(
            screenViewSessionData["firstEventId"],
            structSessionData2["firstEventId"]
        )
        Assert.assertEquals(
            structSessionData2["firstEventId"],
            structSessionData3!!["firstEventId"]
        )
        Assert.assertEquals(1, screenViewSessionData["eventIndex"])
        Assert.assertEquals(2, structSessionData1["eventIndex"])
        Assert.assertEquals(1, structSessionData2["eventIndex"])
        Assert.assertEquals(2, structSessionData3["eventIndex"])
        Assert.assertEquals(
            screenViewSessionData["firstEventTimestamp"],
            structSessionData1["firstEventTimestamp"]
        )
        Assert.assertNotEquals(
            screenViewSessionData["firstEventTimestamp"],
            structSessionData2["firstEventTimestamp"]
        )
        Assert.assertEquals(
            structSessionData2["firstEventTimestamp"],
            structSessionData3["firstEventTimestamp"]
        )
        Assert.assertEquals(
            structSessionData1["sessionId"],
            structSessionData2["previousSessionId"]
        )
        killMockServer(mockServer)
    }

    // Helpers
    private fun getTracker(namespace: String, uri: String, method: HttpMethod): Tracker {
        val ns = namespace + Math.random().toString() // add random number to ensure different namespace on each run
        createSessionSharedPreferences(InstrumentationRegistry.getInstrumentation().targetContext, ns)
        val builder = { emitter: Emitter ->
            emitter.bufferOption = BufferOption.Single
            emitter.httpMethod = method
            emitter.requestSecurity = Protocol.HTTP
            emitter.emitterTick = 0
            emitter.emptyLimit = 0
            emitter.emitRange = 1
        }
        val emitter = Emitter(ns, null, InstrumentationRegistry.getInstrumentation().targetContext, uri, builder)
        val subject = Subject(InstrumentationRegistry.getInstrumentation().targetContext, null)
        
        if (tracker != null) tracker!!.close()
        
        val trackerBuilder = { tracker: Tracker ->
            tracker.subject = subject
            tracker.base64Encoded = false
            tracker.logLevel = LogLevel.DEBUG
            tracker.sessionContext = true
            tracker.platformContextEnabled = true
            tracker.geoLocationContext = false
        }
        tracker = Tracker(
            emitter,
            ns,
            "myAppId",
            null,
            context = InstrumentationRegistry.getInstrumentation().targetContext,
            builder = trackerBuilder
        )
        emitter.eventStore.removeAllEvents()
        return tracker!!
    }

    @SuppressLint("DefaultLocale")
    fun getMockServerURI(mockServer: MockWebServer): String {
        return String.format("%s:%d", mockServer.hostName, mockServer.port)
    }

    @Throws(Exception::class)
    fun getRequests(mockServer: MockWebServer, count: Int): LinkedList<RecordedRequest?> {
        val requests = LinkedList<RecordedRequest?>()
        for (i in 0 until count) {
            val request = mockServer.takeRequest(60, TimeUnit.SECONDS)
            if (request == null) {
                Assert.fail("MockWebServer didn't receive events.")
            }
            requests.add(request)
        }
        return requests
    }

    @Throws(Exception::class)
    fun getQueryMap(query: String): Map<String?, String?> {
        val params = query.split("&").toTypedArray()
        val map: MutableMap<String?, String?> = HashMap()
        for (param in params) {
            val name = param.split("=").toTypedArray()[0]
            val value = param.split("=").toTypedArray()[1]
            map[URLDecoder.decode(name, "UTF-8")] = URLDecoder.decode(value, "UTF-8")
        }
        return map
    }

    @Throws(Exception::class)
    fun waitForTracker(tracker: Tracker) {
        var counter = 0
        while (!tracker.emitter.emitterStatus) {
            Thread.sleep(500)
            counter++
            if (counter > 10) {
                return
            }
        }
        counter = 0
        while (tracker.emitter.emitterStatus) {
            Thread.sleep(500)
            counter++
            if (counter > 10) {
                return
            }
        }
        Thread.sleep(500)
        tracker.pauseEventTracking()
    }

    @Throws(JSONException::class)
    fun getSessionData(entities: JSONArray): JSONObject? {
        for (i in 0 until entities.length()) {
            val sessionSchema =
                "iglu:com.snowplowanalytics.snowplow/client_session/jsonschema/1-0-2"
            val entity = entities[i] as JSONObject
            val schema = entity["schema"] as String
            if (schema == sessionSchema) {
                return entity["data"] as JSONObject
            }
        }
        return null
    }

    // Event Validation
    @Throws(Exception::class)
    fun checkGetRequest(requests: LinkedList<RecordedRequest?>) {
        Assert.assertEquals(14, requests.size.toLong())
        for (request in requests) {
            Assert.assertEquals("/i", request!!.path!!.substring(0, 2))
            Assert.assertEquals("GET", request.method)
            val query = JSONObject(getQueryMap(request.path!!.substring(3)))
            Assert.assertEquals("mob", query["p"])
            Assert.assertEquals("myAppId", query["aid"])
            Assert.assertTrue(query.getString("tna").startsWith("myNamespace"))
            Assert.assertEquals(BuildConfig.TRACKER_LABEL, query["tv"])
            Assert.assertEquals("English", query["lang"])
            Assert.assertTrue(query.has("dtm"))
            Assert.assertTrue(query.has("stm"))
            Assert.assertTrue(query.has("e"))
            Assert.assertTrue(query.has("co"))
            Assert.assertTrue(query.has("eid"))
            when (query["e"].toString()) {
                "pv" -> checkPageView(query)
                "ue" -> checkUnstructuredEvent(query)
                "se" -> checkStructuredEvent(query)
                "tr" -> checkEcommerceEvent(query)
                "ti" -> checkEcommerceItemEvent(query)
                else -> {}
            }
        }
    }

    @Throws(Exception::class)
    fun checkPostRequest(requests: LinkedList<RecordedRequest?>) {
        Assert.assertEquals(14, requests.size.toLong())
        for (request in requests) {
            Assert.assertEquals("/com.snowplowanalytics.snowplow/tp2", request!!.path)
            Assert.assertEquals("POST", request.method)
            val body = JSONObject(request.body.readUtf8())
            Assert.assertEquals(
                "iglu:com.snowplowanalytics.snowplow/payload_data/jsonschema/1-0-4",
                body.getString("schema")
            )
            val data = body.getJSONArray("data")
            for (i in 0 until data.length()) {
                val json = data.getJSONObject(i)
                Assert.assertEquals("mob", json.getString("p"))
                Assert.assertEquals("myAppId", json.getString("aid"))
                Assert.assertTrue(json.getString("tna").startsWith("myNamespacePost"))
                Assert.assertEquals(BuildConfig.TRACKER_LABEL, json.getString("tv"))
                Assert.assertEquals("English", json.getString("lang"))
                Assert.assertTrue(json.has("dtm"))
                Assert.assertTrue(json.has("stm"))
                Assert.assertTrue(json.has("e"))
                Assert.assertTrue(json.has("co"))
                Assert.assertTrue(json.has("eid"))
                when (json.getString("e")) {
                    "pv" -> checkPageView(json)
                    "ue" -> checkUnstructuredEvent(json)
                    "se" -> checkStructuredEvent(json)
                    "tr" -> checkEcommerceEvent(json)
                    "ti" -> checkEcommerceItemEvent(json)
                    else -> {}
                }
            }
        }
    }

    @Throws(Exception::class)
    fun checkPageView(json: JSONObject) {
        Assert.assertEquals("pageUrl", json.getString("url"))
        Assert.assertEquals("pageReferrer", json.getString("refr"))
        Assert.assertEquals("pageTitle", json.getString("page"))
    }

    @Throws(Exception::class)
    fun checkScreenView(json: JSONObject) {
        Assert.assertEquals("screenId", json.getString("id"))
        Assert.assertEquals("screenName", json.getString("name"))
    }

    @Throws(Exception::class)
    fun checkTimings(json: JSONObject) {
        Assert.assertEquals(1, json.getInt("timing").toLong())
        Assert.assertEquals("variable", json.getString("variable"))
        Assert.assertEquals("category", json.getString("category"))
        Assert.assertEquals("label", json.getString("label"))
    }

    @Throws(Exception::class)
    fun checkStructuredEvent(json: JSONObject) {
        Assert.assertEquals("property", json.getString("se_pr"))
        Assert.assertEquals("0.0", json.getString("se_va"))
        Assert.assertEquals("label", json.getString("se_la"))
        Assert.assertEquals("category", json.getString("se_ca"))
        Assert.assertEquals("action", json.getString("se_ac"))
    }

    @Throws(Exception::class)
    fun checkTestEvent(json: JSONObject) {
        Assert.assertEquals("test-value-1", json.getString("test-key-1"))
    }

    @Throws(Exception::class)
    fun checkEcommerceEvent(json: JSONObject) {
        Assert.assertEquals("AUD", json.getString("tr_cu"))
        Assert.assertEquals("5.0", json.getString("tr_sh"))
        Assert.assertEquals("Australia", json.getString("tr_co"))
        Assert.assertEquals("2.5", json.getString("tr_tx"))
        Assert.assertEquals("affiliation", json.getString("tr_af"))
        Assert.assertEquals("order-1", json.getString("tr_id"))
        Assert.assertEquals("Sydney", json.getString("tr_ci"))
        Assert.assertEquals("42.5", json.getString("tr_tt"))
        Assert.assertEquals("NSW", json.getString("tr_st"))
    }

    @Throws(Exception::class)
    fun checkEcommerceItemEvent(json: JSONObject) {
        Assert.assertEquals("Acme 1", json.getString("ti_nm"))
        Assert.assertEquals("order-1", json.getString("ti_id"))
        Assert.assertEquals("AUD", json.getString("ti_cu"))
        Assert.assertEquals("1", json.getString("ti_qu"))
        Assert.assertEquals("35.0", json.getString("ti_pr"))
        Assert.assertEquals("Stuff", json.getString("ti_ca"))
        Assert.assertEquals("sku-1", json.getString("ti_sk"))
    }

    @Throws(Exception::class)
    fun checkConsentGrantedEvent(json: JSONObject) {
        Assert.assertEquals("gexpiry", json.getString("expiry"))
    }

    @Throws(Exception::class)
    fun checkConsentWithdrawnEvent(json: JSONObject) {
        Assert.assertFalse(json.getBoolean("all"))
    }

    @Throws(Exception::class)
    fun checkUnstructuredEvent(json: JSONObject) {
        val unstructEvent = JSONObject(json.getString("ue_pr"))
        Assert.assertEquals(
            "iglu:com.snowplowanalytics.snowplow/unstruct_event/jsonschema/1-0-0",
            unstructEvent.getString("schema")
        )
        val innerSchema = unstructEvent.getJSONObject("data").getString("schema")
        val innerData = unstructEvent.getJSONObject("data").getJSONObject("data")
        when (innerSchema) {
            "iglu:com.snowplowanalytics.snowplow/screen_view/jsonschema/1-0-0" -> checkScreenView(
                innerData
            )
            "iglu:com.snowplowanalytics.snowplow/timing/jsonschema/1-0-0" -> checkTimings(innerData)
            "iglu:com.snowplowanalytics.snowplow/test_sdj/jsonschema/1-0-1" -> checkTestEvent(
                innerData
            )
            "iglu:com.snowplowanalytics.snowplow/consent_granted/jsonschema/1-0-0" -> checkConsentGrantedEvent(
                innerData
            )
            "iglu:com.snowplowanalytics.snowplow/consent_withdrawn/jsonschema/1-0-0" -> checkConsentWithdrawnEvent(
                innerData
            )
            else -> {}
        }
    }

    // Event Tracker Functions
    @Throws(Exception::class)
    fun trackPageView(tracker: Tracker) {
        tracker.track(PageView("pageUrl").pageTitle("pageTitle").referrer("pageReferrer"))
        tracker.track(
            PageView("pageUrl").pageTitle("pageTitle").referrer("pageReferrer").entities(
                customContext
            )
        )
    }

    private fun trackStructuredEvent(tracker: Tracker) {
        tracker.track(
            Structured("category", "action").label("label").property("property").value(0.00)
        )
        tracker.track(
            Structured("category", "action").label("label").property("property").value(0.00)
                .entities(
                    customContext
                )
        )
    }

    private fun trackScreenView(tracker: Tracker) {
        tracker.track(ScreenView("screenName"))
        tracker.track(ScreenView("screenName").entities(customContext))
    }

    private fun trackTimings(tracker: Tracker) {
        tracker.track(Timing("category", "variable", 1).label("label"))
        tracker.track(
            Timing("category", "variable", 1).label("label").entities(
                customContext
            )
        )
    }

    private fun trackUnstructuredEvent(tracker: Tracker) {
        val attributes: MutableMap<String, String> = HashMap()
        attributes["test-key-1"] = "test-value-1"
        val test = SelfDescribingJson(
            "iglu:com.snowplowanalytics.snowplow/test_sdj/jsonschema/1-0-1",
            attributes
        )
        tracker.track(SelfDescribing(test))
        tracker.track(
            SelfDescribing(test).entities(
                customContext
            )
        )
    }

    @Suppress("deprecation")
    private fun trackEcommerceEvent(tracker: Tracker) {
        val item = EcommerceTransactionItem("sku-1", 35.00, 1).name("Acme 1").category("Stuff")
            .currency("AUD")
        val items: MutableList<EcommerceTransactionItem> = LinkedList()
        items.add(item)
        tracker.track(
            EcommerceTransaction("order-1", 42.50, items).affiliation("affiliation").taxValue(2.50)
                .shipping(5.00).city("Sydney").state("NSW").country("Australia").currency("AUD")
        )
        tracker.track(
            EcommerceTransaction("order-1", 42.50, items).affiliation("affiliation").taxValue(2.50)
                .shipping(5.00).city("Sydney").state("NSW").country("Australia").currency("AUD")
                .entities(
                    customContext
                )
        )
    }

    private fun trackConsentGranted(tracker: Tracker) {
        val documents: MutableList<ConsentDocument> = LinkedList()
        documents.add(
            ConsentDocument("granted context id 1", "granted context version 1")
                .documentDescription("granted context desc 1")
                .documentName("granted context name 1")
        )
        documents.add(
            ConsentDocument("granted context id 2", "granted context version 2")
                .documentDescription("granted context desc 2")
                .documentName("granted context name 2")
        )
        tracker.track(
            ConsentGranted("gexpiry", "gid", "dversion").documentDescription("gdesc")
                .documentName("dname").documents(documents)
        )
        tracker.track(
            ConsentGranted("gexpiry", "gid", "dversion").documentDescription("gdesc")
                .documentName("dname").documents(documents).entities(
                customContext
            )
        )
    }

    private fun trackConsentWithdrawn(tracker: Tracker) {
        val documents: MutableList<ConsentDocument> = LinkedList()
        documents.add(
            ConsentDocument("withdrawn context id 1", "withdrawn context version 1")
                .documentDescription("withdrawn context desc 1")
                .documentName("withdrawn context name 1")
        )
        documents.add(
            ConsentDocument("withdrawn context id 2", "withdrawn context version 2")
                .documentDescription("withdrawn context desc 2")
                .documentName("withdrawn context name 2")
        )
        tracker.track(
            ConsentWithdrawn(false, "gid", "dversion").documentDescription("gdesc")
                .documentName("dname").documents(documents)
        )
        tracker.track(
            ConsentWithdrawn(false, "gid", "dversion").documentDescription("gdesc")
                .documentName("dname").documents(documents).entities(
                customContext
            )
        )
    }

    private val customContext: List<SelfDescribingJson>
        get() {
            val contexts: MutableList<SelfDescribingJson> = ArrayList()
            val attributes1: MutableMap<String, String> = HashMap()
            attributes1["key-1"] = "value-1"
            val json1 = SelfDescribingJson(
                "iglu:com.snowplowanalytics.snowplow/example_1/jsonschema/1-0-1", attributes1
            )
            contexts.add(json1)
            return contexts
        }

    companion object {
        private var tracker: Tracker? = null
    }
}
