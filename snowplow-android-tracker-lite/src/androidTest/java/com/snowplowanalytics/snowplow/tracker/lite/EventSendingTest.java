package com.snowplowanalytics.snowplow.tracker.lite;

import com.snowplowanalytics.snowplow.tracker.BufferOption;
import com.snowplowanalytics.snowplow.tracker.HttpMethod;
import com.snowplowanalytics.snowplow.tracker.RequestSecurity;
import com.snowplowanalytics.snowplow.tracker.Tracker;

import com.snowplowanalytics.snowplow.tracker.lite.utils.LogFetcher;

public class EventSendingTest extends SnowplowLiteTestCase {

    public void testSendGet() throws Exception {
        setup();

        // Setup the Tracker
        com.snowplowanalytics.snowplow.tracker.Emitter emitter = getEmitter(
                HttpMethod.GET, BufferOption.Single, RequestSecurity.HTTP);
        emitter.getEventStore().removeAllEvents();
        Tracker tracker = getTracker(emitter, getSubject());

        trackStructuredEvent(tracker);
        trackUnStructuredEvent(tracker);
        trackPageView(tracker);
        trackTimings(tracker);
        trackScreenView(tracker);
        trackEcommerceEvent(tracker);

        Thread.sleep(2000);

        checkGetRequest(LogFetcher.getMountebankGetRequests());

        tracker.getEmitter().shutdown();
    }

    public void testSendPost() throws Exception {
        setup();

        // Setup the Tracker
        com.snowplowanalytics.snowplow.tracker.Emitter emitter = getEmitter(
                HttpMethod.POST, BufferOption.Single, RequestSecurity.HTTP);
        emitter.getEventStore().removeAllEvents();
        Tracker tracker = getTracker(emitter, getSubject());

        trackStructuredEvent(tracker);
        trackUnStructuredEvent(tracker);
        trackPageView(tracker);
        trackTimings(tracker);
        trackScreenView(tracker);
        trackEcommerceEvent(tracker);

        Thread.sleep(2000);

        checkPostRequest(LogFetcher.getMountebankPostRequests());

        tracker.getEmitter().shutdown();
    }
}
