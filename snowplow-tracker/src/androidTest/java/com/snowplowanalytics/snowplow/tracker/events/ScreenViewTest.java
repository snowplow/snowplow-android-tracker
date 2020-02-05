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

import org.junit.internal.runners.statements.Fail;

import java.util.Map;
import java.util.UUID;

public class ScreenViewTest extends AndroidTestCase {

    public void testExpectedForm() {
        ScreenView screenView = ScreenView.builder()
                .name("name")
                .build();

        Map data = screenView.getData().getMap();

        assertNotNull(data);
        assertEquals("name", data.get(Parameters.SV_NAME));
        assertTrue(data.containsKey(Parameters.SV_ID));

        String id = UUID.randomUUID().toString();
        screenView = ScreenView.builder()
                .id(id)
                .build();

        data = screenView.getData().getMap();

        assertNotNull(data);
        assertEquals(id, data.get(Parameters.SV_ID));
        assertFalse(data.containsKey(Parameters.SV_NAME));

        screenView = ScreenView.builder()
                .name("name")
                .id(id)
                .build();

        data = screenView.getData().getMap();

        assertNotNull(data);
        assertEquals("name", data.get(Parameters.SV_NAME));
        assertEquals(id, data.get(Parameters.SV_ID));
    }

    public void testBuilderFailures() {
        try {
            ScreenView.builder().id("id").build();
            fail();
        } catch (Exception e) {
            assertEquals(null, e.getMessage());
        }
    }
}
