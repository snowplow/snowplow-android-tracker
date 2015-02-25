package com.snowplowanalytics.snowplow.tracker;

import android.test.AndroidTestCase;

import com.snowplowanalytics.snowplow.tracker.utils.Logger;

import junit.framework.Assert;

public class EmitterTest extends AndroidTestCase {

    private final static String TAG = Emitter.class.getSimpleName();
    private final static String testURL = "testUrl";

    // Helper Methods

    private Emitter getEmitter(HttpMethod method, BufferOption option, RequestSecurity security) {
        return new Emitter.EmitterBuilder(testURL, getContext())
                .option(option)
                .method(method)
                .security(security)
                .callback(getCallback())
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
        Assert.assertEquals(HttpMethod.GET, emitter.getHttpMethod());

        emitter = getEmitter(HttpMethod.POST, BufferOption.DefaultGroup, RequestSecurity.HTTP);
        Assert.assertEquals(HttpMethod.POST, emitter.getHttpMethod());
    }

    public void testBufferOptionSet() {
        Emitter emitter = getEmitter(HttpMethod.GET, BufferOption.Single, RequestSecurity.HTTP);
        Assert.assertEquals(BufferOption.Single, emitter.getBufferOption());

        emitter = getEmitter(HttpMethod.GET, BufferOption.DefaultGroup, RequestSecurity.HTTP);
        Assert.assertEquals(BufferOption.DefaultGroup, emitter.getBufferOption());

        emitter.setBufferOption(BufferOption.HeavyGroup);
        Assert.assertEquals(BufferOption.HeavyGroup, emitter.getBufferOption());
    }

    public void testCallbackSet() {
        Emitter emitter = getEmitter(HttpMethod.GET, BufferOption.DefaultGroup, RequestSecurity.HTTP);
        assertNotNull(emitter.getRequestCallback());
    }

    public void testUriSet() {
        Emitter emitter = getEmitter(HttpMethod.GET, BufferOption.Single, RequestSecurity.HTTP);
        Assert.assertEquals("http://" + testURL + "/i", emitter.getEmitterUri());

        emitter = getEmitter(HttpMethod.POST, BufferOption.DefaultGroup, RequestSecurity.HTTP);
        Assert.assertEquals("http://" + testURL + "/com.snowplowanalytics.snowplow/tp2",
                emitter.getEmitterUri());

        emitter = getEmitter(HttpMethod.GET, BufferOption.DefaultGroup, RequestSecurity.HTTPS);
        Assert.assertEquals("https://" + testURL + "/i", emitter.getEmitterUri());

        emitter = getEmitter(HttpMethod.POST, BufferOption.DefaultGroup, RequestSecurity.HTTPS);
        Assert.assertEquals("https://" + testURL + "/com.snowplowanalytics.snowplow/tp2",
                emitter.getEmitterUri());
    }

    public void testSecuritySet() {
        Emitter emitter = getEmitter(HttpMethod.GET, BufferOption.Single, RequestSecurity.HTTP);
        Assert.assertEquals(RequestSecurity.HTTP, emitter.getRequestSecurity());

        emitter = getEmitter(HttpMethod.POST, BufferOption.DefaultGroup, RequestSecurity.HTTPS);
        Assert.assertEquals(RequestSecurity.HTTPS, emitter.getRequestSecurity());
    }
}
