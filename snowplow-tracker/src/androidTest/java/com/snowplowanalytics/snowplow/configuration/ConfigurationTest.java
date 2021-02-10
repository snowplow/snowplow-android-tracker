package com.snowplowanalytics.snowplow.configuration;

import android.content.Context;
import android.net.Network;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.snowplowanalytics.snowplow.controller.TrackerController;
import com.snowplowanalytics.snowplow.internal.tracker.ServiceProvider;
import com.snowplowanalytics.snowplow.internal.tracker.Tracker;
import com.snowplowanalytics.snowplow.network.HttpMethod;
import com.snowplowanalytics.snowplow.network.Protocol;
import com.snowplowanalytics.snowplow.util.TimeMeasure;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URI;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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

    @Test
    public void sessionInitialization() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        TimeMeasure expectedForeground = new TimeMeasure(42, TimeUnit.SECONDS);
        TimeMeasure expectedBackground = new TimeMeasure(24, TimeUnit.SECONDS);
        NetworkConfiguration networkConfig = new NetworkConfiguration("fake-url", Protocol.HTTPS, HttpMethod.POST);
        TrackerConfiguration trackerConfig = new TrackerConfiguration("namespace", "appId");
        SessionConfiguration sessionConfig = new SessionConfiguration(expectedForeground, expectedBackground);
        TrackerController tracker = Tracker.setup(context, networkConfig, trackerConfig, Collections.singletonList(sessionConfig));
        TimeMeasure foreground = tracker.getSession().getForegroundTimeout();
        TimeMeasure background = tracker.getSession().getBackgroundTimeout();
        assertEquals(expectedForeground, foreground);
        assertEquals(expectedBackground, background);
    }

    @Test
    public void sessionControllerUnavailableWhenContextTurnedOff() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        NetworkConfiguration networkConfiguration = new NetworkConfiguration("fake-url", Protocol.HTTPS, HttpMethod.POST);
        TrackerConfiguration trackerConfiguration = new TrackerConfiguration("namespace", "appid");
        trackerConfiguration.sessionContext = true;
        TrackerController tracker = ServiceProvider.setup(context, networkConfiguration, trackerConfiguration);
        assertNotNull(tracker.getSession());

        trackerConfiguration.sessionContext = false;
        tracker = ServiceProvider.setup(context, networkConfiguration, trackerConfiguration);
        assertNull(tracker.getSession());
    }
}
