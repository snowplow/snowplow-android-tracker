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

import java.util.Map;
import java.util.UUID;

public class ScreenViewTest extends AndroidTestCase {

    public void testExpectedForm() {
        ScreenView screenView = new ScreenView("name");

        Map<String, Object> data = screenView.getDataPayload();

        assertNotNull(data);
        assertEquals("name", data.get(Parameters.SV_NAME));
        assertTrue(data.containsKey(Parameters.SV_ID));

        UUID id = UUID.randomUUID();
        screenView = new ScreenView("name", id);

        data = screenView.getDataPayload();

        assertNotNull(data);
        assertEquals(id.toString(), data.get(Parameters.SV_ID));
        assertEquals("name", data.get(Parameters.SV_NAME));
    }

    public void testBuilderFailures() {
        try {
            new ScreenView(null, UUID.randomUUID());
            fail();
        } catch (Exception e) {
            assertNull(e.getMessage());
        }
    }
}
