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
import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;

import java.util.Map;

public class SelfDescribingTest extends AndroidTestCase {

    public void testExpectedForm() {
        SelfDescribingJson sdj = new SelfDescribingJson("iglu:foo/bar/jsonschema/1-0-0");

        SelfDescribing pageView = SelfDescribing.builder()
                .eventData(sdj)
                .build();

        Map data = pageView.getPayload().getMap();

        assertNotNull(data);
        assertEquals("ue", data.get(Parameters.EVENT));
        assertEquals("{\"schema\":\"iglu:com.snowplowanalytics.snowplow\\/unstruct_event\\/jsonschema\\/1-0-0\",\"data\":{\"schema\":\"iglu:foo\\/bar\\/jsonschema\\/1-0-0\",\"data\":{}}}", data.get(Parameters.UNSTRUCTURED));
        assertFalse(data.containsKey(Parameters.UNSTRUCTURED_ENCODED));

        pageView.setBase64Encode(true);
        data = pageView.getPayload().getMap();

        assertNotNull(data);
        assertEquals("ue", data.get(Parameters.EVENT));
        assertEquals("eyJzY2hlbWEiOiJpZ2x1OmNvbS5zbm93cGxvd2FuYWx5dGljcy5zbm93cGxvd1wvdW5zdHJ1Y3RfZXZlbnRcL2pzb25zY2hlbWFcLzEtMC0wIiwiZGF0YSI6eyJzY2hlbWEiOiJpZ2x1OmZvb1wvYmFyXC9qc29uc2NoZW1hXC8xLTAtMCIsImRhdGEiOnt9fX0=", data.get(Parameters.UNSTRUCTURED_ENCODED));
        assertFalse(data.containsKey(Parameters.UNSTRUCTURED));
    }

    public void testBuilderFailures() {
        boolean exception = false;
        try {
            SelfDescribing.builder().build();
        } catch (Exception e) {
            assertEquals(null, e.getMessage());
            exception = true;
        }
        assertTrue(exception);
    }
}
