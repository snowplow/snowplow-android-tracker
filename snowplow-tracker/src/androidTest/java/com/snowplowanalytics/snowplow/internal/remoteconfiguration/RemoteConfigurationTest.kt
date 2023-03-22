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
package com.snowplowanalytics.snowplow.internal.remoteconfiguration

import android.annotation.SuppressLint
import androidx.core.util.Pair
import androidx.test.espresso.core.internal.deps.guava.collect.Lists
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.snowplowanalytics.core.remoteconfiguration.ConfigurationCache
import com.snowplowanalytics.core.remoteconfiguration.ConfigurationFetcher
import com.snowplowanalytics.core.remoteconfiguration.ConfigurationProvider
import com.snowplowanalytics.core.remoteconfiguration.FetchedConfigurationBundle
import com.snowplowanalytics.snowplow.configuration.ConfigurationBundle
import com.snowplowanalytics.snowplow.configuration.ConfigurationState
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration
import com.snowplowanalytics.snowplow.configuration.RemoteConfiguration
import com.snowplowanalytics.snowplow.network.HttpMethod
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class RemoteConfigurationTest {
    @Test
    @Throws(JSONException::class)
    fun testJSONToConfigurations() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config =
            ("{\"\$schema\":\"http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-0-0\","
                    + "\"configurationVersion\":12,\"configurationBundle\": ["
                    + "{\"namespace\": \"default1\","
                    + "\"networkConfiguration\": {\"endpoint\":\"https://fake.snowplow.io\",\"method\":\"get\"},"
                    + "\"trackerConfiguration\": {\"applicationContext\":false,\"screenContext\":false},"
                    + "\"sessionConfiguration\": {\"backgroundTimeout\":60,\"foregroundTimeout\":60}"
                    + "},"
                    + "{\"namespace\": \"default2\","
                    + "\"subjectConfiguration\": {\"userId\":\"testUserId\"}"
                    + "}"
                    + "]}")
        val json = JSONObject(config)
        val fetchedConfigurationBundle = FetchedConfigurationBundle(context, json)
        Assert.assertEquals(
            "http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-0-0",
            fetchedConfigurationBundle.schema
        )
        Assert.assertEquals(12, fetchedConfigurationBundle.configurationVersion.toLong())
        Assert.assertEquals(2, fetchedConfigurationBundle.configurationBundle.size.toLong())

        // Regular setup
        var configurationBundle = fetchedConfigurationBundle.configurationBundle[0]
        Assert.assertEquals("default1", configurationBundle.namespace)
        Assert.assertNotNull(configurationBundle.networkConfiguration)
        Assert.assertNotNull(configurationBundle.trackerConfiguration)
        Assert.assertNotNull(configurationBundle.sessionConfiguration)
        Assert.assertNull(configurationBundle.subjectConfiguration)
        val networkConfiguration = configurationBundle.networkConfiguration
        Assert.assertEquals(HttpMethod.GET, networkConfiguration!!.method)
        val trackerConfiguration = configurationBundle.trackerConfiguration
        Assert.assertFalse(trackerConfiguration!!.applicationContext)
        val sessionConfiguration = configurationBundle.sessionConfiguration
        Assert.assertEquals(60, sessionConfiguration!!.foregroundTimeout.convert(TimeUnit.SECONDS))

        // Regular setup without NetworkConfiguration
        configurationBundle = fetchedConfigurationBundle.configurationBundle[1]
        Assert.assertEquals("default2", configurationBundle.namespace)
        Assert.assertNull(configurationBundle.networkConfiguration)
        Assert.assertNotNull(configurationBundle.subjectConfiguration)
        val subjectConfiguration = configurationBundle.subjectConfiguration
        Assert.assertEquals("testUserId", subjectConfiguration!!.userId)
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun testDownloadConfiguration() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val body =
            "{\"\$schema\":\"http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-0-0\",\"configurationVersion\":12,\"configurationBundle\":[]}"
        val mockWebServer = getMockServer(200, body)
        val endpoint = getMockServerURI(mockWebServer)
        
        val expectation = Any() as Object
        val remoteConfig = RemoteConfiguration(endpoint!!, HttpMethod.GET)
        ConfigurationFetcher(
            context,
            remoteConfig
        ) { fetchedConfigurationBundle: FetchedConfigurationBundle ->
            Assert.assertNotNull(fetchedConfigurationBundle)
            Assert.assertEquals(
                "http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-0-0",
                fetchedConfigurationBundle.schema
            )
            synchronized(expectation) { expectation.notify() }
        }
        synchronized(expectation) { expectation.wait(10000) }
        mockWebServer.shutdown()
    }

    @Test
    fun testConfigurationCache() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bundle = ConfigurationBundle("test")
        bundle.networkConfiguration = NetworkConfiguration("endpoint")
        val expected =
            FetchedConfigurationBundle("http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-0-0")
        expected.configurationVersion = 12
        expected.configurationBundle = listOf(bundle)
        val remoteConfiguration = RemoteConfiguration("http://example.com", HttpMethod.GET)
        var cache = ConfigurationCache(remoteConfiguration)
        cache.clearCache(context)
        cache.writeCache(context, expected)
        cache = ConfigurationCache(remoteConfiguration)
        val config = cache.readCache(context)
        Assert.assertEquals(
            expected.configurationVersion.toLong(),
            config!!.configurationVersion.toLong()
        )
        Assert.assertEquals(expected.schema, config.schema)
        Assert.assertEquals(
            expected.configurationBundle.size.toLong(),
            config.configurationBundle.size.toLong()
        )
        val expectedBundle = expected.configurationBundle[0]
        val configBundle = config.configurationBundle[0]
        Assert.assertEquals(
            expectedBundle.networkConfiguration!!.endpoint,
            configBundle.networkConfiguration!!.endpoint
        )
        Assert.assertNull(configBundle.trackerConfiguration)
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun testConfigurationFetcher_downloads() {
        // prepare test
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val mockWebServer = getMockServer(
            200,
            "{\"\$schema\":\"http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/2-0-0\",\"configurationVersion\":12,\"configurationBundle\":[]}"
        )
        val endpoint = getMockServerURI(mockWebServer)

        // test
        val expectation = Any() as Object
        val expectationNotified = AtomicBoolean(false)
        val remoteConfig = RemoteConfiguration(endpoint!!, HttpMethod.GET)
        ConfigurationFetcher(
            context,
            remoteConfig
        ) {
            expectationNotified.set(true)
            synchronized(expectation) { expectation.notify() }
        }
        synchronized(expectation) { expectation.wait(5000) }
        Assert.assertTrue(expectationNotified.get())
        mockWebServer.shutdown()
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun testConfigurationProvider_notDownloading_fails() {
        // prepare test
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val mockWebServer = getMockServer(500, "{}")
        val endpoint = getMockServerURI(mockWebServer)
        val remoteConfig = RemoteConfiguration(endpoint!!, HttpMethod.GET)
        val cache = ConfigurationCache(remoteConfig)
        cache.clearCache(context)

        // test
        val expectation = Any() as Object
        val provider = ConfigurationProvider(remoteConfig)
        provider.retrieveConfiguration(
            context,
            false
        ) { Assert.fail() }
        synchronized(expectation) { expectation.wait(5000) }
        mockWebServer.shutdown()
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun testConfigurationProvider_downloadOfWrongSchema_fails() {
        // prepare test
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val mockWebServer = getMockServer(
            200,
            "{\"\$schema\":\"http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-0-0\",\"configurationVersion\":12,\"configurationBundle\":[]}"
        )
        val endpoint = getMockServerURI(mockWebServer)
        val remoteConfig = RemoteConfiguration(endpoint!!, HttpMethod.GET)
        val cache = ConfigurationCache(remoteConfig)
        cache.clearCache(context)

        // test
        val expectation = Any() as Object
        val provider = ConfigurationProvider(remoteConfig)
        provider.retrieveConfiguration(
            context,
            false
        ) { Assert.fail() }
        synchronized(expectation) { expectation.wait(5000) }
        mockWebServer.shutdown()
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun testConfigurationProvider_downloadSameConfigVersionThanCached_dontUpdate() {
        // prepare test
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val mockWebServer = getMockServer(
            200,
            "{\"\$schema\":\"http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-1-0\",\"configurationVersion\":1,\"configurationBundle\":[]}"
        )
        val endpoint = getMockServerURI(mockWebServer)
        val remoteConfig = RemoteConfiguration(endpoint!!, HttpMethod.GET)
        val cache = ConfigurationCache(remoteConfig)
        cache.clearCache(context)
        val bundle = ConfigurationBundle("namespace")
        bundle.networkConfiguration = NetworkConfiguration("endpoint")
        val cached =
            FetchedConfigurationBundle("http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-0-0")
        cached.configurationVersion = 1
        cached.configurationBundle = listOf(bundle)
        cache.writeCache(context, cached)

        // test
        val expectation = Any() as Object
        val provider = ConfigurationProvider(remoteConfig)
        val i = intArrayOf(0) // Needed to make it accessible inside the closure.
        provider.retrieveConfiguration(
            context,
            false
        ) { pair: Pair<FetchedConfigurationBundle, ConfigurationState> ->
            val fetchedConfigurationBundle = pair.first
            Assert.assertEquals(ConfigurationState.CACHED, pair.second)
            if (i[0] == 1 || fetchedConfigurationBundle.schema == "http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-1-0") {
                Assert.fail()
            }
            if (i[0] == 0 && fetchedConfigurationBundle.schema == "http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-0-0") {
                i[0]++
            }
        }
        synchronized(expectation) { expectation.wait(5000) }
        Assert.assertEquals(1, i[0])
        mockWebServer.shutdown()
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun testConfigurationProvider_downloadHigherConfigVersionThanCached_doUpdate() {
        // prepare test
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val mockWebServer = getMockServer(
            200,
            "{\"\$schema\":\"http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-1-0\",\"configurationVersion\":2,\"configurationBundle\":[]}"
        )
        val endpoint = getMockServerURI(mockWebServer)
        val remoteConfig = RemoteConfiguration(endpoint!!, HttpMethod.GET)
        val cache = ConfigurationCache(remoteConfig)
        cache.clearCache(context)
        val bundle = ConfigurationBundle("namespace")
        bundle.networkConfiguration = NetworkConfiguration("endpoint")
        val cached =
            FetchedConfigurationBundle("http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-0-0")
        cached.configurationVersion = 1
        cached.configurationBundle = listOf(bundle)
        cache.writeCache(context, cached)

        // test
        val expectation = Any() as Object
        val provider = ConfigurationProvider(remoteConfig)
        val i = intArrayOf(0) // Needed to make it accessible inside the closure.
        provider.retrieveConfiguration(
            context,
            false
        ) { pair: Pair<FetchedConfigurationBundle, ConfigurationState> ->
            val fetchedConfigurationBundle = pair.first
            Assert.assertEquals(
                if (i[0] == 0) ConfigurationState.CACHED else ConfigurationState.FETCHED,
                pair.second
            )
            if (i[0] == 1 || fetchedConfigurationBundle.schema == "http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-1-0") {
                i[0]++
            }
            if (i[0] == 0 && fetchedConfigurationBundle.schema == "http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-0-0") {
                i[0]++
            }
        }
        synchronized(expectation) { expectation.wait(10000) }
        Assert.assertEquals(2, i[0])
        mockWebServer.shutdown()
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun testConfigurationProvider_justRefresh_downloadSameConfigVersionThanCached_dontUpdate() {
        // prepare test
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val mockWebServer = getMockServer(404, "{}")
        val endpoint = getMockServerURI(mockWebServer)
        val remoteConfig = RemoteConfiguration(endpoint!!, HttpMethod.GET)
        val cache = ConfigurationCache(remoteConfig)
        cache.clearCache(context)
        val bundle = ConfigurationBundle("namespace")
        bundle.networkConfiguration = NetworkConfiguration("endpoint")
        val cached =
            FetchedConfigurationBundle("http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-0-0")
        cached.configurationVersion = 1
        cached.configurationBundle = listOf(bundle)
        cache.writeCache(context, cached)
        val expectation = Any() as Object
        val provider = ConfigurationProvider(remoteConfig)
        val i = intArrayOf(0) // Needed to make it accessible inside the closure.
        provider.retrieveConfiguration(
            context,
            false
        ) { pair: Pair<FetchedConfigurationBundle, ConfigurationState> ->
            Assert.assertEquals(ConfigurationState.CACHED, pair.second)
            synchronized(expectation) { expectation.notify() }
        }
        synchronized(expectation) { expectation.wait(5000) }
        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"\$schema\":\"http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-1-0\",\"configurationVersion\":1,\"configurationBundle\":[]}")
        mockWebServer.enqueue(mockResponse)

        // test
        val expectation2 = Any() as Object
        provider.retrieveConfiguration(
            context,
            true
        ) { Assert.fail() }
        synchronized(expectation2) { expectation2.wait(5000) }
        mockWebServer.shutdown()
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun testConfigurationProvider_justRefresh_downloadHigherConfigVersionThanCached_doUpdate() {
        // prepare test
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val mockWebServer = getMockServer(404, "{}")
        val endpoint = getMockServerURI(mockWebServer)
        val remoteConfig = RemoteConfiguration(endpoint!!, HttpMethod.GET)
        val cache = ConfigurationCache(remoteConfig)
        cache.clearCache(context)
        val bundle = ConfigurationBundle("namespace")
        bundle.networkConfiguration = NetworkConfiguration("endpoint")
        val cached =
            FetchedConfigurationBundle("http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-0-0")
        cached.configurationVersion = 1
        cached.configurationBundle = listOf(bundle)
        cache.writeCache(context, cached)
        val expectation = Any() as Object
        val provider = ConfigurationProvider(remoteConfig)
        val i = intArrayOf(0) // Needed to make it accessible inside the closure.
        provider.retrieveConfiguration(
            context,
            false
        ) {
            synchronized(expectation) { expectation.notify() }
        }
        synchronized(expectation) { expectation.wait(5000) }
        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"\$schema\":\"http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-1-0\",\"configurationVersion\":2,\"configurationBundle\":[]}")
        mockWebServer.enqueue(mockResponse)

        // test
        val expectation2 = Any() as Object
        val j = intArrayOf(0) // Needed to make it accessible inside the closure.
        provider.retrieveConfiguration(
            context,
            true
        ) { pair: Pair<FetchedConfigurationBundle, ConfigurationState> ->
            val fetchedConfigurationBundle = pair.first
            if (fetchedConfigurationBundle.schema == "http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-1-0") {
                j[0]++
                Assert.assertEquals(ConfigurationState.FETCHED, pair.second)
                synchronized(expectation2) { expectation2.notify() }
            }
        }
        synchronized(expectation2) { expectation2.wait(5000) }
        Assert.assertEquals(1, j[0])
        mockWebServer.shutdown()
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun testDoesntUseCachedConfigurationIfDifferentRemoteEndpoint() {
        // prepare test
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val cachedRemoteConfig = RemoteConfiguration("http://cache.example.com", HttpMethod.GET)
        val cache = ConfigurationCache(cachedRemoteConfig)
        cache.clearCache(context)

        // write configuration (version 2) to cache
        val bundle = ConfigurationBundle("namespace")
        bundle.networkConfiguration = NetworkConfiguration("endpoint")
        val cached =
            FetchedConfigurationBundle("http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-0-0")
        cached.configurationVersion = 2
        cached.configurationBundle = listOf(bundle)
        cache.writeCache(context, cached)

        // stub request for configuration (return version 1)
        val mockWebServer = getMockServer(
            200,
            "{\"\$schema\":\"http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-1-0\",\"configurationVersion\":1,\"configurationBundle\":[]}"
        )
        val endpoint = getMockServerURI(mockWebServer)

        // retrieve remote configuration
        val remoteConfig = RemoteConfiguration(endpoint!!, HttpMethod.GET)
        val provider = ConfigurationProvider(remoteConfig)
        val numCallbackCalls = intArrayOf(0)
        val expectation = Any() as Object
        provider.retrieveConfiguration(
            context,
            true
        ) { pair: Pair<FetchedConfigurationBundle, ConfigurationState> ->
            val fetchedConfigurationBundle = pair.first
            numCallbackCalls[0]++
            // should be the non-cache configuration (version 1)
            Assert.assertEquals(
                "http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-1-0",
                fetchedConfigurationBundle.schema
            )
            Assert.assertEquals(1, fetchedConfigurationBundle.configurationVersion.toLong())
        }
        synchronized(expectation) { expectation.wait(5000) }
        Assert.assertEquals(1, numCallbackCalls[0])
        mockWebServer.shutdown()
    }

    // Private methods
    @Throws(IOException::class)
    fun getMockServer(responseCode: Int, body: String?): MockWebServer {
        val mockServer = MockWebServer()
        mockServer.start()
        val mockResponse = MockResponse()
            .setResponseCode(responseCode)
            .setHeader("Content-Type", "application/json")
            .setBody(body!!)
        mockServer.enqueue(mockResponse)
        return mockServer
    }

    @SuppressLint("DefaultLocale")
    fun getMockServerURI(mockServer: MockWebServer?): String? {
        return if (mockServer != null) {
            String.format("http://%s:%d", mockServer.hostName, mockServer.port)
        } else null
    }
}
