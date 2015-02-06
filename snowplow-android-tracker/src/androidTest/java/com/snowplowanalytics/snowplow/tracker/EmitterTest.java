package com.snowplowanalytics.snowplow.tracker;

import android.test.AndroidTestCase;

import com.snowplowanalytics.snowplow.tracker.utils.Logger;
import com.snowplowanalytics.snowplow.tracker.utils.emitter.BufferOption;
import com.snowplowanalytics.snowplow.tracker.utils.emitter.HttpMethod;
import com.snowplowanalytics.snowplow.tracker.utils.emitter.RequestCallback;

public class EmitterTest extends AndroidTestCase {

    private final static String TAG = Emitter.class.getSimpleName();
    private final static String testURL = "10.0.2.2:4545";

    // Helper Methods

    private Emitter getEmitter(HttpMethod method, BufferOption option) {
        return new Emitter
                .EmitterBuilder(testURL, getContext())
                .bufferOption(option)
                .httpMethod(method)
                .requestCallback(getCallback())
                .build();
    }

    private RequestCallback getCallback() {
        return new RequestCallback() {
            @Override
            public void onSuccess(int successCount) {
                Logger.ifDebug(TAG, "Successful Sends: %s", successCount);
            }
            @Override
            public void onFailure(int successCount, int failureCount) {
                Logger.ifDebug(TAG,
                        "Successful Sends: %s, Failed Sends: %s",
                        successCount,
                        failureCount);
            }
        };
    }

    // Tests

    public void testHttpMethodSet() {
        Emitter emitter = getEmitter(HttpMethod.GET, BufferOption.DefaultGroup);
        assertEquals(HttpMethod.GET, emitter.getHttpMethod());

        emitter = getEmitter(HttpMethod.POST, BufferOption.DefaultGroup);
        assertEquals(HttpMethod.POST, emitter.getHttpMethod());
    }

    public void testBufferOptionSet() {
        Emitter emitter = getEmitter(HttpMethod.GET, BufferOption.Single);
        assertEquals(BufferOption.Single, emitter.getBufferOption());

        emitter = getEmitter(HttpMethod.GET, BufferOption.DefaultGroup);
        assertEquals(BufferOption.DefaultGroup, emitter.getBufferOption());

        emitter.setBufferOption(BufferOption.HeavyGroup);
        assertEquals(BufferOption.HeavyGroup, emitter.getBufferOption());
    }

    public void testCallbackSet() {
        Emitter emitter = getEmitter(HttpMethod.GET, BufferOption.DefaultGroup);
        assertNotNull(emitter.getRequestCallback());
    }

    public void testUriSet() {
        Emitter emitter = getEmitter(HttpMethod.GET, BufferOption.Single);
        assertEquals("http://"+testURL+"/i", emitter.getEmitterUri());

        emitter = getEmitter(HttpMethod.POST, BufferOption.DefaultGroup);
        assertEquals("http://"+testURL+"/com.snowplowanalytics.snowplow/tp2",
                emitter.getEmitterUri());
    }

    public void testIsOnlineIsSubscribed() {
        Emitter emitter = getEmitter(HttpMethod.GET, BufferOption.Single);
        boolean isOnline = emitter.isOnline();

        if (isOnline) {
            assertEquals(true, emitter.getEmitterSubscriptionStatus());
        }
        else {
            assertEquals(false, emitter.getEmitterSubscriptionStatus());
        }
    }
}
