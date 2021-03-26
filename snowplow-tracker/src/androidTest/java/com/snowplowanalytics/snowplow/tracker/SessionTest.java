/*
 * Copyright (c) 2015-2020 Snowplow Analytics Ltd. All rights reserved.
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
import android.test.AndroidTestCase;

import com.snowplowanalytics.snowplow.event.Event;
import com.snowplowanalytics.snowplow.event.Structured;
import com.snowplowanalytics.snowplow.internal.emitter.Emitter;
import com.snowplowanalytics.snowplow.internal.session.Session;
import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.internal.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.internal.tracker.Tracker;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.internal.session.FileStore;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotEquals;

public class SessionTest extends AndroidTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Context context = getContext();
        FileStore.deleteFile(TrackerConstants.SNOWPLOW_SESSION_VARS, context);
        if (Build.VERSION.SDK_INT >= 24) {
            context.deleteSharedPreferences(TrackerConstants.SNOWPLOW_SESSION_VARS);
        } else {
            SharedPreferences.Editor editor =
                    context.getSharedPreferences(TrackerConstants.SNOWPLOW_SESSION_VARS, Context.MODE_PRIVATE).edit();
            editor.remove(Parameters.SESSION_USER_ID);
            editor.remove(Parameters.SESSION_ID);
            editor.remove(Parameters.SESSION_PREVIOUS_ID);
            editor.remove(Parameters.SESSION_INDEX);
            editor.remove(Parameters.SESSION_FIRST_ID);
            editor.remove(Parameters.SESSION_STORAGE);
            editor.commit();
        }
    }

    public void testSessionInit() {
        Session session = getSession(600, 300);

        assertNotNull(session);
        assertEquals(600000, session.getForegroundTimeout());
        assertEquals(300000, session.getBackgroundTimeout());
        assertEquals("LOCAL_STORAGE", session.getSessionStorage());
        assertNull(session.getPreviousSessionId());
        assertNotNull(session.getUserId());
        assertNull(session.getFirstId());

        SelfDescribingJson sdj = session.getSessionContext("first-id-1");
        assertEquals("first-id-1", session.getFirstId());
        sdj = session.getSessionContext("second-id-2");
        assertEquals("first-id-1", session.getFirstId());

        assertEquals(TrackerConstants.SESSION_SCHEMA, sdj.getMap().get("schema"));
    }

    public void testFirstSession() {
        Session session = getSession(3, 3);

        Map<String, Object> sessionContext = getSessionContext(session,"event_1");
        String sessionId = (String)sessionContext.get(Parameters.SESSION_ID);
        assertEquals(1, session.getSessionIndex());
        assertNotNull(sessionContext.get(Parameters.SESSION_INDEX));
        assertEquals(1, sessionContext.get(Parameters.SESSION_INDEX));
        assertEquals("event_1", sessionContext.get(Parameters.SESSION_FIRST_ID));
    }

    public void testEventsOnSameSession() throws InterruptedException {
        Session session = getSession(5, 0);

        Map<String, Object> sessionContext = getSessionContext(session, "event_1");
        String sessionId = (String)sessionContext.get(Parameters.SESSION_ID);
        assertNotNull(sessionId);
        assertEquals(1, sessionContext.get(Parameters.SESSION_INDEX));
        assertEquals("event_1", sessionContext.get(Parameters.SESSION_FIRST_ID));

        Thread.sleep(1000);

        sessionContext = getSessionContext(session, "event_2");
        assertEquals(sessionId, (String)sessionContext.get(Parameters.SESSION_ID));
        assertEquals(1, sessionContext.get(Parameters.SESSION_INDEX));
        assertEquals("event_1", sessionContext.get(Parameters.SESSION_FIRST_ID));

        Thread.sleep(1000);

        sessionContext = getSessionContext(session, "event_3");
        assertEquals(sessionId, (String)sessionContext.get(Parameters.SESSION_ID));
        assertEquals(1, sessionContext.get(Parameters.SESSION_INDEX));
        assertEquals("event_1", sessionContext.get(Parameters.SESSION_FIRST_ID));

        Thread.sleep(5100);

        sessionContext = getSessionContext(session, "event_4");
        assertEquals(sessionId, (String)sessionContext.get(Parameters.SESSION_PREVIOUS_ID));
        assertEquals(2, sessionContext.get(Parameters.SESSION_INDEX));
        assertEquals("event_4", sessionContext.get(Parameters.SESSION_FIRST_ID));
    }

    public void testBackgroundEventsOnSameSession() throws InterruptedException {
        Session session = getSession(0, 5);

        session.setIsBackground(true);

        Map<String, Object> sessionContext = getSessionContext(session, "event_1");
        String sessionId = (String)sessionContext.get(Parameters.SESSION_ID);
        assertNotNull(sessionId);
        assertEquals(1, sessionContext.get(Parameters.SESSION_INDEX));
        assertEquals("event_1", sessionContext.get(Parameters.SESSION_FIRST_ID));

        Thread.sleep(1000);

        sessionContext = getSessionContext(session, "event_2");
        assertEquals(sessionId, (String)sessionContext.get(Parameters.SESSION_ID));
        assertEquals(1, sessionContext.get(Parameters.SESSION_INDEX));
        assertEquals("event_1", sessionContext.get(Parameters.SESSION_FIRST_ID));

        Thread.sleep(1000);

        sessionContext = getSessionContext(session, "event_3");
        assertEquals(sessionId, (String)sessionContext.get(Parameters.SESSION_ID));
        assertEquals(1, sessionContext.get(Parameters.SESSION_INDEX));
        assertEquals("event_1", sessionContext.get(Parameters.SESSION_FIRST_ID));

        Thread.sleep(5100);

        sessionContext = getSessionContext(session, "event_4");
        assertEquals(sessionId, (String)sessionContext.get(Parameters.SESSION_PREVIOUS_ID));
        assertEquals(2, sessionContext.get(Parameters.SESSION_INDEX));
        assertEquals("event_4", sessionContext.get(Parameters.SESSION_FIRST_ID));
    }

    public void testMixedEventsOnManySessions() throws InterruptedException {
        Session session = getSession(1, 1);

        Map<String, Object> sessionContext = getSessionContext(session, "event_1");
        String sessionId = (String)sessionContext.get(Parameters.SESSION_ID);
        assertNotNull(sessionId);
        assertEquals(1, sessionContext.get(Parameters.SESSION_INDEX));
        assertEquals("event_1", sessionContext.get(Parameters.SESSION_FIRST_ID));
        String oldSessionId = sessionId;

        session.setIsBackground(true);
        Thread.sleep(1100);

        sessionContext = getSessionContext(session, "event_2");
        sessionId = (String)sessionContext.get(Parameters.SESSION_ID);
        assertEquals(oldSessionId, (String)sessionContext.get(Parameters.SESSION_PREVIOUS_ID));
        assertEquals(2, sessionContext.get(Parameters.SESSION_INDEX));
        assertEquals("event_2", sessionContext.get(Parameters.SESSION_FIRST_ID));
        oldSessionId = sessionId;

        session.setIsBackground(false);
        Thread.sleep(1100);

        sessionContext = getSessionContext(session, "event_3");
        sessionId = (String)sessionContext.get(Parameters.SESSION_ID);
        assertEquals(oldSessionId, (String)sessionContext.get(Parameters.SESSION_PREVIOUS_ID));
        assertEquals(3, sessionContext.get(Parameters.SESSION_INDEX));
        assertEquals("event_3", sessionContext.get(Parameters.SESSION_FIRST_ID));
        oldSessionId = sessionId;

        session.setIsBackground(true);
        Thread.sleep(1100);

        sessionContext = getSessionContext(session, "event_4");
        assertEquals(oldSessionId, (String)sessionContext.get(Parameters.SESSION_PREVIOUS_ID));
        assertEquals(4, sessionContext.get(Parameters.SESSION_INDEX));
        assertEquals("event_4", sessionContext.get(Parameters.SESSION_FIRST_ID));
    }

    public void testTimeoutSessionWhenPauseAndResume() throws InterruptedException {
        Session session = getSession(1, 1);

        Map<String, Object> sessionContext = getSessionContext(session, "event_1");
        Integer oldSessionIndex = (Integer)sessionContext.get(Parameters.SESSION_INDEX);
        String prevSessionId = (String)sessionContext.get(Parameters.SESSION_ID);
        assertEquals("event_1", sessionContext.get(Parameters.SESSION_FIRST_ID));

        session.setIsSuspended(true);
        Thread.sleep(2000);

        sessionContext = getSessionContext(session, "event_2");
        assertEquals(oldSessionIndex, sessionContext.get(Parameters.SESSION_INDEX));
        assertEquals(prevSessionId, (String)sessionContext.get(Parameters.SESSION_ID));
        assertEquals("event_1", sessionContext.get(Parameters.SESSION_FIRST_ID));
        prevSessionId = (String)sessionContext.get(Parameters.SESSION_ID);

        session.setIsSuspended(false);

        sessionContext = getSessionContext(session, "event_3");
        assertEquals(oldSessionIndex + 1, sessionContext.get(Parameters.SESSION_INDEX));
        assertEquals(prevSessionId, (String)sessionContext.get(Parameters.SESSION_PREVIOUS_ID));
        assertEquals("event_3", sessionContext.get(Parameters.SESSION_FIRST_ID));
    }

    public void testNoEventsForLongTimeDontIncreaseSessionIndexMultipleTimes() throws InterruptedException {
        Session session = getSession(1, 1);

        Map<String, Object> sessionContext = getSessionContext(session, "event_1");
        Integer oldSessionIndex = (Integer)sessionContext.get(Parameters.SESSION_INDEX);
        assertEquals("event_1", sessionContext.get(Parameters.SESSION_FIRST_ID));

        Thread.sleep(4000);

        sessionContext = getSessionContext(session, "event_2");
        assertEquals(oldSessionIndex + 1, sessionContext.get(Parameters.SESSION_INDEX));
        assertEquals("event_2", sessionContext.get(Parameters.SESSION_FIRST_ID));
    }

    public void testSessionInitWithIncompletePersistedData() {
        SharedPreferences prefs = getContext().getSharedPreferences(TrackerConstants.SNOWPLOW_SESSION_VARS, Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
        prefs.edit().putString(Parameters.SESSION_USER_ID, UUID.randomUUID().toString()).commit();

        Session session = new Session(600, 300, TimeUnit.SECONDS, getContext());

        assertTrue(session.waitForSessionFileLoad());
        assertNotNull(session);
        assertEquals(600000, session.getForegroundTimeout());
        assertEquals(300000, session.getBackgroundTimeout());
        assertEquals("LOCAL_STORAGE", session.getSessionStorage());
        assertNull(session.getPreviousSessionId());
        assertNotNull(session.getUserId());
        assertNull(session.getFirstId());
    }

    public void testStartNewSessionRenewTheSession() throws InterruptedException {
        Session session = getSession(3, 3);

        Map<String, Object> sessionContext = getSessionContext(session, "event_1");
        String sessionId = (String)sessionContext.get(Parameters.SESSION_ID);
        assertNotNull(sessionId);
        assertEquals(1, sessionContext.get(Parameters.SESSION_INDEX));
        assertEquals("event_1", sessionContext.get(Parameters.SESSION_FIRST_ID));

        Thread.sleep(200);
        session.startNewSession();

        sessionContext = getSessionContext(session, "event_2");
        assertEquals(sessionId, (String)sessionContext.get(Parameters.SESSION_PREVIOUS_ID));
        assertEquals(2, sessionContext.get(Parameters.SESSION_INDEX));
        assertEquals("event_2", sessionContext.get(Parameters.SESSION_FIRST_ID));
    }

    public void testMultipleTrackersUpdateDifferentSessions() throws InterruptedException {
        Emitter emitter = new Emitter.EmitterBuilder("", getContext()).build();
        Tracker tracker1 = new Tracker.TrackerBuilder(emitter, "tracker1", "app", getContext())
                .sessionContext(true)
                .foregroundTimeout(10)
                .backgroundTimeout(10)
                .build();
        Tracker tracker2 = new Tracker.TrackerBuilder(emitter, "tracker2", "app", getContext())
                .sessionContext(true)
                .foregroundTimeout(10)
                .backgroundTimeout(10)
                .build();
        Session session1 = tracker1.getSession();
        Session session2 = tracker2.getSession();
        session1.waitForSessionFileLoad();
        session2.waitForSessionFileLoad();

        session1.getSessionContext("fake-id");
        session2.getSessionContext("fake-id");

        long initialValue1 = session1.getSessionIndex();
        String id1 = session1.getCurrentSessionId();
        long initialValue2 = session2.getSessionIndex();
        String id2 = session2.getCurrentSessionId();

        // Retrigger session in tracker1
        Thread.sleep(7000);
        session1.getSessionContext("fake-id");
        Thread.sleep(5000);

        // Retrigger timedout session in tracker2
        session2.getSessionContext("fake-id");
        id2 = session2.getCurrentSessionId();

        // Check sessions have the correct state
        assertEquals(0, session1.getSessionIndex() - initialValue1);
        assertEquals(1, session2.getSessionIndex() - initialValue2);

        // Recreate tracker2
        Tracker tracker2b = new Tracker.TrackerBuilder(emitter, "tracker2", "app", getContext())
                .sessionContext(true)
                .foregroundTimeout(10)
                .backgroundTimeout(10)
                .build();
        tracker2b.getSession().getSessionContext("fake-id");
        long initialValue2b = tracker2b.getSession().getSessionIndex();
        String previousId2b = tracker2b.getSession().getPreviousSessionId();

        // Check the new tracker session gets the data from the old tracker2 session
        assertEquals(initialValue2 + 2, initialValue2b);
        assertEquals(id2, previousId2b);
        assertNotEquals(id1, previousId2b);
    }

    // Private methods

    private Session getSession(long foregroundTimeout, long backgroundTimeout) {
        getContext().getSharedPreferences(TrackerConstants.SNOWPLOW_SESSION_VARS, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
        Session session = new Session(foregroundTimeout, backgroundTimeout, TimeUnit.SECONDS, getContext());
        session.waitForSessionFileLoad();
        return session;
    }

    private Map<String, Object> getSessionContext(Session session, String eventId) {
        return (Map<String, Object>)session.getSessionContext(eventId).getMap().get(Parameters.DATA);
    }
}
