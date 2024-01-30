/*
 * Copyright (c) 2015-2023 Snowplow Analytics Ltd. All rights reserved.
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
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.core.emitter.Emitter
import com.snowplowanalytics.core.session.FileStore.deleteFile
import com.snowplowanalytics.core.session.Session
import com.snowplowanalytics.core.tracker.Tracker
import com.snowplowanalytics.core.utils.NotificationCenter.postNotification
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class SessionTest {
    private var timestamp = 1234567891012L
    private var timestampDateTime = "2009-02-13T23:31:31.012Z"
    
    @Before
    @Throws(Exception::class)
    fun setUp() {
        val context = context
        cleanSharedPreferences(context, TrackerConstants.SNOWPLOW_SESSION_VARS)
        cleanSharedPreferences(context, TrackerConstants.SNOWPLOW_GENERAL_VARS)
    }

    @Test
    fun testSessionInit() {
        val session = getSession(600, 300)
        var sessionState = session.state
        Assert.assertNotNull(session)
        Assert.assertEquals(600000, session.foregroundTimeout)
        Assert.assertEquals(300000, session.backgroundTimeout)
        Assert.assertNull(sessionState)
        Assert.assertNotNull(session.userId)
        val sdj = session.getSessionContext("first-id-1", timestamp, false)
        sessionState = session.state

        Assert.assertNotNull(sdj)
        Assert.assertNotNull(sessionState)
        Assert.assertEquals("first-id-1", sessionState!!.firstEventId)
        Assert.assertEquals(timestampDateTime, sessionState.firstEventTimestamp)
        session.getSessionContext("second-id-2", timestamp + 10000, false)
        Assert.assertEquals("first-id-1", sessionState.firstEventId)
        Assert.assertEquals(timestampDateTime, sessionState.firstEventTimestamp)
        Assert.assertEquals(TrackerConstants.SESSION_SCHEMA, sdj!!.map["schema"])
    }

    @Test
    fun testFirstSession() {
        val session = getSession(3, 3)
        val sessionContext = getSessionContext(session, "event_1", timestamp, false)
        Assert.assertNotNull(sessionContext!![Parameters.SESSION_USER_ID])
        Assert.assertNotNull(sessionContext[Parameters.SESSION_ID])
        Assert.assertEquals(1, session.sessionIndex)
        Assert.assertNotNull(sessionContext[Parameters.SESSION_INDEX])
        Assert.assertEquals(1, sessionContext[Parameters.SESSION_INDEX])
        Assert.assertEquals("event_1", sessionContext[Parameters.SESSION_FIRST_ID])
        Assert.assertEquals(timestampDateTime, sessionContext[Parameters.SESSION_FIRST_TIMESTAMP])
        Assert.assertEquals(1, sessionContext[Parameters.SESSION_EVENT_INDEX])
        Assert.assertEquals("LOCAL_STORAGE", sessionContext[Parameters.SESSION_STORAGE])
    }

    @Test
    @Throws(InterruptedException::class)
    fun testForegroundEventsOnSameSession() {
        val session = getSession(15, 0)
        var sessionContext = getSessionContext(session, "event_1", timestamp, false)
        val sessionId = sessionContext!![Parameters.SESSION_ID] as String?
        Assert.assertNotNull(sessionId)
        Assert.assertEquals(1, sessionContext[Parameters.SESSION_INDEX])
        Assert.assertEquals("event_1", sessionContext[Parameters.SESSION_FIRST_ID])
        Assert.assertEquals(timestampDateTime, sessionContext[Parameters.SESSION_FIRST_TIMESTAMP])
        Assert.assertEquals(1, sessionContext[Parameters.SESSION_EVENT_INDEX])
        Thread.sleep(100)
        sessionContext = getSessionContext(session, "event_2", timestamp + 5000, false)
        Assert.assertEquals(sessionId, sessionContext!![Parameters.SESSION_ID] as String?)
        Assert.assertEquals(1, sessionContext[Parameters.SESSION_INDEX])
        Assert.assertEquals("event_1", sessionContext[Parameters.SESSION_FIRST_ID])
        Assert.assertEquals(timestampDateTime, sessionContext[Parameters.SESSION_FIRST_TIMESTAMP])
        Assert.assertEquals(2, sessionContext[Parameters.SESSION_EVENT_INDEX])
        Thread.sleep(15100)
        sessionContext = getSessionContext(session, "event_3", timestamp + 10000, false)
        Assert.assertEquals(sessionId, sessionContext!![Parameters.SESSION_PREVIOUS_ID] as String?)
        Assert.assertEquals(2, sessionContext[Parameters.SESSION_INDEX])
        Assert.assertEquals("event_3", sessionContext[Parameters.SESSION_FIRST_ID])
        Assert.assertEquals(
            "2009-02-13T23:31:41.012Z",
            sessionContext[Parameters.SESSION_FIRST_TIMESTAMP]
        )
        Assert.assertEquals(1, sessionContext[Parameters.SESSION_EVENT_INDEX])
    }

    @Test
    @Throws(InterruptedException::class)
    fun testBackgroundEventsOnSameSession() {
        val session = getSession(0, 15)
        session.setBackground(true)
        var sessionContext = getSessionContext(session, "event_1", timestamp, false)
        val sessionId = sessionContext!![Parameters.SESSION_ID] as String?
        Assert.assertNotNull(sessionId)
        Assert.assertEquals(1, sessionContext[Parameters.SESSION_INDEX])
        Assert.assertEquals("event_1", sessionContext[Parameters.SESSION_FIRST_ID])
        Assert.assertEquals(timestampDateTime, sessionContext[Parameters.SESSION_FIRST_TIMESTAMP])
        Assert.assertEquals(1, sessionContext[Parameters.SESSION_EVENT_INDEX])
        Thread.sleep(100)
        sessionContext = getSessionContext(session, "event_2", timestamp + 5000, false)
        Assert.assertEquals(1, sessionContext!![Parameters.SESSION_INDEX])
        Assert.assertEquals(sessionId, sessionContext[Parameters.SESSION_ID] as String?)
        Assert.assertEquals("event_1", sessionContext[Parameters.SESSION_FIRST_ID])
        Assert.assertEquals(timestampDateTime, sessionContext[Parameters.SESSION_FIRST_TIMESTAMP])
        Assert.assertEquals(2, sessionContext[Parameters.SESSION_EVENT_INDEX])
        Thread.sleep(15100)
        sessionContext = getSessionContext(session, "event_3", timestamp + 10000, false)
        Assert.assertEquals(2, sessionContext!![Parameters.SESSION_INDEX])
        Assert.assertEquals(sessionId, sessionContext[Parameters.SESSION_PREVIOUS_ID] as String?)
        Assert.assertEquals("event_3", sessionContext[Parameters.SESSION_FIRST_ID])
        Assert.assertEquals(
            "2009-02-13T23:31:41.012Z",
            sessionContext[Parameters.SESSION_FIRST_TIMESTAMP]
        )
        Assert.assertEquals(1, sessionContext[Parameters.SESSION_EVENT_INDEX])
    }

    @Test
    @Throws(InterruptedException::class)
    fun testMixedEventsOnManySessions() {
        val session = getSession(1, 1)
        var sessionContext = getSessionContext(session, "event_1", timestamp, false)
        var sessionId = sessionContext!![Parameters.SESSION_ID] as String?
        Assert.assertNotNull(sessionId)
        Assert.assertEquals(1, sessionContext[Parameters.SESSION_INDEX])
        Assert.assertEquals("event_1", sessionContext[Parameters.SESSION_FIRST_ID])
        Assert.assertEquals(timestampDateTime, sessionContext[Parameters.SESSION_FIRST_TIMESTAMP])
        var oldSessionId = sessionId
        session.setBackground(true)
        Thread.sleep(1100)
        sessionContext = getSessionContext(session, "event_2", timestamp + 10000, false)
        sessionId = sessionContext!![Parameters.SESSION_ID] as String?
        Assert.assertEquals(oldSessionId, sessionContext[Parameters.SESSION_PREVIOUS_ID] as String?)
        Assert.assertEquals(2, sessionContext[Parameters.SESSION_INDEX])
        Assert.assertEquals("event_2", sessionContext[Parameters.SESSION_FIRST_ID])
        oldSessionId = sessionId
        session.setBackground(false)
        Thread.sleep(1100)
        sessionContext = getSessionContext(session, "event_3", timestamp + 20000, false)
        sessionId = sessionContext!![Parameters.SESSION_ID] as String?
        Assert.assertEquals(oldSessionId, sessionContext[Parameters.SESSION_PREVIOUS_ID] as String?)
        Assert.assertEquals(3, sessionContext[Parameters.SESSION_INDEX])
        Assert.assertEquals("event_3", sessionContext[Parameters.SESSION_FIRST_ID])
        oldSessionId = sessionId
        session.setBackground(true)
        Thread.sleep(1100)
        sessionContext = getSessionContext(session, "event_4", timestamp + 30000, false)
        Assert.assertEquals(
            oldSessionId,
            sessionContext!![Parameters.SESSION_PREVIOUS_ID] as String?
        )
        Assert.assertEquals(4, sessionContext[Parameters.SESSION_INDEX])
        Assert.assertEquals("event_4", sessionContext[Parameters.SESSION_FIRST_ID])
    }

    @Test
    @Throws(InterruptedException::class)
    fun testTimeoutSessionWhenPauseAndResume() {
        val session = getSession(1, 1)
        var sessionContext = getSessionContext(session, "event_1", timestamp, false)
        val oldSessionIndex = sessionContext!![Parameters.SESSION_INDEX] as Int?
        var prevSessionId = sessionContext[Parameters.SESSION_ID] as String?
        Assert.assertEquals("event_1", sessionContext[Parameters.SESSION_FIRST_ID])
        session.setIsSuspended(true)
        Thread.sleep(2000)
        sessionContext = getSessionContext(session, "event_2", timestamp, false)
        Assert.assertEquals(oldSessionIndex, sessionContext!![Parameters.SESSION_INDEX])
        Assert.assertEquals(prevSessionId, sessionContext[Parameters.SESSION_ID] as String?)
        Assert.assertEquals("event_1", sessionContext[Parameters.SESSION_FIRST_ID])
        prevSessionId = sessionContext[Parameters.SESSION_ID] as String?
        session.setIsSuspended(false)
        sessionContext = getSessionContext(session, "event_3", timestamp, false)
        Assert.assertEquals(oldSessionIndex!! + 1, sessionContext!![Parameters.SESSION_INDEX])
        Assert.assertEquals(
            prevSessionId,
            sessionContext[Parameters.SESSION_PREVIOUS_ID] as String?
        )
        Assert.assertEquals("event_3", sessionContext[Parameters.SESSION_FIRST_ID])
    }

    @Test
    @Throws(InterruptedException::class)
    fun testBackgroundTimeBiggerThanBackgroundTimeoutCausesNewSession() {
        cleanSharedPreferences(context, TrackerConstants.SNOWPLOW_SESSION_VARS + "_tracker")
        
        val emitter = Emitter(
            "tracker", null, context, "", null
        )
        val trackerBuilder = { tracker: Tracker ->
            tracker.sessionContext = true
            tracker.lifecycleAutotracking = true
            tracker.foregroundTimeout = 100
            tracker.backgroundTimeout = 2
        }
        val tracker = Tracker(emitter, "tracker", "app", context = context, builder = trackerBuilder)
        val session = tracker.session
        
        getSessionContext(session, "event_1", timestamp, false)
        var sessionState = session!!.state
        Assert.assertNotNull(sessionState)
        Assert.assertEquals(1, sessionState!!.sessionIndex.toLong())
        Assert.assertEquals("event_1", sessionState.firstEventId)
        val oldSessionId = sessionState.sessionId
        
        Thread.sleep(1000) // Smaller than background timeout
        var notificationData: MutableMap<String, Any> = HashMap()
        notificationData["isForeground"] = java.lang.Boolean.FALSE
        postNotification("SnowplowLifecycleTracking", notificationData)
        
        Thread.sleep(3000) // Bigger than background timeout
        notificationData = HashMap()
        notificationData["isForeground"] = java.lang.Boolean.TRUE
        postNotification("SnowplowLifecycleTracking", notificationData)
        
        sessionState = session.state
        Assert.assertEquals(2, sessionState!!.sessionIndex.toLong())
        Assert.assertEquals(oldSessionId, sessionState.previousSessionId)
        Assert.assertFalse(session.isBackground)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testBackgroundTimeSmallerThanBackgroundTimeoutDoesntCauseNewSession() {
        cleanSharedPreferences(context, TrackerConstants.SNOWPLOW_SESSION_VARS + "_tracker")
        val emitter = Emitter(
            "tracker", null, context, "", null
        )
        val trackerBuilder = { tracker: Tracker ->
            tracker.sessionContext = true
            tracker.lifecycleAutotracking = true
            tracker.foregroundTimeout = 100
            tracker.backgroundTimeout = 2
        }
        val tracker = Tracker(emitter, "tracker", "app", context = context, builder = trackerBuilder)
        val session = tracker.session
        getSessionContext(session, "event_1", timestamp, false)
        var sessionState = session!!.state
        Assert.assertNotNull(sessionState)
        Assert.assertEquals(1, sessionState!!.sessionIndex.toLong())
        Assert.assertEquals("event_1", sessionState.firstEventId)
        val oldSessionId = sessionState.sessionId
        Thread.sleep(3000) // Bigger than background timeout
        var notificationData: MutableMap<String, Any> = HashMap()
        notificationData["isForeground"] = java.lang.Boolean.FALSE
        postNotification("SnowplowLifecycleTracking", notificationData)
        Thread.sleep(1000) // Smaller than background timeout
        notificationData = HashMap()
        notificationData["isForeground"] = java.lang.Boolean.TRUE
        postNotification("SnowplowLifecycleTracking", notificationData)
        sessionState = session.state
        Assert.assertEquals(1, sessionState!!.sessionIndex.toLong())
        Assert.assertEquals(oldSessionId, sessionState.sessionId)
        Assert.assertFalse(session.isBackground)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testNoEventsForLongTimeDontIncreaseSessionIndexMultipleTimes() {
        val session = getSession(1, 1)
        var sessionContext = getSessionContext(session, "event_1", timestamp, false)
        val oldSessionIndex = sessionContext!![Parameters.SESSION_INDEX] as Int?
        Assert.assertEquals("event_1", sessionContext[Parameters.SESSION_FIRST_ID])
        Thread.sleep(4000)
        sessionContext = getSessionContext(session, "event_2", timestamp, false)
        Assert.assertEquals(oldSessionIndex!! + 1, sessionContext!![Parameters.SESSION_INDEX])
        Assert.assertEquals("event_2", sessionContext[Parameters.SESSION_FIRST_ID])
    }

    @Test
    fun testSessionInitWithIncompletePersistedData() {
        val prefs = context.getSharedPreferences(
            TrackerConstants.SNOWPLOW_SESSION_VARS,
            Context.MODE_PRIVATE
        )
        prefs.edit().clear().commit()
        prefs.edit().putString(Parameters.SESSION_USER_ID, UUID.randomUUID().toString()).commit()
        val session = Session(600, 300, TimeUnit.SECONDS, null, context)
        Assert.assertNotNull(session)
        Assert.assertNull(session.state)
        Assert.assertEquals(600000, session.foregroundTimeout)
        Assert.assertEquals(300000, session.backgroundTimeout)
        Assert.assertNotNull(session.userId)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testStartNewSessionRenewTheSession() {
        val session = getSession(3, 3)
        var sessionContext = getSessionContext(session, "event_1", timestamp, false)
        val sessionId = sessionContext!![Parameters.SESSION_ID] as String?
        Assert.assertNotNull(sessionId)
        Assert.assertEquals(1, sessionContext[Parameters.SESSION_INDEX])
        Assert.assertEquals("event_1", sessionContext[Parameters.SESSION_FIRST_ID])
        Thread.sleep(200)
        session.startNewSession()
        sessionContext = getSessionContext(session, "event_2", timestamp, false)
        Assert.assertEquals(sessionId, sessionContext!![Parameters.SESSION_PREVIOUS_ID] as String?)
        Assert.assertEquals(2, sessionContext[Parameters.SESSION_INDEX])
        Assert.assertEquals("event_2", sessionContext[Parameters.SESSION_FIRST_ID])
    }

    @Test
    @Throws(InterruptedException::class)
    fun testMultipleTrackersUpdateDifferentSessions() {
        cleanSharedPreferences(context, TrackerConstants.SNOWPLOW_SESSION_VARS + "_tracker1")
        cleanSharedPreferences(context, TrackerConstants.SNOWPLOW_SESSION_VARS + "_tracker2")
        val emitter = Emitter(
            "ns", null, context, "", null
        )
        val trackerBuilder = { tracker: Tracker ->
            tracker.sessionContext = true
            tracker.foregroundTimeout = 20
            tracker.backgroundTimeout = 20
        }
        val tracker1 = Tracker(emitter, "tracker1", "app", context = context, builder = trackerBuilder)
        val tracker2 = Tracker(emitter, "tracker2", "app", context = context, builder = trackerBuilder)
        val session1 = tracker1.session
        val session2 = tracker2.session
        session1!!.getSessionContext("session1-fake-id1", timestamp, false)
        session2!!.getSessionContext("session2-fake-id1", timestamp, false)
        val initialValue1 = session1.sessionIndex?.toLong()
        val id1 = session1.state!!.sessionId
        val initialValue2 = session2.sessionIndex?.toLong()

        // Retrigger session in tracker1
        // The timeout is 20s, this sleep is only 2s - it's still the same session
        Thread.sleep(2000)
        session1.getSessionContext("session1-fake-id2", timestamp, false)

        // Retrigger timedout session in tracker2
        // 20s has then passed. Session must be updated, increasing the sessionIndex by 1
        Thread.sleep(18000)
        session2.getSessionContext("session2-fake-id2", timestamp, false)

        // Check sessions have the correct state
        Assert.assertEquals(0, session1.sessionIndex!! - initialValue1!!)
        Assert.assertEquals(1, session2.sessionIndex!! - initialValue2!!)
        val id2 = session2.state!!.sessionId

        // Recreate tracker2
        val tracker2b = Tracker(emitter, "tracker2", "app", context = context, builder = trackerBuilder)
        tracker2b.session!!.getSessionContext("session2b-fake-id3", timestamp, false)
        val initialValue2b = tracker2b.session!!.sessionIndex?.toLong()
        val previousId2b = tracker2b.session!!.state!!.previousSessionId

        // Check the new tracker session gets the data from the old tracker2 session
        Assert.assertEquals(initialValue2 + 2, initialValue2b)
        Assert.assertEquals(id2, previousId2b)
        Assert.assertNotEquals(id1, previousId2b)
    }

    @Test
    fun testAnonymisesUserAndPreviousSessionIdentifiers() {
        val session = Session(600, 300, TimeUnit.SECONDS, null, context)
        getSessionContext(session, "eid1", 1000, false)
        session.startNewSession() // so that a reference to previous session is created
        val context = getSessionContext(session, "eid2", 1001, true)
        Assert.assertEquals(
            "00000000-0000-0000-0000-000000000000",
            context!![Parameters.SESSION_USER_ID]
        )
        Assert.assertNull(context[Parameters.SESSION_PREVIOUS_ID])
    }

    // Private methods
    private fun getSession(foregroundTimeout: Long, backgroundTimeout: Long): Session {
        context.getSharedPreferences(TrackerConstants.SNOWPLOW_SESSION_VARS, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        return Session(foregroundTimeout, backgroundTimeout, TimeUnit.SECONDS, null, context)
    }

    private fun getSessionContext(
        session: Session?,
        eventId: String,
        eventTimestamp: Long,
        userAnonymisation: Boolean
    ): Map<String, Any>? {
        return session!!.getSessionContext(
            eventId,
            eventTimestamp,
            userAnonymisation
        )?.map?.get(Parameters.DATA) as Map<String, Any>?
    }

    private fun cleanSharedPreferences(context: Context, sharedPreferencesName: String) {
        deleteFile(sharedPreferencesName, context)
        if (Build.VERSION.SDK_INT >= 24) {
            context.deleteSharedPreferences(sharedPreferencesName)
        } else {
            context.getSharedPreferences(sharedPreferencesName, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit()
        }
    }

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext
}
