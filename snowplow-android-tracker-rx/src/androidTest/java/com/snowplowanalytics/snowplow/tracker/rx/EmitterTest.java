package com.snowplowanalytics.snowplow.tracker.rx;

import com.snowplowanalytics.snowplow.tracker.*;
import com.snowplowanalytics.snowplow.tracker.Emitter;

public class EmitterTest extends SnowplowRxTestCase {

    public void testDefaultEmitterShouldBeLiteEmitter() {
        Emitter emitter = getEmitter(
                HttpMethod.GET,
                BufferOption.DefaultGroup,
                RequestSecurity.HTTP
        );
        assertEquals(com.snowplowanalytics.snowplow.tracker.rx.Emitter.class, emitter.getClass());
    }
}
