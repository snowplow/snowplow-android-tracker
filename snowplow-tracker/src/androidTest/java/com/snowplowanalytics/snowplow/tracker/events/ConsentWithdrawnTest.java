/*
 * Copyright (c) 2015-2019 Snowplow Analytics Ltd. All rights reserved.
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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ConsentWithdrawnTest extends AndroidTestCase {

    public void testExpectedForm() {
        ConsentWithdrawn event = ConsentWithdrawn.builder()
                .documentName("name")
                .documentDescription("description")
                .documentId("id")
                .documentVersion("v1.0")
                .all(false)
                .build();

        Map data = event.getData().getMap();

        assertNotNull(data);
        assertEquals(false, data.get(Parameters.CW_ALL));

        List<ConsentDocument> documents = new LinkedList<>();
        documents.add(ConsentDocument.builder()
                .documentDescription("withdrawn context desc 1")
                .documentId("withdrawn context id 1")
                .documentName("withdrawn context name 1")
                .documentVersion("withdrawn context version 1")
                .build());
        documents.add(ConsentDocument.builder()
                .documentDescription("withdrawn context desc 2")
                .documentId("withdrawn context id 2")
                .documentName("withdrawn context name 2")
                .documentVersion("withdrawn context version 2")
                .build());

        event = ConsentWithdrawn.builder()
                .documentName("name")
                .documentDescription("description")
                .documentId("id")
                .documentVersion("v1.0")
                .all(false)
                .consentDocuments(documents)
                .build();

        data = event.getData().getMap();

        assertNotNull(data);
        assertEquals(false, data.get(Parameters.CW_ALL));
    }

    public void testBuilderFailures() {
        boolean exception = false;
        try {
            ConsentWithdrawn.builder().build();
        } catch (Exception e) {
            assertEquals(null, e.getMessage());
            exception = true;
        }
        assertTrue(exception);

        exception = false;
        try {
            ConsentWithdrawn.builder().documentId("").documentVersion("test").build();
        } catch (Exception e) {
            assertEquals("Document ID cannot be empty", e.getMessage());
            exception = true;
        }
        assertTrue(exception);

        exception = false;
        try {
            ConsentWithdrawn.builder().documentVersion("").documentId("test").build();
        } catch (Exception e) {
            assertEquals("Document version cannot be empty", e.getMessage());
            exception = true;
        }
        assertTrue(exception);
    }
}
