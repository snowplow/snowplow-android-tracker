/*
 * Copyright (c) 2015-2020 Snowplow Analytics Ltd. All rights reserved.
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

package com.snowplowanalytics.snowplow.event;

import android.test.AndroidTestCase;

import com.snowplowanalytics.snowplow.internal.constants.Parameters;

import java.util.Map;

public class PageViewTest extends AndroidTestCase {

    public void testExpectedForm() {
        PageView pageView = PageView.builder()
                .pageUrl("http://com.acme/foo/bar")
                .build();

        assertEquals("pv", pageView.getName());

        Map data = pageView.getDataPayload();
        assertNotNull(data);
        assertEquals("http://com.acme/foo/bar", data.get(Parameters.PAGE_URL));
        assertFalse(data.containsKey(Parameters.PAGE_TITLE));
        assertFalse(data.containsKey(Parameters.PAGE_REFR));

        pageView = PageView.builder()
                .pageUrl("http://com.acme/foo/bar")
                .pageTitle("Page Title")
                .referrer("http://refr.com")
                .build();

        data = pageView.getDataPayload();

        assertNotNull(data);
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
