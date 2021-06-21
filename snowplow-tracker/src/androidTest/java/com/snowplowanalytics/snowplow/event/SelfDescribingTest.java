package com.snowplowanalytics.snowplow.event;

import android.test.AndroidTestCase;

import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;

public class SelfDescribingTest extends AndroidTestCase {

    public void testSetSessionTrueTimestamp() {
        Event e = SelfDescribing.builder()
                .eventData(new SelfDescribingJson("schema"))
                .trueTimestamp(123456789)
                .build();
        assertEquals(new Long(123456789), e.getTrueTimestamp());
    }

}
