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

package com.snowplowanalytics.snowplow.tracker.events;

import android.test.AndroidTestCase;

import com.snowplowanalytics.snowplow.tracker.constants.Parameters;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

public class ConsentGrantedTest extends AndroidTestCase {

    public void testExpectedForm() {
        ConsentGranted event = ConsentGranted.builder()
                .documentName("name")
                .documentDescription("description")
                .documentId("id")
                .documentVersion("v1.0")
                .expiry("expiration")
                .build();

        Map<String, Object> data = event.getData().getMap();

        assertNotNull(data);
        assertEquals("expiration", data.get(Parameters.CG_EXPIRY));

        List<ConsentDocument> documents = new LinkedList<>();
        documents.add(ConsentDocument.builder()
                .documentDescription("granted context desc 1")
                .documentId("granted context id 1")
                .documentName("granted context name 1")
                .documentVersion("granted context version 1")
                .build());
        documents.add(ConsentDocument.builder()
                .documentDescription("granted context desc 2")
                .documentId("granted context id 2")
                .documentName("granted context name 2")
                .documentVersion("granted context version 2")
                .build());

        event = ConsentGranted.builder()
                .documentName("name")
                .documentDescription("description")
                .documentId("id")
                .documentVersion("v1.0")
                .expiry("expiration")
                .consentDocuments(documents)
                .build();

        data = event.getData().getMap();

        assertNotNull(data);
        assertEquals("expiration", data.get(Parameters.CG_EXPIRY));
    }

    public void testBuilderFailures() {
        boolean exception = false;
        try {
            ConsentGranted.builder().build();
        } catch (Exception e) {
            assertEquals(null, e.getMessage());
            exception = true;
        }
        assertTrue(exception);

        exception = false;
        try {
            ConsentGranted.builder().documentId("").documentVersion("test").expiry("").build();
        } catch (Exception e) {
            assertEquals("Expiry cannot be empty", e.getMessage());
            exception = true;
        }
        assertTrue(exception);

        exception = false;
        try {
            ConsentGranted.builder().documentId("").documentVersion("test").expiry("test").build();
        } catch (Exception e) {
            assertEquals("Document ID cannot be empty", e.getMessage());
            exception = true;
        }
        assertTrue(exception);

        exception = false;
        try {
            ConsentGranted.builder().documentId("test").documentVersion("").expiry("test").build();
        } catch (Exception e) {
            assertEquals("Document version cannot be empty", e.getMessage());
            exception = true;
        }
        assertTrue(exception);
    }
}
