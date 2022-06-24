package com.snowplowanalytics.snowplow.tracker;

import android.test.AndroidTestCase;

import com.snowplowanalytics.snowplow.network.RequestResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class RequestResultTest extends AndroidTestCase {

    public void testSuccessfulRequest() {
        RequestResult result = new RequestResult(200, false, Arrays.asList(100L));
        assertTrue(result.isSuccessful());
        assertFalse(result.isOversize());
        assertFalse(result.shouldRetry(new HashMap<>()));
        assertEquals(result.getEventIds(), Arrays.asList(100L));
    }

    public void testFailedRequest() {
        RequestResult result = new RequestResult(500, false, new ArrayList<>());
        assertFalse(result.isSuccessful());
        assertTrue(result.shouldRetry(new HashMap<>()));
    }

    public void testOversizedFailedRequest() {
        RequestResult result = new RequestResult(500, true, new ArrayList<>());
        assertFalse(result.isSuccessful());
        assertFalse(result.shouldRetry(new HashMap<>()));
    }

    public void testFailedRequestWithNoRetryStatus() {
        RequestResult result = new RequestResult(403, false, new ArrayList<>());
        assertFalse(result.isSuccessful());
        assertFalse(result.shouldRetry(new HashMap<>()));
    }

    public void testFailedRequestWithCustomRetryRules() {
        Map<Integer, Boolean> customRetry = new HashMap<>();
        customRetry.put(403, true);
        customRetry.put(500, false);

        RequestResult result = new RequestResult(403, false, new ArrayList<>());
        assertFalse(result.isSuccessful());
        assertTrue(result.shouldRetry(customRetry));

        result = new RequestResult(500, false, new ArrayList<>());
        assertFalse(result.isSuccessful());
        assertFalse(result.shouldRetry(customRetry));
    }

}
