package com.snowplowanalytics.snowplow.internal.remoteconfiguration;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration;
import com.snowplowanalytics.snowplow.configuration.SessionConfiguration;
import com.snowplowanalytics.snowplow.configuration.SubjectConfiguration;
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration;
import com.snowplowanalytics.snowplow.network.HttpMethod;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class RemoteConfigurationTest {

    @Test
    public void testJSONToConfigurations() throws JSONException {
        String config = "{\"formatVersion\":\"1.2\",\"configurationVersion\":12,\"configurationBundle\": ["
                + "{\"namespace\": \"default1\","
                + "\"networkConfiguration\": {\"endpoint\":\"https://fake.snowplowanalytics.com\",\"method\":\"get\"},"
                + "\"trackerConfiguration\": {\"applicationContext\":false,\"screenContext\":false},"
                + "\"sessionConfiguration\": {\"backgroundTimeout\":60,\"foregroundTimeout\":60}"
                + "},"
                + "{\"namespace\": \"default2\","
                + "\"subjectConfiguration\": {\"userId\":\"testUserId\"}"
                + "}"
                + "]}";
        JSONObject json = new JSONObject(config);

        FetchedConfigurationBundle fetchedConfigurationBundle = new FetchedConfigurationBundle(json);
        assertEquals("1.2", fetchedConfigurationBundle.formatVersion);
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
}
