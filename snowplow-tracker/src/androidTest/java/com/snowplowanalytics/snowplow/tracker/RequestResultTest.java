package com.snowplowanalytics.snowplow.tracker;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.snowplowanalytics.snowplow.network.RequestResult;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class RequestResultTest {

    @Test
    public void testSuccessfulRequest() {
        RequestResult result = new RequestResult(200, false, Arrays.asList(100L));
        assertTrue(result.isSuccessful());
        assertFalse(result.isOversize());
        assertFalse(result.shouldRetry(new HashMap<>()));
        assertEquals(result.getEventIds(), Arrays.asList(100L));
    }

    @Test
    public void testFailedRequest() {
        RequestResult result = new RequestResult(500, false, new ArrayList<>());
        assertFalse(result.isSuccessful());
        assertTrue(result.shouldRetry(new HashMap<>()));
    }

    @Test
    public void testOversizedFailedRequest() {
        RequestResult result = new RequestResult(500, true, new ArrayList<>());
        assertFalse(result.isSuccessful());
        assertFalse(result.shouldRetry(new HashMap<>()));
    }

    @Test
    public void testFailedRequestWithNoRetryStatus() {
        RequestResult result = new RequestResult(403, false, new ArrayList<>());
        assertFalse(result.isSuccessful());
        assertFalse(result.shouldRetry(new HashMap<>()));
    }

    @Test
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
