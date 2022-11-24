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
import static org.junit.Assert.assertNotNull;

import java.util.Map;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class ScreenViewTest {

    @Test
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
}
