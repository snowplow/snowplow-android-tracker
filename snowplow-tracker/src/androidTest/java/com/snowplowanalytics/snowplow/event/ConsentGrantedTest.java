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

import android.test.AndroidTestCase;

import com.snowplowanalytics.snowplow.internal.constants.Parameters;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ConsentGrantedTest extends AndroidTestCase {

    public void testExpectedForm() {
        ConsentGranted event = new ConsentGranted("expiration", "id", "v1.0")
                .documentName("name")
                .documentDescription("description");

        Map<String, Object> data = event.getDataPayload();

        assertNotNull(data);
        assertEquals("expiration", data.get(Parameters.CG_EXPIRY));

        List<ConsentDocument> documents = new LinkedList<>();
        documents.add(new ConsentDocument("granted context id 1", "granted context version 1")
                .documentDescription("granted context desc 1")
                .documentName("granted context name 1"));
        documents.add(new ConsentDocument("granted context id 2", "granted context version 2")
                .documentDescription("granted context desc 2")
                .documentName("granted context name 2"));

        event = new ConsentGranted("expiration", "id", "v1.0")
                .documentName("name")
                .documentDescription("description")
                .documents(documents);

        data = event.getDataPayload();

        assertNotNull(data);
        assertEquals("expiration", data.get(Parameters.CG_EXPIRY));
    }

    public void testBuilderFailures() {
        boolean exception = false;
        try {
            new ConsentGranted(null, null, null);
        } catch (Exception e) {
            assertEquals(null, e.getMessage());
            exception = true;
        }
        assertTrue(exception);

        exception = false;
        try {
            new ConsentGranted("", "", "test");
        } catch (Exception e) {
            assertEquals("Expiry cannot be empty", e.getMessage());
            exception = true;
        }
        assertTrue(exception);

        exception = false;
        try {
            new ConsentGranted("test", "", "test");
        } catch (Exception e) {
            assertEquals("Document ID cannot be empty", e.getMessage());
            exception = true;
        }
        assertTrue(exception);

        exception = false;
        try {
            new ConsentGranted("test", "test", "");
        } catch (Exception e) {
            assertEquals("Document version cannot be empty", e.getMessage());
            exception = true;
        }
        assertTrue(exception);
    }
}
