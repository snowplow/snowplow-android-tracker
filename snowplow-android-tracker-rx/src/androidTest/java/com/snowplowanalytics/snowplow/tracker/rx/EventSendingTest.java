package com.snowplowanalytics.snowplow.tracker.rx;

import com.snowplowanalytics.snowplow.tracker.BufferOption;
import com.snowplowanalytics.snowplow.tracker.RequestSecurity;
import com.snowplowanalytics.snowplow.tracker.HttpMethod;
import com.snowplowanalytics.snowplow.tracker.Tracker;

import com.snowplowanalytics.snowplow.tracker.rx.utils.LogFetcher;

public class EventSendingTest extends SnowplowRxTestCase {

    // Tests

    public void testSendGet() throws Exception {
        setup();

        // Setup the Tracker
        com.snowplowanalytics.snowplow.tracker.Emitter emitter =
                getEmitter(HttpMethod.GET, BufferOption.Single, RequestSecurity.HTTP);
        emitter.getEventStore().removeAllEvents();
        Tracker tracker = getTracker(emitter, getSubject());

        trackStructuredEvent(tracker);
        trackUnStructuredEvent(tracker);
        trackPageView(tracker);
        trackTimings(tracker);
        trackScreenView(tracker);
        trackEcommerceEvent(tracker);

        // Wait for Tracker to shutdown
        while (tracker.getEmitter().getEmitterStatus()) {
            Thread.sleep(500);
        }
        Thread.sleep(500);

        checkGetRequest(LogFetcher.getMountebankGetRequests());
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

        // Wait for Tracker to shutdown
        while (tracker.getEmitter().getEmitterStatus()) {
            Thread.sleep(500);
        }
        Thread.sleep(500);

        checkPostRequest(LogFetcher.getMountebankPostRequests());
    }
}
