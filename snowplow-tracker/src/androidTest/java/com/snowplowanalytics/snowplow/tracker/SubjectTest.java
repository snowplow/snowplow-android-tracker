/*
 * Copyright (c) 2015 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */

package com.snowplowanalytics.snowplow.tracker;

import android.test.AndroidTestCase;

import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.utils.LogLevel;
import com.snowplowanalytics.snowplow.tracker.utils.Logger;

import java.util.Map;

public class SubjectTest extends AndroidTestCase {

    // Helper Methods

    private Subject getSubject() {
        Logger.updateLogLevel(LogLevel.DEBUG);
        return new Subject
                .SubjectBuilder()
                .context(getContext())
                .build();
    }

    // Tests

    public void testSubjectWithNullContext() {
        Subject subject = new Subject.SubjectBuilder().build();
        Map<String, String> map = subject.getSubject();
        assertFalse(map.containsKey(Parameters.RESOLUTION));
    }

    public void testGetSubjectStandardPairs() throws Exception {
        Subject subject = getSubject();
        Map<String, String> standardPairs = subject.getSubject();

        assertTrue(standardPairs.containsKey("tz"));
        assertTrue(standardPairs.containsKey("lang"));
        assertTrue(standardPairs.containsKey("res"));
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
