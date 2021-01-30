package com.snowplowanalytics.snowplow.configuration;


import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.snowplowanalytics.snowplow.controller.TrackerController;
import com.snowplowanalytics.snowplow.internal.tracker.ServiceProvider;
import com.snowplowanalytics.snowplow.network.HttpMethod;
import com.snowplowanalytics.snowplow.network.Protocol;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class ConfigurationTest {

    // Tests

    @Test
    public void basicInitialization() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        NetworkConfiguration networkConfiguration = new NetworkConfiguration("fake-url", Protocol.HTTPS, HttpMethod.POST);
        TrackerConfiguration trackerConfiguration = new TrackerConfiguration("namespace", "appid");
        trackerConfiguration.platformContext = true;
        TrackerController tracker = ServiceProvider.setup(context, networkConfiguration, trackerConfiguration);

        assertNotNull(tracker);
        URI uri = URI.create(tracker.getNetwork().getEndpoint());
        assertNotNull(uri);
        String host = uri.getHost();
        String scheme = uri.getScheme();

        String protocol = networkConfiguration.getProtocol() == Protocol.HTTP ? "http" : "https";

        assertEquals(networkConfiguration.getEndpoint(), host);
        assertEquals(protocol, scheme);
        assertEquals(trackerConfiguration.appId, tracker.getAppId());
        assertEquals(trackerConfiguration.namespace, tracker.getNamespace());
    }

}
