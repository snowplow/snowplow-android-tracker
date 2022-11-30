/*
 * Copyright (c) 2015-2022 Snowplow Analytics Ltd. All rights reserved.
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

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.snowplowanalytics.core.constants.Parameters;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class PageViewTest {

    @Test
    public void testExpectedForm() {
        PageView pageView = new PageView("http://com.acme/foo/bar");

        assertEquals("pv", pageView.getName());

        Map data = pageView.getDataPayload();
        assertNotNull(data);
        assertEquals("http://com.acme/foo/bar", data.get(Parameters.PAGE_URL));
        assertFalse(data.containsKey(Parameters.PAGE_TITLE));
        assertFalse(data.containsKey(Parameters.PAGE_REFR));

        pageView = new PageView("http://com.acme/foo/bar")
                .pageTitle("Page Title")
                .referrer("http://refr.com");

        data = pageView.getDataPayload();

        assertNotNull(data);
        assertEquals("http://com.acme/foo/bar", data.get(Parameters.PAGE_URL));
        assertEquals("Page Title", data.get(Parameters.PAGE_TITLE));
        assertEquals("http://refr.com", data.get(Parameters.PAGE_REFR));
    }

    @Test
    public void testBuilderFailures() {
        boolean exception = false;
        try {
            new PageView("");
        } catch (Exception e) {
            assertEquals("pageUrl cannot be empty", e.getMessage());
            exception = true;
        }
        assertTrue(exception);
    }
}
