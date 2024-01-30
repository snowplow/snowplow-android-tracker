/*
 * Copyright (c) 2015-present Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.snowplow.tracker

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.snowplowanalytics.core.tracker.Logger.updateLogLevel
import com.snowplowanalytics.core.tracker.Subject
import com.snowplowanalytics.snowplow.Snowplow.createTracker
import com.snowplowanalytics.snowplow.util.Size
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SubjectTest {
    // Tests
    @Test
    @Throws(Exception::class)
    fun testGetSubjectStandardPairs() {
        val subject = createSubject()
        val standardPairs = subject.getSubject(false)
        Assert.assertTrue(standardPairs.containsKey("tz"))
        Assert.assertTrue(standardPairs.containsKey("lang"))
        Assert.assertTrue(standardPairs.containsKey("res"))
    }

    @Test
    fun testSetUserId() {
        val subject = createSubject()
        subject.userId = "newUserId"
        Assert.assertEquals("newUserId", subject.getSubject(false)["uid"])
    }

    @Test
    fun testSetScreenRes() {
        val subject = createSubject()
        subject.screenResolution = Size(3000, 1000)
        Assert.assertEquals("3000x1000", subject.getSubject(false)["res"])
    }

    @Test
    fun testSetViewPort() {
        val subject = createSubject()
        subject.screenViewPort = Size(3000, 1000)
        Assert.assertEquals("3000x1000", subject.getSubject(false)["vp"])
    }

    @Test
    fun testSetColorDepth() {
        val subject = createSubject()
        subject.colorDepth = 1000
        Assert.assertEquals("1000", subject.getSubject(false)["cd"])
    }

    @Test
    fun testSetTimezone() {
        val subject = createSubject()
        subject.timezone = "fake/timezone"
        Assert.assertEquals("fake/timezone", subject.getSubject(false)["tz"])
    }

    @Test
    fun testSetLanguage() {
        val subject = createSubject()
        subject.language = "French"
        Assert.assertEquals("French", subject.getSubject(false)["lang"])
    }

    @Test
    fun testSetIpAddress() {
        val subject = createSubject()
        subject.ipAddress = "127.0.0.1"
        Assert.assertEquals("127.0.0.1", subject.getSubject(false)["ip"])
    }

    @Test
    fun testSetUseragent() {
        val subject = createSubject()
        subject.useragent = "Agent"
        Assert.assertEquals("Agent", subject.getSubject(false)["ua"])
    }

    @Test
    fun testSetNetworkUID() {
        val subject = createSubject()
        subject.networkUserId = "nuid-test"
        Assert.assertEquals("nuid-test", subject.getSubject(false)["tnuid"])
    }

    @Test
    fun testSetDomainUID() {
        val subject = createSubject()
        subject.domainUserId = "duid-test"
        Assert.assertEquals("duid-test", subject.getSubject(false)["duid"])
    }

    @Test
    fun testSubjectUserIdCanBeUpdated() {
        val tracker = createTracker(context, "default", "https://fake-url")
        Assert.assertNotNull(tracker.subject)
        Assert.assertNull(tracker.subject.userId)
        tracker.subject.userId = "fakeUserId"
        Assert.assertEquals("fakeUserId", tracker.subject.userId)
        tracker.subject.userId = null
        Assert.assertNull(tracker.subject.userId)
    }

    @Test
    fun testAnonymisesUserIdentifiers() {
        val subject = createSubject()
        subject.userId = "uid-test"
        subject.domainUserId = "duid-test"
        subject.networkUserId = "nuid-test"
        subject.ipAddress = "127.0.0.1"
        Assert.assertNull(subject.getSubject(true)["uid"])
        Assert.assertNull(subject.getSubject(true)["duid"])
        Assert.assertNull(subject.getSubject(true)["tnuid"])
        Assert.assertNull(subject.getSubject(true)["ip"])
    }

    @Test
    fun testSetsScreenResolutionAutomaticaly() {
        val subject = createSubject()
        Assert.assertNotNull(subject.screenResolution)
        Assert.assertTrue((subject.screenResolution?.width ?: 0) > 0)
        Assert.assertTrue((subject.screenResolution?.height ?: 0) > 0)
    }

    // Helper Methods
    private fun createSubject(): Subject {
        updateLogLevel(LogLevel.DEBUG)
        return Subject(context, null)
    }

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext
}
