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

import android.content.Context
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.snowplowanalytics.core.emitter.Emitter
import com.snowplowanalytics.core.emitter.TLSVersion
import com.snowplowanalytics.snowplow.emitter.BufferOption
import com.snowplowanalytics.snowplow.network.*
import com.snowplowanalytics.snowplow.payload.Payload
import com.snowplowanalytics.snowplow.payload.TrackerPayload
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class EmitterTest {
    // Builder Tests
    @Test
    fun testHttpMethodSet() {
        var builder = { emitter: Emitter -> emitter.httpMethod = HttpMethod.GET }
        var emitter = Emitter(context, "com.acme", builder)
        Assert.assertEquals(HttpMethod.GET, emitter.httpMethod)
        builder = { emitter1: Emitter -> emitter1.httpMethod = HttpMethod.POST }
        emitter = Emitter(context, "com.acme", builder)
        Assert.assertEquals(HttpMethod.POST, emitter.httpMethod)
    }

    @Test
    fun testBufferOptionSet() {
        var builder = { emitter: Emitter -> emitter.bufferOption = BufferOption.Single }
        var emitter = Emitter(context, "com.acme", builder)
        Assert.assertEquals(BufferOption.Single, emitter.bufferOption)
        builder = { emitter1: Emitter -> emitter1.bufferOption = BufferOption.DefaultGroup }
        emitter = Emitter(context, "com.acme", builder)
        Assert.assertEquals(BufferOption.DefaultGroup, emitter.bufferOption)
        builder = { emitter2: Emitter -> emitter2.bufferOption = BufferOption.HeavyGroup }
        emitter = Emitter(context, "com.acme", builder)
        Assert.assertEquals(BufferOption.HeavyGroup, emitter.bufferOption)
    }

    @Test
    fun testCallbackSet() {
        val builder = { emitter: Emitter ->
            emitter.requestCallback = object : RequestCallback {
                override fun onSuccess(successCount: Int) {}
                override fun onFailure(successCount: Int, failureCount: Int) {}
            }
        }
        val emitter = Emitter(context, "com.acme", builder)
        Assert.assertNotNull(emitter.requestCallback)
    }

    @Test
    fun testUriSet() {
        val uri = "com.acme"
        var builder = { emitter: Emitter ->
            emitter.bufferOption = BufferOption.Single
            emitter.httpMethod = HttpMethod.GET
            emitter.requestSecurity = Protocol.HTTP
        }
        var emitter = Emitter(context, uri, builder)
        Assert.assertEquals("http://$uri/i", emitter.emitterUri)
        builder = { emitter1: Emitter ->
            emitter1.bufferOption = BufferOption.DefaultGroup
            emitter1.httpMethod = HttpMethod.POST
            emitter1.requestSecurity = Protocol.HTTP
        }
        emitter = Emitter(context, uri, builder)
        Assert.assertEquals(
            "http://$uri/com.snowplowanalytics.snowplow/tp2",
            emitter.emitterUri
        )
        builder = { emitter2: Emitter ->
            emitter2.bufferOption = BufferOption.DefaultGroup
            emitter2.httpMethod = HttpMethod.GET
            emitter2.requestSecurity = Protocol.HTTPS
        }
        emitter = Emitter(context, uri, builder)
        Assert.assertEquals("https://$uri/i", emitter.emitterUri)
        builder = { emitter3: Emitter ->
            emitter3.bufferOption = BufferOption.DefaultGroup
            emitter3.httpMethod = HttpMethod.POST
            emitter3.requestSecurity = Protocol.HTTPS
        }
        emitter = Emitter(context, uri, builder)
        Assert.assertEquals("https://$uri/com.snowplowanalytics.snowplow/tp2", emitter.emitterUri)
    }

    @Test
    fun testSecuritySet() {
        var builder = { emitter: Emitter -> emitter.requestSecurity = Protocol.HTTP }
        var emitter = Emitter(context, "com.acme", builder)
        Assert.assertEquals(Protocol.HTTP, emitter.requestSecurity)
        builder = { emitter1: Emitter -> emitter1.requestSecurity = Protocol.HTTPS }
        emitter = Emitter(context, "com.acme", builder)
        Assert.assertEquals(Protocol.HTTPS, emitter.requestSecurity)
    }

    @Test
    fun testTickSet() {
        val builder = { emitter: Emitter -> emitter.emitterTick = 0 }
        val emitter = Emitter(context, "com.acme", builder)
        Assert.assertEquals(0, emitter.emitterTick.toLong())
    }

    @Test
    fun testEmptyLimitSet() {
        val builder = { emitter: Emitter -> emitter.emptyLimit = 0 }
        val emitter = Emitter(context, "com.acme", builder)
        Assert.assertEquals(0, emitter.emptyLimit.toLong())
    }

    @Test
    fun testSendLimitSet() {
        val builder = { emitter: Emitter -> emitter.sendLimit = 200 }
        val emitter = Emitter(context, "com.acme", builder)
        Assert.assertEquals(200, emitter.sendLimit.toLong())
    }

    @Test
    fun testByteLimitGetSet() {
        val builder = { emitter: Emitter -> emitter.byteLimitGet = 20000 }
        val emitter = Emitter(context, "com.acme", builder)
        Assert.assertEquals(20000, emitter.byteLimitGet)
    }

    @Test
    fun testByteLimitPostSet() {
        val builder = { emitter: Emitter -> emitter.byteLimitPost = 25000 }
        val emitter = Emitter(context, "com.acme", builder)
        Assert.assertEquals(25000, emitter.byteLimitPost)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testUpdatingEmitterSettings() {
        val uri = "snowplow.io"
        var builder = { emitter: Emitter ->
            emitter.bufferOption = BufferOption.Single
            emitter.httpMethod = HttpMethod.POST
            emitter.requestSecurity = Protocol.HTTP
            emitter.emitterTick = 250
            emitter.emptyLimit = 5
            emitter.sendLimit = 200
            emitter.byteLimitGet = 20000
            emitter.byteLimitPost = 25000
            emitter.eventStore = MockEventStore()
            emitter.timeUnit = TimeUnit.MILLISECONDS
        }
        val emitter = Emitter(context, uri, builder)
        Assert.assertFalse(emitter.emitterStatus)
        Assert.assertEquals(BufferOption.Single, emitter.bufferOption)
        Assert.assertEquals("http://$uri/com.snowplowanalytics.snowplow/tp2", emitter.emitterUri)
        emitter.httpMethod = HttpMethod.GET
        Assert.assertEquals("http://$uri/i", emitter.emitterUri)
        emitter.emitterUri = uri
        emitter.requestSecurity = Protocol.HTTPS
        Assert.assertEquals("https://$uri/i", emitter.emitterUri)
        emitter.emitterUri = "com.acme"
        Assert.assertEquals("https://com.acme/i", emitter.emitterUri)
        emitter.bufferOption = BufferOption.HeavyGroup
        Assert.assertEquals(BufferOption.HeavyGroup, emitter.bufferOption)
        emitter.flush()
        emitter.flush()
        Thread.sleep(500)
        Assert.assertTrue(emitter.emitterStatus)
        emitter.httpMethod = HttpMethod.POST
        Assert.assertEquals(
            "https://com.acme/com.snowplowanalytics.snowplow/tp2",
            emitter.emitterUri
        )
        emitter.emitterUri = "com.foo"
        Assert.assertEquals(
            "https://com.foo/com.snowplowanalytics.snowplow/tp2",
            emitter.emitterUri
        )
        emitter.bufferOption = BufferOption.DefaultGroup
        Assert.assertEquals(BufferOption.HeavyGroup, emitter.bufferOption)
        emitter.shutdown()
        builder = { emitter1: Emitter ->
            emitter1.bufferOption = BufferOption.Single
            emitter1.httpMethod = HttpMethod.POST
            emitter1.requestSecurity = Protocol.HTTP
            emitter1.emitterTick = 250
            emitter1.emptyLimit = 5
            emitter1.sendLimit = 200
            emitter1.byteLimitGet = 20000
            emitter1.byteLimitPost = 25000
            emitter1.eventStore = MockEventStore()
            emitter1.timeUnit = TimeUnit.MILLISECONDS
            emitter1.customPostPath = "com.acme.company/tpx"
        }
        val customPathEmitter = Emitter(context, uri, builder)
        Assert.assertEquals("com.acme.company/tpx", customPathEmitter.customPostPath)
        Assert.assertEquals("http://$uri/com.acme.company/tpx", customPathEmitter.emitterUri)
        customPathEmitter.shutdown()
    }

    // Emitting Tests
    @Test
    @Throws(InterruptedException::class)
    fun testEmitEventWithBrokenNetworkConnectionDoesntFreezeEmitterStatus() {
        val networkConnection: NetworkConnection = BrokenNetworkConnection()
        val emitter = getEmitter(networkConnection, BufferOption.Single)
        emitter.add(generatePayloads(1)[0])
        Thread.sleep(1000)
        Assert.assertFalse(emitter.emitterStatus)
        emitter.flush()
    }

    @Test
    @Throws(InterruptedException::class)
    fun testEmitSingleGetEventWithSuccess() {
        val networkConnection = MockNetworkConnection(HttpMethod.GET, 200)
        val emitter = getEmitter(networkConnection, BufferOption.Single)
        emitter.add(generatePayloads(1)[0])
        var i = 0
        while (i < 10 && (networkConnection.sendingCount() < 1 || emitter.emitterStatus)) {
            Thread.sleep(600)
            i++
        }
        Assert.assertEquals(1, networkConnection.previousResults.size.toLong())
        Assert.assertEquals(1, networkConnection.previousResults[0].size.toLong())
        Assert.assertTrue(networkConnection.previousResults[0][0].isSuccessful)
        Assert.assertEquals(0, emitter.eventStore!!.size())
        emitter.flush()
    }

    @Test
    @Throws(InterruptedException::class)
    fun testEmitSingleGetEventWithNoSuccess() {
        val networkConnection = MockNetworkConnection(HttpMethod.GET, 500)
        val emitter = getEmitter(networkConnection, BufferOption.Single)
        emitter.add(generatePayloads(1)[0])
        var i = 0
        while (i < 10 && (networkConnection.sendingCount() < 1 || emitter.emitterStatus)) {
            Thread.sleep(600)
            i++
        }
        Assert.assertEquals(1, networkConnection.previousResults.size.toLong())
        Assert.assertEquals(1, networkConnection.previousResults[0].size.toLong())
        Assert.assertFalse(networkConnection.previousResults[0][0].isSuccessful)
        Assert.assertEquals(1, emitter.eventStore!!.size())
        emitter.flush()
    }

    @Test
    @Throws(InterruptedException::class)
    fun testEmitTwoGetEventsWithSuccess() {
        val networkConnection = MockNetworkConnection(HttpMethod.GET, 200)
        val emitter = getEmitter(networkConnection, BufferOption.Single)
        for (payload in generatePayloads(2)) {
            emitter.add(payload)
        }
        var i = 0
        while (i < 10 && (networkConnection.sendingCount() < 2 || emitter.emitterStatus)) {
            Thread.sleep(600)
            i++
        }
        Assert.assertEquals(0, emitter.eventStore!!.size())
        var totEvents = 0
        for (results in networkConnection.previousResults) {
            for (result in results) {
                Assert.assertTrue(result.isSuccessful)
                totEvents += result.eventIds.size
            }
        }
        Assert.assertEquals(2, totEvents.toLong())
        emitter.flush()
    }

    @Test
    @Throws(InterruptedException::class)
    fun testEmitTwoGetEventsWithNoSuccess() {
        val networkConnection = MockNetworkConnection(HttpMethod.GET, 500)
        val emitter = getEmitter(networkConnection, BufferOption.Single)
        for (payload in generatePayloads(2)) {
            emitter.add(payload)
        }
        var i = 0
        while (i < 10 && (networkConnection.sendingCount() < 2 || emitter.emitterStatus)) {
            Thread.sleep(600)
            i++
        }
        Assert.assertEquals(2, emitter.eventStore!!.size())
        for (results in networkConnection.previousResults) {
            for (result in results) {
                Assert.assertFalse(result.isSuccessful)
            }
        }
        // Can't check the total number of events sent as the Emitter stops to send if a request fails.
        emitter.flush()
    }

    @Test
    @Throws(InterruptedException::class)
    fun testEmitSinglePostEventWithSuccess() {
        val networkConnection = MockNetworkConnection(HttpMethod.POST, 200)
        val emitter = getEmitter(networkConnection, BufferOption.Single)
        emitter.add(generatePayloads(1)[0])
        var i = 0
        while (i < 10 && networkConnection.sendingCount() < 1) {
            Thread.sleep(600)
            i++
        }
        Assert.assertEquals(1, networkConnection.previousResults.size.toLong())
        Assert.assertEquals(1, networkConnection.previousResults[0].size.toLong())
        Assert.assertTrue(networkConnection.previousResults[0][0].isSuccessful)
        Assert.assertEquals(0, emitter.eventStore!!.size())
        emitter.flush()
    }

    @Test
    @Throws(InterruptedException::class)
    fun testPauseAndResumeEmittingEvents() {
        val networkConnection = MockNetworkConnection(HttpMethod.POST, 200)
        val emitter = getEmitter(networkConnection, BufferOption.Single)
        emitter.pauseEmit()
        emitter.add(generatePayloads(1)[0])
        
        Thread.sleep(1000)
        Assert.assertEquals(false, emitter.emitterStatus)
        Assert.assertEquals(0, networkConnection.sendingCount().toLong())
        Assert.assertEquals(0, networkConnection.previousResults.size.toLong())
        Assert.assertEquals(1, emitter.eventStore!!.size())
        
        emitter.resumeEmit()
        var i = 0
        while (i < 10 && networkConnection.sendingCount() < 1) {
            Thread.sleep(600)
            i++
        }
        Assert.assertEquals(1, networkConnection.previousResults.size.toLong())
        Assert.assertEquals(1, networkConnection.previousResults[0].size.toLong())
        Assert.assertTrue(networkConnection.previousResults[0][0].isSuccessful)
        Assert.assertEquals(0, emitter.eventStore!!.size())
        emitter.flush()
    }

    @Test
    @Throws(InterruptedException::class)
    fun testUpdatesNetworkConnectionWhileRunning() {
        val builder = { emitter: Emitter -> emitter.eventStore = MockEventStore() }
        val emitter = Emitter(context, "com.acme", builder)
        emitter.flush()
        Thread.sleep(100)
        Assert.assertTrue(emitter.emitterStatus) // is running
        emitter.emitterUri = "new.uri" // update while running
        Assert.assertTrue(emitter.emitterStatus) // is running
        Assert.assertTrue(emitter.emitterUri.contains("new.uri"))
    }

    @Test
    @Throws(InterruptedException::class)
    fun testRemovesEventsFromQueueOnNoRetryStatus() {
        val networkConnection = MockNetworkConnection(HttpMethod.GET, 403)
        val emitter = getEmitter(networkConnection, BufferOption.Single)
        emitter.add(generatePayloads(1)[0])
        var i = 0
        while (i < 10 && (networkConnection.sendingCount() < 1 || emitter.emitterStatus)) {
            Thread.sleep(600)
            i++
        }
        Assert.assertEquals(1, networkConnection.previousResults.size.toLong())
        Assert.assertFalse(networkConnection.previousResults[0][0].isSuccessful)
        Assert.assertEquals(0, emitter.eventStore!!.size())
        emitter.flush()
    }

    @Test
    @Throws(InterruptedException::class)
    fun testFollowCustomRetryRules() {
        val networkConnection = MockNetworkConnection(HttpMethod.GET, 500)
        val emitter = getEmitter(networkConnection, BufferOption.Single)
        val customRules: MutableMap<Int, Boolean> = HashMap()
        customRules[403] = true
        customRules[500] = false
        emitter.customRetryForStatusCodes = customRules

        // no events in queue since they were dropped because retrying is disabled for 500
        emitter.add(generatePayloads(1)[0])
        Thread.sleep(1000)
        Assert.assertEquals(0, emitter.eventStore!!.size())
        
        networkConnection.statusCode = 403
        emitter.add(generatePayloads(1)[0])
        Thread.sleep(1000)

        // event still in queue because retrying is enabled for 403
        Assert.assertEquals(1, emitter.eventStore!!.size())
        Assert.assertEquals(2, networkConnection.previousResults.size.toLong())
        emitter.flush()
    }

    @Test
    @Throws(InterruptedException::class)
    fun testDoesNotRetryFailedRequestsIfDisabled() {
        val networkConnection = MockNetworkConnection(HttpMethod.GET, 500)
        val emitter = getEmitter(networkConnection, BufferOption.Single)
        emitter.retryFailedRequests = false

        // no events in queue since they were dropped because retrying is disabled
        emitter.add(generatePayloads(1)[0])
        Thread.sleep(1000)
        Assert.assertEquals(0, emitter.eventStore!!.size())

        emitter.retryFailedRequests = true
        emitter.add(generatePayloads(1)[0])
        Thread.sleep(1000)

        // event still in queue because retrying is enabled
        Assert.assertEquals(1, emitter.eventStore!!.size())
        Assert.assertEquals(2, networkConnection.previousResults.size.toLong())
        emitter.flush()
    }

    @Test
    fun testDoesntMakeRequestUnlessBufferSizeIsReached() {
        val networkConnection = MockNetworkConnection(HttpMethod.GET, 200)
        val emitter = getEmitter(networkConnection, BufferOption.SmallGroup)

        for (payload in generatePayloads(9)) {
            emitter.add(payload)
        }
        Thread.sleep(1000)

        // all events waiting in queue
        Assert.assertEquals(0, networkConnection.previousResults.size)
        Assert.assertEquals(9, emitter.eventStore!!.size())

        emitter.add(generatePayloads(1)[0])
        Thread.sleep(1000)

        // all events sent
        Assert.assertEquals(0, emitter.eventStore!!.size())
        Assert.assertEquals(1, networkConnection.previousResults.size)
        emitter.flush()
    }

    // Emitter Builder
    private fun getEmitter(networkConnection: NetworkConnection?, option: BufferOption?): Emitter {
        val builder = { emitter: Emitter ->
            emitter.networkConnection = networkConnection
            emitter.bufferOption = option!!
            emitter.emitterTick = 0
            emitter.emptyLimit = 0
            emitter.sendLimit = 200
            emitter.byteLimitGet = 20000
            emitter.byteLimitPost = 25000
            emitter.eventStore = MockEventStore()
            emitter.timeUnit = TimeUnit.SECONDS
            emitter.tlsVersions = EnumSet.of(TLSVersion.TLSv1_2)
        }
        return Emitter(context, "com.acme", builder)
    }

    // Service methods
    private fun generatePayloads(count: Int): List<Payload> {
        val payloads = ArrayList<Payload>()
        for (i in 0 until count) {
            val payload = TrackerPayload()
            payload.add("a", i.toString())
            payloads.add(payload)
        }
        return payloads
    }
    
    private val context: Context
        get() = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
} // Mock classes

internal class BrokenNetworkConnection : NetworkConnection {
    override fun sendRequests(requests: List<Request>): List<RequestResult> {
        throw UnsupportedOperationException("Broken NetworkConnection")
    }

    override val httpMethod: HttpMethod
        get() {
            throw UnsupportedOperationException("Broken NetworkConnection")
        }
    override val uri: Uri
        get() {
            throw UnsupportedOperationException("Broken NetworkConnection")
        }
}
