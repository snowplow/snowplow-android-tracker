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
import android.content.SharedPreferences;
import android.os.Build;

import com.snowplowanalytics.snowplow.internal.emitter.Emitter;
import com.snowplowanalytics.snowplow.internal.session.Session;
import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.internal.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.internal.tracker.Tracker;
import com.snowplowanalytics.snowplow.internal.utils.NotificationCenter;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.internal.session.FileStore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SessionTest {

    long timestamp = 1234567891012L;
    String timestampDateTime = "2009-02-13T23:31:31.012Z";

    @Before
    public void setUp() throws Exception {
        Context context = getContext();
        cleanSharedPreferences(context, TrackerConstants.SNOWPLOW_SESSION_VARS);
        cleanSharedPreferences(context, TrackerConstants.SNOWPLOW_GENERAL_VARS);
    }

    @Test
    public void testSessionInit() {
        Session session = getSession(600, 300);
        SessionState sessionState = session.getState();

        assertNotNull(session);
        assertEquals(600000, session.getForegroundTimeout());
        assertEquals(300000, session.getBackgroundTimeout());
        assertNull(sessionState);
        assertNotNull(session.getUserId());

        SelfDescribingJson sdj = session.getSessionContext("first-id-1", timestamp, false);
        sessionState = session.getState();
        assertNotNull(sessionState);
        assertEquals("first-id-1", sessionState.getFirstEventId());
        assertEquals(timestampDateTime, sessionState.getFirstEventTimestamp());

        session.getSessionContext("second-id-2", timestamp + 10000, false);
        assertEquals("first-id-1", sessionState.getFirstEventId());
        assertEquals(timestampDateTime, sessionState.getFirstEventTimestamp());

        assertEquals(TrackerConstants.SESSION_SCHEMA, sdj.getMap().get("schema"));
    }

    @Test
    public void testFirstSession() {
        Session session = getSession(3, 3);

        Map<String, Object> sessionContext = getSessionContext(session, "event_1", timestamp, false);
        assertNotNull(sessionContext.get(Parameters.SESSION_USER_ID));
        assertNotNull(sessionContext.get(Parameters.SESSION_ID));

        assertEquals(1, session.getSessionIndex());
        assertNotNull(sessionContext.get(Parameters.SESSION_INDEX));
        assertEquals(1, sessionContext.get(Parameters.SESSION_INDEX));

        assertEquals("event_1", sessionContext.get(Parameters.SESSION_FIRST_ID));
        assertEquals(timestampDateTime, sessionContext.get(Parameters.SESSION_FIRST_TIMESTAMP));
        assertEquals(1, sessionContext.get(Parameters.SESSION_EVENT_INDEX));
        assertEquals("LOCAL_STORAGE", sessionContext.get(Parameters.SESSION_STORAGE));
    }

    @Test
    public void testForegroundEventsOnSameSession() throws InterruptedException {
        Session session = getSession(15, 0);

        Map<String, Object> sessionContext = getSessionContext(session, "event_1", timestamp, false);
        String sessionId = (String) sessionContext.get(Parameters.SESSION_ID);
        assertNotNull(sessionId);
        assertEquals(1, sessionContext.get(Parameters.SESSION_INDEX));
        assertEquals("event_1", sessionContext.get(Parameters.SESSION_FIRST_ID));
        assertEquals(timestampDateTime, sessionContext.get(Parameters.SESSION_FIRST_TIMESTAMP));
        assertEquals(1, sessionContext.get(Parameters.SESSION_EVENT_INDEX));

        Thread.sleep(100);

        sessionContext = getSessionContext(session, "event_2", timestamp + 5000, false);
        assertEquals(sessionId, (String) sessionContext.get(Parameters.SESSION_ID));
        assertEquals(1, sessionContext.get(Parameters.SESSION_INDEX));
        assertEquals("event_1", sessionContext.get(Parameters.SESSION_FIRST_ID));
        assertEquals(timestampDateTime, sessionContext.get(Parameters.SESSION_FIRST_TIMESTAMP));
        assertEquals(2, sessionContext.get(Parameters.SESSION_EVENT_INDEX));

        Thread.sleep(15100);

        sessionContext = getSessionContext(session, "event_3", timestamp + 10000, false);
        assertEquals(sessionId, (String) sessionContext.get(Parameters.SESSION_PREVIOUS_ID));
        assertEquals(2, sessionContext.get(Parameters.SESSION_INDEX));
        assertEquals("event_3", sessionContext.get(Parameters.SESSION_FIRST_ID));
        assertEquals("2009-02-13T23:31:41.012Z", sessionContext.get(Parameters.SESSION_FIRST_TIMESTAMP));
        assertEquals(1, sessionContext.get(Parameters.SESSION_EVENT_INDEX));
    }

    @Test
    public void testBackgroundEventsOnSameSession() throws InterruptedException {
        Session session = getSession(0, 15);

        session.setBackground(true);

        Map<String, Object> sessionContext = getSessionContext(session, "event_1", timestamp, false);
        String sessionId = (String) sessionContext.get(Parameters.SESSION_ID);
        assertNotNull(sessionId);
        assertEquals(1, sessionContext.get(Parameters.SESSION_INDEX));
        assertEquals("event_1", sessionContext.get(Parameters.SESSION_FIRST_ID));
        assertEquals(timestampDateTime, sessionContext.get(Parameters.SESSION_FIRST_TIMESTAMP));
        assertEquals(1, sessionContext.get(Parameters.SESSION_EVENT_INDEX));

        Thread.sleep(100);

        sessionContext = getSessionContext(session, "event_2", timestamp + 5000, false);
        assertEquals(1, sessionContext.get(Parameters.SESSION_INDEX));
        assertEquals(sessionId, (String) sessionContext.get(Parameters.SESSION_ID));
        assertEquals("event_1", sessionContext.get(Parameters.SESSION_FIRST_ID));
        assertEquals(timestampDateTime, sessionContext.get(Parameters.SESSION_FIRST_TIMESTAMP));
        assertEquals(2, sessionContext.get(Parameters.SESSION_EVENT_INDEX));

        Thread.sleep(15100);

        sessionContext = getSessionContext(session, "event_3", timestamp + 10000, false);
        assertEquals(2, sessionContext.get(Parameters.SESSION_INDEX));
        assertEquals(sessionId, (String) sessionContext.get(Parameters.SESSION_PREVIOUS_ID));
        assertEquals("event_3", sessionContext.get(Parameters.SESSION_FIRST_ID));
        assertEquals("2009-02-13T23:31:41.012Z", sessionContext.get(Parameters.SESSION_FIRST_TIMESTAMP));
        assertEquals(1, sessionContext.get(Parameters.SESSION_EVENT_INDEX));
    }

    @Test
    public void testMixedEventsOnManySessions() throws InterruptedException {
        Session session = getSession(1, 1);

        Map<String, Object> sessionContext = getSessionContext(session, "event_1", timestamp, false);
        String sessionId = (String) sessionContext.get(Parameters.SESSION_ID);
        assertNotNull(sessionId);
        assertEquals(1, sessionContext.get(Parameters.SESSION_INDEX));
        assertEquals("event_1", sessionContext.get(Parameters.SESSION_FIRST_ID));
        assertEquals(timestampDateTime, sessionContext.get(Parameters.SESSION_FIRST_TIMESTAMP));
        String oldSessionId = sessionId;

        session.setBackground(true);
        Thread.sleep(1100);

        sessionContext = getSessionContext(session, "event_2", timestamp + 10000, false);
        sessionId = (String) sessionContext.get(Parameters.SESSION_ID);
        assertEquals(oldSessionId, (String) sessionContext.get(Parameters.SESSION_PREVIOUS_ID));
        assertEquals(2, sessionContext.get(Parameters.SESSION_INDEX));
        assertEquals("event_2", sessionContext.get(Parameters.SESSION_FIRST_ID));
        oldSessionId = sessionId;

        session.setBackground(false);
        Thread.sleep(1100);

        sessionContext = getSessionContext(session, "event_3", timestamp + 20000, false);
        sessionId = (String) sessionContext.get(Parameters.SESSION_ID);
        assertEquals(oldSessionId, (String) sessionContext.get(Parameters.SESSION_PREVIOUS_ID));
        assertEquals(3, sessionContext.get(Parameters.SESSION_INDEX));
        assertEquals("event_3", sessionContext.get(Parameters.SESSION_FIRST_ID));
        oldSessionId = sessionId;

        session.setBackground(true);
        Thread.sleep(1100);

        sessionContext = getSessionContext(session, "event_4", timestamp + 30000, false);
        assertEquals(oldSessionId, (String) sessionContext.get(Parameters.SESSION_PREVIOUS_ID));
        assertEquals(4, sessionContext.get(Parameters.SESSION_INDEX));
        assertEquals("event_4", sessionContext.get(Parameters.SESSION_FIRST_ID));
    }

    @Test
    public void testTimeoutSessionWhenPauseAndResume() throws InterruptedException {
        Session session = getSession(1, 1);

        Map<String, Object> sessionContext = getSessionContext(session, "event_1", timestamp, false);
        Integer oldSessionIndex = (Integer) sessionContext.get(Parameters.SESSION_INDEX);
        String prevSessionId = (String) sessionContext.get(Parameters.SESSION_ID);
        assertEquals("event_1", sessionContext.get(Parameters.SESSION_FIRST_ID));

        session.setIsSuspended(true);
        Thread.sleep(2000);

        sessionContext = getSessionContext(session, "event_2", timestamp, false);
        assertEquals(oldSessionIndex, sessionContext.get(Parameters.SESSION_INDEX));
        assertEquals(prevSessionId, (String) sessionContext.get(Parameters.SESSION_ID));
        assertEquals("event_1", sessionContext.get(Parameters.SESSION_FIRST_ID));
        prevSessionId = (String) sessionContext.get(Parameters.SESSION_ID);

        session.setIsSuspended(false);

        sessionContext = getSessionContext(session, "event_3", timestamp, false);
        assertEquals(oldSessionIndex + 1, sessionContext.get(Parameters.SESSION_INDEX));
        assertEquals(prevSessionId, (String) sessionContext.get(Parameters.SESSION_PREVIOUS_ID));
        assertEquals("event_3", sessionContext.get(Parameters.SESSION_FIRST_ID));
    }

    @Test
    public void testBackgroundTimeBiggerThanBackgroundTimeoutCausesNewSession() throws InterruptedException {
        cleanSharedPreferences(getContext(), TrackerConstants.SNOWPLOW_SESSION_VARS + "_tracker");

        Emitter emitter = new Emitter(getContext(), "", null);
        Tracker tracker = new Tracker(new Tracker.TrackerBuilder(emitter, "tracker", "app", getContext())
                .sessionContext(true)
                .lifecycleEvents(true)
                .foregroundTimeout(100)
                .backgroundTimeout(2)
        );
        Session session = tracker.getSession();

        getSessionContext(session, "event_1", timestamp, false);
        SessionState sessionState = session.getState();
        assertNotNull(sessionState);
        assertEquals(1, sessionState.getSessionIndex());
        assertEquals("event_1", sessionState.getFirstEventId());
        String oldSessionId = sessionState.getSessionId();

        Thread.sleep(1000); // Smaller than background timeout
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("isForeground", Boolean.FALSE);
        NotificationCenter.postNotification("SnowplowLifecycleTracking", notificationData);

        Thread.sleep(3000); // Bigger than background timeout
        notificationData = new HashMap<>();
        notificationData.put("isForeground", Boolean.TRUE);
        NotificationCenter.postNotification("SnowplowLifecycleTracking", notificationData);

        sessionState = session.getState();
        assertEquals(2, sessionState.getSessionIndex());
        assertEquals(oldSessionId, sessionState.getPreviousSessionId());
        assertFalse(session.isBackground());
    }

    @Test
    public void testBackgroundTimeSmallerThanBackgroundTimeoutDoesntCauseNewSession() throws InterruptedException {
        cleanSharedPreferences(getContext(), TrackerConstants.SNOWPLOW_SESSION_VARS + "_tracker");

        Emitter emitter = new Emitter(getContext(), "", null);
        Tracker tracker = new Tracker(new Tracker.TrackerBuilder(emitter, "tracker", "app", getContext())
                .sessionContext(true)
                .lifecycleEvents(true)
                .foregroundTimeout(100)
                .backgroundTimeout(2)
        );
        Session session = tracker.getSession();

        getSessionContext(session, "event_1", timestamp, false);
        SessionState sessionState = session.getState();
        assertNotNull(sessionState);
        assertEquals(1, sessionState.getSessionIndex());
        assertEquals("event_1", sessionState.getFirstEventId());
        String oldSessionId = sessionState.getSessionId();


        Thread.sleep(3000); // Bigger than background timeout
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("isForeground", Boolean.FALSE);
        NotificationCenter.postNotification("SnowplowLifecycleTracking", notificationData);

        Thread.sleep(1000); // Smaller than background timeout
        notificationData = new HashMap<>();
        notificationData.put("isForeground", Boolean.TRUE);
        NotificationCenter.postNotification("SnowplowLifecycleTracking", notificationData);

        sessionState = session.getState();
        assertEquals(1, sessionState.getSessionIndex());
        assertEquals(oldSessionId, sessionState.getSessionId());
        assertFalse(session.isBackground());
    }

    @Test
    public void testNoEventsForLongTimeDontIncreaseSessionIndexMultipleTimes() throws InterruptedException {
        Session session = getSession(1, 1);

        Map<String, Object> sessionContext = getSessionContext(session, "event_1", timestamp, false);
        Integer oldSessionIndex = (Integer) sessionContext.get(Parameters.SESSION_INDEX);
        assertEquals("event_1", sessionContext.get(Parameters.SESSION_FIRST_ID));

        Thread.sleep(4000);

        sessionContext = getSessionContext(session, "event_2", timestamp, false);
        assertEquals(oldSessionIndex + 1, sessionContext.get(Parameters.SESSION_INDEX));
        assertEquals("event_2", sessionContext.get(Parameters.SESSION_FIRST_ID));
    }

    @Test
    public void testSessionInitWithIncompletePersistedData() {
        SharedPreferences prefs = getContext().getSharedPreferences(TrackerConstants.SNOWPLOW_SESSION_VARS, Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
        prefs.edit().putString(Parameters.SESSION_USER_ID, UUID.randomUUID().toString()).commit();

        Session session = new Session(600, 300, TimeUnit.SECONDS, null, getContext());

        assertNotNull(session);
        assertNull(session.getState());
        assertEquals(600000, session.getForegroundTimeout());
        assertEquals(300000, session.getBackgroundTimeout());
        assertNotNull(session.getUserId());
    }

    @Test
    public void testStartNewSessionRenewTheSession() throws InterruptedException {
        Session session = getSession(3, 3);

        Map<String, Object> sessionContext = getSessionContext(session, "event_1", timestamp, false);
        String sessionId = (String) sessionContext.get(Parameters.SESSION_ID);
        assertNotNull(sessionId);
        assertEquals(1, sessionContext.get(Parameters.SESSION_INDEX));
        assertEquals("event_1", sessionContext.get(Parameters.SESSION_FIRST_ID));

        Thread.sleep(200);
        session.startNewSession();

        sessionContext = getSessionContext(session, "event_2", timestamp, false);
        assertEquals(sessionId, (String) sessionContext.get(Parameters.SESSION_PREVIOUS_ID));
        assertEquals(2, sessionContext.get(Parameters.SESSION_INDEX));
        assertEquals("event_2", sessionContext.get(Parameters.SESSION_FIRST_ID));
    }

    @Test
    public void testMultipleTrackersUpdateDifferentSessions() throws InterruptedException {
        cleanSharedPreferences(getContext(), TrackerConstants.SNOWPLOW_SESSION_VARS + "_tracker1");
        cleanSharedPreferences(getContext(), TrackerConstants.SNOWPLOW_SESSION_VARS + "_tracker2");

        Emitter emitter = new Emitter(getContext(), "", null);
        Tracker tracker1 = new Tracker(new Tracker.TrackerBuilder(emitter, "tracker1", "app", getContext())
                .sessionContext(true)
                .foregroundTimeout(20)
                .backgroundTimeout(20)
        );
        Tracker tracker2 = new Tracker(new Tracker.TrackerBuilder(emitter, "tracker2", "app", getContext())
                .sessionContext(true)
                .foregroundTimeout(20)
                .backgroundTimeout(20)
        );
        Session session1 = tracker1.getSession();
        Session session2 = tracker2.getSession();

        session1.getSessionContext("session1-fake-id1", timestamp, false);
        session2.getSessionContext("session2-fake-id1", timestamp, false);

        long initialValue1 = session1.getSessionIndex();
        String id1 = session1.getState().getSessionId();
        long initialValue2 = session2.getSessionIndex();

        // Retrigger session in tracker1
        // The timeout is 20s, this sleep is only 2s - it's still the same session
        Thread.sleep(2000);
        session1.getSessionContext("session1-fake-id2", timestamp, false);

        // Retrigger timedout session in tracker2
        // 20s has then passed. Session must be updated, increasing the sessionIndex by 1
        Thread.sleep(18000);
        session2.getSessionContext("session2-fake-id2", timestamp, false);

        // Check sessions have the correct state
        assertEquals(0, session1.getSessionIndex() - initialValue1);
        assertEquals(1, session2.getSessionIndex() - initialValue2);
        String id2 = session2.getState().getSessionId();

        // Recreate tracker2
        Tracker tracker2b = new Tracker(new Tracker.TrackerBuilder(emitter, "tracker2", "app", getContext())
                .sessionContext(true)
                .foregroundTimeout(20)
                .backgroundTimeout(20)
        );
        tracker2b.getSession().getSessionContext("session2b-fake-id3", timestamp, false);
        long initialValue2b = tracker2b.getSession().getSessionIndex();
        String previousId2b = tracker2b.getSession().getState().getPreviousSessionId();

        // Check the new tracker session gets the data from the old tracker2 session
        assertEquals(initialValue2 + 2, initialValue2b);
        assertEquals(id2, previousId2b);
        assertNotEquals(id1, previousId2b);
    }

    @Test
    public void testAnonymisesUserAndPreviousSessionIdentifiers() {
        Session session = new Session(600, 300, TimeUnit.SECONDS, null, getContext());
        getSessionContext(session, "eid1", 1000, false);
        session.startNewSession(); // so that a reference to previous session is created
        Map<String, Object> context = getSessionContext(session, "eid2", 1001, true);

        assertEquals("00000000-0000-0000-0000-000000000000", context.get(Parameters.SESSION_USER_ID));
        assertNull(context.get(Parameters.SESSION_PREVIOUS_ID));
    }

    // Private methods

    private Session getSession(long foregroundTimeout, long backgroundTimeout) {
        getContext().getSharedPreferences(TrackerConstants.SNOWPLOW_SESSION_VARS, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
        return new Session(foregroundTimeout, backgroundTimeout, TimeUnit.SECONDS, null, getContext());
    }

    private Map<String, Object> getSessionContext(Session session, String eventId, long eventTimestamp, boolean userAnonymisation) {
        return (Map<String, Object>) session.getSessionContext(eventId, eventTimestamp, userAnonymisation).getMap().get(Parameters.DATA);
    }

    private void cleanSharedPreferences(Context context, String sharedPreferencesName) {
        FileStore.deleteFile(sharedPreferencesName, context);
        if (Build.VERSION.SDK_INT >= 24) {
            context.deleteSharedPreferences(sharedPreferencesName);
        } else {
            context.getSharedPreferences(sharedPreferencesName, Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .commit();
        }
    }

    private Context getContext() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }
}
