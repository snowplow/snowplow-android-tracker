/*
 * Copyright (c) 2015-2022 Snowplow Analytics Ltd. All rights reserved.
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

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.snowplowanalytics.snowplow.Snowplow;
import com.snowplowanalytics.snowplow.controller.TrackerController;
import com.snowplowanalytics.snowplow.internal.tracker.Subject;
import com.snowplowanalytics.snowplow.internal.tracker.Logger;
import com.snowplowanalytics.snowplow.network.HttpMethod;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class SubjectTest {

    // Tests

    @Test
    public void testGetSubjectStandardPairs() throws Exception {
        Subject subject = createSubject();
        Map<String, String> standardPairs = subject.getSubject(false);

        assertTrue(standardPairs.containsKey("tz"));
        assertTrue(standardPairs.containsKey("lang"));
        assertTrue(standardPairs.containsKey("res"));
    }

    @Test
    public void testSetUserId() {
        Subject subject = createSubject();
        subject.setUserId("newUserId");
        assertEquals("newUserId", subject.getSubject(false).get("uid"));
    }

    @Test
    public void testSetScreenRes() {
        Subject subject = createSubject();
        subject.setScreenResolution(3000,1000);
        assertEquals("3000x1000", subject.getSubject(false).get("res"));
    }

    @Test
    public void testSetViewPort() {
        Subject subject = createSubject();
        subject.setViewPort(3000,1000);
        assertEquals("3000x1000", subject.getSubject(false).get("vp"));
    }

    @Test
    public void testSetColorDepth() {
        Subject subject = createSubject();
        subject.setColorDepth(1000);
        assertEquals("1000", subject.getSubject(false).get("cd"));
    }

    @Test
    public void testSetTimezone() {
        Subject subject = createSubject();
        subject.setTimezone("fake/timezone");
        assertEquals("fake/timezone", subject.getSubject(false).get("tz"));
    }

    @Test
    public void testSetLanguage() {
        Subject subject = createSubject();
        subject.setLanguage("French");
        assertEquals("French", subject.getSubject(false).get("lang"));
    }

    @Test
    public void testSetIpAddress() {
        Subject subject = createSubject();
        subject.setIpAddress("127.0.0.1");
        assertEquals("127.0.0.1", subject.getSubject(false).get("ip"));
    }

    @Test
    public void testSetUseragent() {
        Subject subject = createSubject();
        subject.setUseragent("Agent");
        assertEquals("Agent", subject.getSubject(false).get("ua"));
    }

    @Test
    public void testSetNetworkUID() {
        Subject subject = createSubject();
        subject.setNetworkUserId("nuid-test");
        assertEquals("nuid-test", subject.getSubject(false).get("tnuid"));
    }

    @Test
    public void testSetDomainUID() {
        Subject subject = createSubject();
        subject.setDomainUserId("duid-test");
        assertEquals("duid-test", subject.getSubject(false).get("duid"));
    }

    @Test
    public void testSubjectUserIdCanBeUpdated() {
        TrackerController tracker = Snowplow.createTracker(getContext(), "default", "https://fake-url", HttpMethod.POST);
        assertNotNull(tracker.getSubject());
        assertNull(tracker.getSubject().getUserId());
        tracker.getSubject().setUserId("fakeUserId");
        assertEquals("fakeUserId", tracker.getSubject().getUserId());
        tracker.getSubject().setUserId(null);
        assertNull(tracker.getSubject().getUserId());
    }

    @Test
    public void testAnonymisesUserIdentifiers() {
        Subject subject = createSubject();
        subject.setUserId("uid-test");
        subject.setDomainUserId("duid-test");
        subject.setNetworkUserId("nuid-test");
        subject.setIpAddress("127.0.0.1");
        assertNull(subject.getSubject(true).get("uid"));
        assertNull(subject.getSubject(true).get("duid"));
        assertNull(subject.getSubject(true).get("tnuid"));
        assertNull(subject.getSubject(true).get("ip"));
    }

    // Helper Methods

    private Subject createSubject() {
        Logger.updateLogLevel(LogLevel.DEBUG);
        return new Subject(getContext(), null);
    }

    private Context getContext() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }
}
