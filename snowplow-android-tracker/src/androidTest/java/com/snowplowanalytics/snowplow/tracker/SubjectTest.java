package com.snowplowanalytics.snowplow.tracker;

import android.test.AndroidTestCase;

import java.util.Map;

public class SubjectTest extends AndroidTestCase {

    // Helper Methods

    private Subject getSubject() {
        return new Subject
                .SubjectBuilder()
                .context(getContext())
                .build();
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
        //assertTrue(mobilePairs.containsKey("androidIdfa")); Does not work in Travis!
        assertTrue(standardPairs.containsKey("res"));
        assertTrue(mobilePairs.containsKey("carrier"));
    }

    public void testSetUserId() {
        Subject subject = getSubject();
        subject.setUserId("newUserId");
        assertEquals("newUserId", subject.getSubject().get("uid"));
    }

    public void testSetScreenRes() {
        Subject subject = getSubject();
        subject.setScreenResolution(3000,1000);
        assertEquals("3000x1000", subject.getSubject().get("res"));
    }

    public void testSetViewPort() {
        Subject subject = getSubject();
        subject.setViewPort(3000,1000);
        assertEquals("3000x1000", subject.getSubject().get("vp"));
    }

    public void testSetColorDepth() {
        Subject subject = getSubject();
        subject.setColorDepth(1000);
        assertEquals("1000", subject.getSubject().get("cd"));
    }

    public void testSetTimezone() {
        Subject subject = getSubject();
        subject.setTimezone("fake/timezone");
        assertEquals("fake/timezone", subject.getSubject().get("tz"));
    }

    public void testSetLanguage() {
        Subject subject = getSubject();
        subject.setLanguage("French");
        assertEquals("French", subject.getSubject().get("lang"));
    }

    public void testSetIpAddress() {
        Subject subject = getSubject();
        subject.setIpAddress("127.0.0.1");
        assertEquals("127.0.0.1", subject.getSubject().get("ip"));
    }

    public void testSetUseragent() {
        Subject subject = getSubject();
        subject.setUseragent("Agent");
        assertEquals("Agent", subject.getSubject().get("ua"));
    }

    public void testSetNetworkUID() {
        Subject subject = getSubject();
        subject.setNetworkUserId("nuid-test");
        assertEquals("nuid-test", subject.getSubject().get("tnuid"));
    }

    public void testSetDomainUID() {
        Subject subject = getSubject();
        subject.setDomainUserId("duid-test");
        assertEquals("duid-test", subject.getSubject().get("duid"));
    }
}
