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

import com.snowplowanalytics.snowplow.tracker.BufferOption;
import com.snowplowanalytics.snowplow.tracker.HttpMethod;
import com.snowplowanalytics.snowplow.tracker.RequestSecurity;
import com.snowplowanalytics.snowplow.tracker.Tracker;

import com.snowplowanalytics.snowplow.tracker.classic.utils.LogFetcher;

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

        Thread.sleep(15000);

        tracker.getEmitter().flush();

        Thread.sleep(5000);

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
        trackUnstructuredEvent(tracker);
        trackPageView(tracker);
        trackTimings(tracker);
        trackScreenView(tracker);
        trackEcommerceEvent(tracker);

        Thread.sleep(15000);

        tracker.getEmitter().flush();

        Thread.sleep(5000);

        checkPostRequest(LogFetcher.getMountebankPostRequests());
    }
}
