package com.snowplowanalytics.snowplow.internal.remoteconfiguration;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.core.util.Pair;
import androidx.test.espresso.core.internal.deps.guava.collect.Lists;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration;
import com.snowplowanalytics.snowplow.configuration.RemoteConfiguration;
import com.snowplowanalytics.snowplow.configuration.SessionConfiguration;
import com.snowplowanalytics.snowplow.configuration.SubjectConfiguration;
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration;
import com.snowplowanalytics.snowplow.network.HttpMethod;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class RemoteConfigurationTest {

    @Test
    public void testJSONToConfigurations() throws JSONException {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String config = "{\"$schema\":\"http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-0-0\","
                + "\"configurationVersion\":12,\"configurationBundle\": ["
                + "{\"namespace\": \"default1\","
                + "\"networkConfiguration\": {\"endpoint\":\"https://fake.snowplow.io\",\"method\":\"get\"},"
                + "\"trackerConfiguration\": {\"applicationContext\":false,\"screenContext\":false},"
                + "\"sessionConfiguration\": {\"backgroundTimeout\":60,\"foregroundTimeout\":60}"
                + "},"
                + "{\"namespace\": \"default2\","
                + "\"subjectConfiguration\": {\"userId\":\"testUserId\"}"
                + "}"
                + "]}";
        JSONObject json = new JSONObject(config);

        FetchedConfigurationBundle fetchedConfigurationBundle = new FetchedConfigurationBundle(context, json);
        assertEquals("http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-0-0",
                fetchedConfigurationBundle.schema);

        assertEquals(12, fetchedConfigurationBundle.configurationVersion);
        assertEquals(2, fetchedConfigurationBundle.configurationBundle.size());

        // Regular setup
        ConfigurationBundle configurationBundle = fetchedConfigurationBundle.configurationBundle.get(0);
        assertEquals("default1", configurationBundle.namespace);
        assertNotNull(configurationBundle.networkConfiguration);
        assertNotNull(configurationBundle.trackerConfiguration);
        assertNotNull(configurationBundle.sessionConfiguration);
        assertNull(configurationBundle.subjectConfiguration);
        NetworkConfiguration networkConfiguration = configurationBundle.networkConfiguration;
        assertEquals(HttpMethod.GET, networkConfiguration.getMethod());
        TrackerConfiguration trackerConfiguration = configurationBundle.trackerConfiguration;
        assertFalse(trackerConfiguration.applicationContext);
        SessionConfiguration sessionConfiguration = configurationBundle.sessionConfiguration;
        assertEquals(60, sessionConfiguration.foregroundTimeout.convert(TimeUnit.SECONDS));

        // Regular setup without NetworkConfiguration
        configurationBundle = fetchedConfigurationBundle.configurationBundle.get(1);
        assertEquals("default2", configurationBundle.namespace);
        assertNull(configurationBundle.networkConfiguration);
        assertNotNull(configurationBundle.subjectConfiguration);
        SubjectConfiguration subjectConfiguration = configurationBundle.subjectConfiguration;
        assertEquals("testUserId", subjectConfiguration.userId);
    }

    @Test
    public void testDownloadConfiguration() throws IOException, InterruptedException {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String body = "{\"$schema\":\"http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-0-0\",\"configurationVersion\":12,\"configurationBundle\":[]}";
        MockWebServer mockWebServer = getMockServer(200, body);
        String endpoint = getMockServerURI(mockWebServer);
        final Object expectation = new Object();

        RemoteConfiguration remoteConfig = new RemoteConfiguration(endpoint, HttpMethod.GET);
        new ConfigurationFetcher(context, remoteConfig, fetchedConfigurationBundle -> {
            assertNotNull(fetchedConfigurationBundle);
            assertEquals("http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-0-0", fetchedConfigurationBundle.schema);
            synchronized (expectation) {
                expectation.notify();
            }
        });

        synchronized (expectation) {
            expectation.wait(10000);
        }
        mockWebServer.shutdown();
    }

    @Test
    public void testConfigurationCache() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        ConfigurationBundle bundle = new ConfigurationBundle("test");
        bundle.networkConfiguration = new NetworkConfiguration("endpoint");
        FetchedConfigurationBundle expected = new FetchedConfigurationBundle("http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-0-0");
        expected.configurationVersion = 12;
        expected.configurationBundle = Lists.newArrayList(bundle);

        RemoteConfiguration remoteConfiguration = new RemoteConfiguration("http://example.com", HttpMethod.GET);
        ConfigurationCache cache = new ConfigurationCache(remoteConfiguration);
        cache.clearCache(context);
        cache.writeCache(context, expected);

        cache = new ConfigurationCache(remoteConfiguration);
        FetchedConfigurationBundle config = cache.readCache(context);

        assertEquals(expected.configurationVersion, config.configurationVersion);
        assertEquals(expected.schema, config.schema);
        assertEquals(expected.configurationBundle.size(), config.configurationBundle.size());
        ConfigurationBundle expectedBundle = expected.configurationBundle.get(0);
        ConfigurationBundle configBundle = config.configurationBundle.get(0);
        assertEquals(expectedBundle.networkConfiguration.getEndpoint(), configBundle.networkConfiguration.getEndpoint());
        assertNull(configBundle.trackerConfiguration);
    }

    @Test
    public void testConfigurationFetcher_downloads() throws IOException, InterruptedException {
        // prepare test
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        MockWebServer mockWebServer = getMockServer(200, "{\"$schema\":\"http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/2-0-0\",\"configurationVersion\":12,\"configurationBundle\":[]}");
        String endpoint = getMockServerURI(mockWebServer);

        // test
        final Object expectation = new Object();
        AtomicBoolean expectationNotified = new AtomicBoolean(false);
        RemoteConfiguration remoteConfig = new RemoteConfiguration(endpoint, HttpMethod.GET);
        new ConfigurationFetcher(context, remoteConfig, fetchedConfigurationBundle -> {
            expectationNotified.set(true);
            synchronized (expectation) {
                expectation.notify();
            }
        });
        synchronized (expectation) {
            expectation.wait(5000);
        }
        assertTrue(expectationNotified.get());
        mockWebServer.shutdown();
    }

    @Test
    public void testConfigurationProvider_notDownloading_fails() throws IOException, InterruptedException {
        // prepare test
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        MockWebServer mockWebServer = getMockServer(500, "{}");
        String endpoint = getMockServerURI(mockWebServer);
        RemoteConfiguration remoteConfig = new RemoteConfiguration(endpoint, HttpMethod.GET);
        ConfigurationCache cache = new ConfigurationCache(remoteConfig);
        cache.clearCache(context);

        // test
        final Object expectation = new Object();
        ConfigurationProvider provider = new ConfigurationProvider(remoteConfig);
        provider.retrieveConfiguration(context, false, pair -> fail());
        synchronized (expectation) {
            expectation.wait(5000);
        }
        mockWebServer.shutdown();
    }

    @Test
    public void testConfigurationProvider_downloadOfWrongSchema_fails() throws IOException, InterruptedException {
        // prepare test
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        MockWebServer mockWebServer = getMockServer(200, "{\"$schema\":\"http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-0-0\",\"configurationVersion\":12,\"configurationBundle\":[]}");
        String endpoint = getMockServerURI(mockWebServer);
        RemoteConfiguration remoteConfig = new RemoteConfiguration(endpoint, HttpMethod.GET);
        ConfigurationCache cache = new ConfigurationCache(remoteConfig);
        cache.clearCache(context);

        // test
        final Object expectation = new Object();
        ConfigurationProvider provider = new ConfigurationProvider(remoteConfig);
        provider.retrieveConfiguration(context, false, pair -> fail());
        synchronized (expectation) {
            expectation.wait(5000);
        }
        mockWebServer.shutdown();
    }

    @Test
    public void testConfigurationProvider_downloadSameConfigVersionThanCached_dontUpdate() throws IOException, InterruptedException {
        // prepare test
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        MockWebServer mockWebServer = getMockServer(200, "{\"$schema\":\"http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-1-0\",\"configurationVersion\":1,\"configurationBundle\":[]}");
        String endpoint = getMockServerURI(mockWebServer);
        RemoteConfiguration remoteConfig = new RemoteConfiguration(endpoint, HttpMethod.GET);
        ConfigurationCache cache = new ConfigurationCache(remoteConfig);
        cache.clearCache(context);

        ConfigurationBundle bundle = new ConfigurationBundle("namespace");
        bundle.networkConfiguration = new NetworkConfiguration("endpoint");
        FetchedConfigurationBundle cached = new FetchedConfigurationBundle("http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-0-0");
        cached.configurationVersion = 1;
        cached.configurationBundle = Lists.newArrayList(bundle);
        cache.writeCache(context, cached);

        // test
        final Object expectation = new Object();
        ConfigurationProvider provider = new ConfigurationProvider(remoteConfig);
        final int[] i = {0}; // Needed to make it accessible inside the closure.
        provider.retrieveConfiguration(context, false, pair -> {
            FetchedConfigurationBundle fetchedConfigurationBundle = pair.first;
            assertEquals(ConfigurationState.CACHED, pair.second);
            if (i[0] == 1 || fetchedConfigurationBundle.schema.equals("http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-1-0")) {
                fail();
            }
            if (i[0] == 0 && fetchedConfigurationBundle.schema.equals("http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-0-0")) {
                i[0]++;
            }
        });
        synchronized (expectation) {
            expectation.wait(5000);
        }
        assertEquals(1, i[0]);
        mockWebServer.shutdown();
    }

    @Test
    public void testConfigurationProvider_downloadHigherConfigVersionThanCached_doUpdate() throws IOException, InterruptedException {
        // prepare test
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        MockWebServer mockWebServer = getMockServer(200, "{\"$schema\":\"http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-1-0\",\"configurationVersion\":2,\"configurationBundle\":[]}");
        String endpoint = getMockServerURI(mockWebServer);
        RemoteConfiguration remoteConfig = new RemoteConfiguration(endpoint, HttpMethod.GET);
        ConfigurationCache cache = new ConfigurationCache(remoteConfig);
        cache.clearCache(context);

        ConfigurationBundle bundle = new ConfigurationBundle("namespace");
        bundle.networkConfiguration = new NetworkConfiguration("endpoint");
        FetchedConfigurationBundle cached = new FetchedConfigurationBundle("http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-0-0");
        cached.configurationVersion = 1;
        cached.configurationBundle = Lists.newArrayList(bundle);
        cache.writeCache(context, cached);

        // test
        final Object expectation = new Object();
        ConfigurationProvider provider = new ConfigurationProvider(remoteConfig);
        final int[] i = {0}; // Needed to make it accessible inside the closure.
        provider.retrieveConfiguration(context, false, pair -> {
            FetchedConfigurationBundle fetchedConfigurationBundle = pair.first;
            assertEquals(
                    i[0] == 0 ? ConfigurationState.CACHED : ConfigurationState.FETCHED,
                    pair.second
            );
            if (i[0] == 1 || fetchedConfigurationBundle.schema.equals("http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-1-0")) {
                i[0]++;
            }
            if (i[0] == 0 && fetchedConfigurationBundle.schema.equals("http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-0-0")) {
                i[0]++;
            }
        });
        synchronized (expectation) {
            expectation.wait(10000);
        }
        assertEquals(2, i[0]);
        mockWebServer.shutdown();
    }

    @Test
    public void testConfigurationProvider_justRefresh_downloadSameConfigVersionThanCached_dontUpdate() throws IOException, InterruptedException {
        // prepare test
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        MockWebServer mockWebServer = getMockServer(404, "{}");
        String endpoint = getMockServerURI(mockWebServer);
        RemoteConfiguration remoteConfig = new RemoteConfiguration(endpoint, HttpMethod.GET);
        ConfigurationCache cache = new ConfigurationCache(remoteConfig);
        cache.clearCache(context);

        ConfigurationBundle bundle = new ConfigurationBundle("namespace");
        bundle.networkConfiguration = new NetworkConfiguration("endpoint");
        FetchedConfigurationBundle cached = new FetchedConfigurationBundle("http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-0-0");
        cached.configurationVersion = 1;
        cached.configurationBundle = Lists.newArrayList(bundle);
        cache.writeCache(context, cached);

        final Object expectation = new Object();
        ConfigurationProvider provider = new ConfigurationProvider(remoteConfig);
        final int[] i = {0}; // Needed to make it accessible inside the closure.
        provider.retrieveConfiguration(context, false, pair -> {
            assertEquals(ConfigurationState.CACHED, pair.second);
            synchronized (expectation) {
                expectation.notify();
            }
        });
        synchronized (expectation) {
            expectation.wait(5000);
        }

        MockResponse mockResponse = new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"$schema\":\"http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-1-0\",\"configurationVersion\":1,\"configurationBundle\":[]}");
        mockWebServer.enqueue(mockResponse);

        // test
        final Object expectation2 = new Object();
        provider.retrieveConfiguration(context, true, pair -> fail());
        synchronized (expectation2) {
            expectation2.wait(5000);
        }
        mockWebServer.shutdown();
    }

    @Test
    public void testConfigurationProvider_justRefresh_downloadHigherConfigVersionThanCached_doUpdate() throws IOException, InterruptedException {
        // prepare test
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        MockWebServer mockWebServer = getMockServer(404, "{}");
        String endpoint = getMockServerURI(mockWebServer);
        RemoteConfiguration remoteConfig = new RemoteConfiguration(endpoint, HttpMethod.GET);
        ConfigurationCache cache = new ConfigurationCache(remoteConfig);
        cache.clearCache(context);

        ConfigurationBundle bundle = new ConfigurationBundle("namespace");
        bundle.networkConfiguration = new NetworkConfiguration("endpoint");
        FetchedConfigurationBundle cached = new FetchedConfigurationBundle("http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-0-0");
        cached.configurationVersion = 1;
        cached.configurationBundle = Lists.newArrayList(bundle);
        cache.writeCache(context, cached);

        final Object expectation = new Object();
        ConfigurationProvider provider = new ConfigurationProvider(remoteConfig);
        final int[] i = {0}; // Needed to make it accessible inside the closure.
        provider.retrieveConfiguration(context, false, pair -> {
            synchronized (expectation) {
                expectation.notify();
            }
        });
        synchronized (expectation) {
            expectation.wait(5000);
        }

        MockResponse mockResponse = new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"$schema\":\"http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-1-0\",\"configurationVersion\":2,\"configurationBundle\":[]}");
        mockWebServer.enqueue(mockResponse);

        // test
        final Object expectation2 = new Object();
        final int[] j = {0}; // Needed to make it accessible inside the closure.
        provider.retrieveConfiguration(context, true, pair -> {
            FetchedConfigurationBundle fetchedConfigurationBundle = pair.first;
            if (fetchedConfigurationBundle.schema.equals("http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-1-0")) {
                j[0]++;
                assertEquals(ConfigurationState.FETCHED, pair.second);
                synchronized (expectation2) {
                    expectation2.notify();
                }
            }
        });
        synchronized (expectation2) {
            expectation2.wait(5000);
        }
        assertEquals(1, j[0]);
        mockWebServer.shutdown();
    }

    @Test
    public void testDoesntUseCachedConfigurationIfDifferentRemoteEndpoint() throws IOException, InterruptedException {
        // prepare test
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteConfiguration cachedRemoteConfig = new RemoteConfiguration("http://cache.example.com", HttpMethod.GET);
        ConfigurationCache cache = new ConfigurationCache(cachedRemoteConfig);
        cache.clearCache(context);

        // write configuration (version 2) to cache
        ConfigurationBundle bundle = new ConfigurationBundle("namespace");
        bundle.networkConfiguration = new NetworkConfiguration("endpoint");
        FetchedConfigurationBundle cached = new FetchedConfigurationBundle("http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-0-0");
        cached.configurationVersion = 2;
        cached.configurationBundle = Lists.newArrayList(bundle);
        cache.writeCache(context, cached);

        // stub request for configuration (return version 1)
        MockWebServer mockWebServer = getMockServer(200, "{\"$schema\":\"http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-1-0\",\"configurationVersion\":1,\"configurationBundle\":[]}");
        String endpoint = getMockServerURI(mockWebServer);

        // retrieve remote configuration
        RemoteConfiguration remoteConfig = new RemoteConfiguration(endpoint, HttpMethod.GET);
        ConfigurationProvider provider = new ConfigurationProvider(remoteConfig);
        final int[] numCallbackCalls = {0};
        final Object expectation = new Object();
        provider.retrieveConfiguration(context, true, pair -> {
            FetchedConfigurationBundle fetchedConfigurationBundle = pair.first;
            numCallbackCalls[0]++;
            // should be the non-cache configuration (version 1)
            assertEquals("http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-1-0", fetchedConfigurationBundle.schema);
            assertEquals(1, fetchedConfigurationBundle.configurationVersion);
        });
        synchronized (expectation) {
            expectation.wait(5000);
        }
        assertEquals(1, numCallbackCalls[0]);
        mockWebServer.shutdown();
    }

    // Private methods

    public MockWebServer getMockServer(int responseCode, String body) throws IOException {
        MockWebServer mockServer = new MockWebServer();
        mockServer.start();
        MockResponse mockResponse = new MockResponse()
                .setResponseCode(responseCode)
                .setHeader("Content-Type", "application/json")
                .setBody(body);
        mockServer.enqueue(mockResponse);
        return mockServer;
    }

    @Nullable
    @SuppressLint("DefaultLocale")
    public String getMockServerURI(MockWebServer mockServer) {
        if (mockServer != null) {
            return String.format("http://%s:%d", mockServer.getHostName(), mockServer.getPort());
        }
        return null;
    }
}
