/*
 * Copyright (c) 2015 Snowplow Analytics Ltd. All rights reserved.
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

package com.snowplowanalytics.snowplow.tracker.rx;

import com.snowplowanalytics.snowplow.tracker.emitter.BufferOption;
import com.snowplowanalytics.snowplow.tracker.emitter.RequestSecurity;
import com.snowplowanalytics.snowplow.tracker.emitter.HttpMethod;
import com.snowplowanalytics.snowplow.tracker.Tracker;

import com.snowplowanalytics.snowplow.tracker.rx.utils.LogFetcher;

public class EventSendingTest extends SnowplowRxTestCase {

    // Tests

    public void testSendGet() throws Exception {
        setup();

        // Setup the Tracker
        com.snowplowanalytics.snowplow.tracker.Emitter emitter = getEmitter(
            HttpMethod.GET, BufferOption.Single, RequestSecurity.HTTP);
        emitter.getEventStore().removeAllEvents();
        Tracker tracker = getTracker(emitter, getSubject());

        trackStructuredEvent(tracker);
        trackUnstructuredEvent(tracker);
        trackPageView(tracker);
        trackTimings(tracker);
        trackScreenView(tracker);
        trackEcommerceEvent(tracker);

        // Wait for emitter to start
        while (!tracker.getEmitter().getEmitterStatus()) {
            Thread.sleep(500);
        }

        // Wait for emitter to end
        while (tracker.getEmitter().getEmitterStatus()) {
            Thread.sleep(500);
        }
        Thread.sleep(500);

        checkGetRequest(LogFetcher.getMountebankGetRequests());
        tracker.shutdown();
    }

    public void testSendPost() throws Exception {
        setup();

        // Setup the Tracker
        com.snowplowanalytics.snowplow.tracker.Emitter emitter = getEmitter(
                HttpMethod.POST, BufferOption.Single, RequestSecurity.HTTP);
        emitter.getEventStore().removeAllEvents();
        Tracker tracker = getTracker(emitter, getSubject());

        trackStructuredEvent(tracker);
        trackUnstructuredEvent(tracker);
        trackPageView(tracker);
        trackTimings(tracker);
        trackScreenView(tracker);
        trackEcommerceEvent(tracker);

        // Wait for emitter to start
        while (!tracker.getEmitter().getEmitterStatus()) {
            Thread.sleep(500);
        }

        // Wait for emitter to end
        while (tracker.getEmitter().getEmitterStatus()) {
            Thread.sleep(500);
        }
        Thread.sleep(500);

        checkPostRequest(LogFetcher.getMountebankPostRequests());
        tracker.shutdown();
    }
}
