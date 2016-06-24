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

package com.snowplowanalytics.snowplow.tracker.events;

import android.test.AndroidTestCase;

import com.snowplowanalytics.snowplow.tracker.constants.Parameters;

import java.util.Map;

public class PageViewTest extends AndroidTestCase {

    public void testExpectedForm() {
        PageView pageView = PageView.builder()
                .pageUrl("http://com.acme/foo/bar")
                .build();

        Map data = pageView.getPayload().getMap();

        assertNotNull(data);
        assertEquals("pv", data.get(Parameters.EVENT));
        assertEquals("http://com.acme/foo/bar", data.get(Parameters.PAGE_URL));
        assertFalse(data.containsKey(Parameters.PAGE_TITLE));
        assertFalse(data.containsKey(Parameters.PAGE_REFR));

        pageView = PageView.builder()
                .pageUrl("http://com.acme/foo/bar")
                .pageTitle("Page Title")
                .referrer("http://refr.com")
                .build();

        data = pageView.getPayload().getMap();

        assertNotNull(data);
        assertEquals("pv", data.get(Parameters.EVENT));
        assertEquals("http://com.acme/foo/bar", data.get(Parameters.PAGE_URL));
        assertEquals("Page Title", data.get(Parameters.PAGE_TITLE));
        assertEquals("http://refr.com", data.get(Parameters.PAGE_REFR));
    }

    public void testBuilderFailures() {
        boolean exception = false;
        try {
            PageView.builder().build();
        } catch (Exception e) {
            assertEquals(null, e.getMessage());
            exception = true;
        }
        assertTrue(exception);

        exception = false;
        try {
            PageView.builder().pageUrl("").build();
        } catch (Exception e) {
            assertEquals("pageUrl cannot be empty", e.getMessage());
            exception = true;
        }
        assertTrue(exception);
    }
}
