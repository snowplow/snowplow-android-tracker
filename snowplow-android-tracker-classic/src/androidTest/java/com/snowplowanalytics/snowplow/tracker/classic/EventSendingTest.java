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

package com.snowplowanalytics.snowplow.tracker.classic;

import com.snowplowanalytics.snowplow.tracker.emitter.BufferOption;
import com.snowplowanalytics.snowplow.tracker.emitter.HttpMethod;
import com.snowplowanalytics.snowplow.tracker.emitter.RequestSecurity;
import com.snowplowanalytics.snowplow.tracker.Tracker;

public class EventSendingTest extends SnowplowClassicTestCase {

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
        int counter = 0;
        while (!tracker.getEmitter().getEmitterStatus()) {
            Thread.sleep(500);
            counter++;
            if (counter > 10) {
                return;
            }
        }

        // Wait for emitter to end
        counter = 0;
        while (tracker.getEmitter().getEmitterStatus()) {
            Thread.sleep(500);
            counter++;
            if (counter > 10) {
                return;
            }
        }
        Thread.sleep(500);

        checkGetRequest(getRequests(28));
        tracker.pauseEventTracking();
        Executor.shutdown();
        tearDown();
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
        int counter = 0;
        while (!tracker.getEmitter().getEmitterStatus()) {
            Thread.sleep(500);
            counter++;
            if (counter > 10) {
                return;
            }
        }

        // Wait for emitter to end
        counter = 0;
        while (tracker.getEmitter().getEmitterStatus()) {
            Thread.sleep(500);
            counter++;
            if (counter > 10) {
                return;
            }
        }
        Thread.sleep(500);

        checkPostRequest(getRequests(28));
        tracker.pauseEventTracking();
        Executor.shutdown();
        tearDown();
    }
}
