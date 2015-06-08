package com.snowplowanalytics.snowplow.tracker.lite;

import com.snowplowanalytics.snowplow.tracker.*;
import com.snowplowanalytics.snowplow.tracker.Emitter;

public class EmitterTest extends SnowplowLiteTestCase {

    public void testDefaultEmitterShouldBeLiteEmitter() {
        Emitter emitter = getEmitter(HttpMethod.GET, BufferOption.DefaultGroup, RequestSecurity.HTTP);
        assertEquals(com.snowplowanalytics.snowplow.tracker.lite.Emitter.class, emitter.getClass());
    }
}
