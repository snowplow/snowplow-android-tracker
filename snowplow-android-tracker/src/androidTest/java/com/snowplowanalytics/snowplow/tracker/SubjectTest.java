package com.snowplowanalytics.snowplow.tracker;

import android.test.AndroidTestCase;

import java.util.Map;

public class SubjectTest extends AndroidTestCase {

    // Helper Methods

    private Subject getSubject() {
        return new Subject(getContext());
    }

    // Tests

    public void testGetSubjectStandardPairs() {
        Subject subject = getSubject();
        Map<String, String> standardPairs = subject.getSubject();
        Map<String, String> mobilePairs = subject.getSubjectMobile();

        assertTrue(standardPairs.containsKey("tz"));
        assertTrue(standardPairs.containsKey("lang"));
        assertTrue(mobilePairs.containsKey("osType"));
        assertTrue(mobilePairs.containsKey("osVersion"));
        assertTrue(mobilePairs.containsKey("deviceModel"));
        assertTrue(mobilePairs.containsKey("deviceManufacturer"));
        assertTrue(mobilePairs.containsKey("androidIdfa"));
        assertTrue(standardPairs.containsKey("res"));
        assertTrue(mobilePairs.containsKey("carrier"));
    }
}
