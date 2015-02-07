package com.snowplowanalytics.snowplow.tracker;

import android.test.AndroidTestCase;

import com.snowplowanalytics.snowplow.tracker.utils.Logger;
import com.snowplowanalytics.snowplow.tracker.utils.emitter.BufferOption;
import com.snowplowanalytics.snowplow.tracker.utils.emitter.HttpMethod;
import com.snowplowanalytics.snowplow.tracker.utils.emitter.RequestCallback;
import com.snowplowanalytics.snowplow.tracker.utils.emitter.RequestSecurity;

public class EmitterTest extends AndroidTestCase {

    private final static String TAG = Emitter.class.getSimpleName();
    private final static String testURL = "10.0.2.2:4545";

    // Helper Methods

    private Emitter getEmitter(HttpMethod method, BufferOption option, RequestSecurity security) {
        return new Emitter
                .EmitterBuilder(testURL, getContext())
                .bufferOption(option)
                .httpMethod(method)
                .requestSecurity(security)
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
        Emitter emitter = getEmitter(HttpMethod.GET, BufferOption.DefaultGroup, RequestSecurity.HTTP);
        assertEquals(HttpMethod.GET, emitter.getHttpMethod());

        emitter = getEmitter(HttpMethod.POST, BufferOption.DefaultGroup, RequestSecurity.HTTP);
        assertEquals(HttpMethod.POST, emitter.getHttpMethod());
    }

    public void testBufferOptionSet() {
        Emitter emitter = getEmitter(HttpMethod.GET, BufferOption.Single, RequestSecurity.HTTP);
        assertEquals(BufferOption.Single, emitter.getBufferOption());

        emitter = getEmitter(HttpMethod.GET, BufferOption.DefaultGroup, RequestSecurity.HTTP);
        assertEquals(BufferOption.DefaultGroup, emitter.getBufferOption());

        emitter.setBufferOption(BufferOption.HeavyGroup);
        assertEquals(BufferOption.HeavyGroup, emitter.getBufferOption());
    }

    public void testCallbackSet() {
        Emitter emitter = getEmitter(HttpMethod.GET, BufferOption.DefaultGroup, RequestSecurity.HTTP);
        assertNotNull(emitter.getRequestCallback());
    }

    public void testUriSet() {
        Emitter emitter = getEmitter(HttpMethod.GET, BufferOption.Single, RequestSecurity.HTTP);
        assertEquals("http://"+testURL+"/i", emitter.getEmitterUri());

        emitter = getEmitter(HttpMethod.POST, BufferOption.DefaultGroup, RequestSecurity.HTTP);
        assertEquals("http://"+testURL+"/com.snowplowanalytics.snowplow/tp2",
                emitter.getEmitterUri());

        emitter = getEmitter(HttpMethod.POST, BufferOption.DefaultGroup, RequestSecurity.HTTPS);
        assertEquals("https://"+testURL+"/com.snowplowanalytics.snowplow/tp2",
                emitter.getEmitterUri());

        emitter = getEmitter(HttpMethod.POST, BufferOption.DefaultGroup, RequestSecurity.HTTPS);
        assertEquals("https://"+testURL+"/com.snowplowanalytics.snowplow/tp2",
                emitter.getEmitterUri());
    }

    public void testSecuritySet() {
        Emitter emitter = getEmitter(HttpMethod.GET, BufferOption.Single, RequestSecurity.HTTP);
        assertEquals(RequestSecurity.HTTP, emitter.getRequestSecurity());

        emitter = getEmitter(HttpMethod.POST, BufferOption.DefaultGroup, RequestSecurity.HTTPS);
        assertEquals(RequestSecurity.HTTPS, emitter.getRequestSecurity());
    }

    public void testIsOnlineIsSubscribed() {
        Emitter emitter = getEmitter(HttpMethod.GET, BufferOption.Single, RequestSecurity.HTTP);
        boolean isOnline = emitter.isOnline();

        if (isOnline) {
            assertEquals(true, emitter.getEmitterSubscriptionStatus());
        }
        else {
            assertEquals(false, emitter.getEmitterSubscriptionStatus());
        }
    }
}
