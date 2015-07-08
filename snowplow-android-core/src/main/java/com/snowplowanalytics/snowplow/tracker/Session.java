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

import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.utils.Logger;
import com.snowplowanalytics.snowplow.tracker.utils.Util;
import com.snowplowanalytics.snowplow.tracker.utils.payload.SelfDescribingJson;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A Session object which gets appended to each
 * event sent from the Tracker and changes based
 * on:
 * - Timeout of use while app is in foreground
 * - Timeout of use while app is in background
 *
 * Essentially will update if it is not accessed within
 * a configurable timeout.
 */
public class Session {

    private static String TAG = Session.class.getSimpleName();

    // Session Variables
    private String userId;
    private String currentSessionId = "";
    private String previousSessionId;
    private int sessionIndex = 0;
    private String sessionStorage = "SQLITE";

    // Variables to control Session Updates
    private AtomicBoolean isBackground = new AtomicBoolean(false);
    private long accessedLast;
    private long foregroundTimeout;
    private long backgroundTimeout;

    /**
     * Creates a new Session object which will
     * update itself overtime.
     *
     * @param foregroundTimeout the amount of time that can elapse before the
     *                          session id is updated while the app is in the
     *                          foreground.
     * @param backgroundTimeout the amount of time that can elapse before the
     *                          session id is updated while the app is in the
     *                          background.
     *
     */
    public Session(long foregroundTimeout, long backgroundTimeout) {
        this.userId = Util.getEventId();
        this.foregroundTimeout = foregroundTimeout;
        this.backgroundTimeout = backgroundTimeout;
        updateSessionInfo();
        updateAccessedTime();

        Logger.v(TAG, "Tracker Session Object created.");
    }

    /**
     * Returns the session context
     *
     * @return a SelfDescribingJson containing the session context
     */
    public SelfDescribingJson getSessionContext() {
        Logger.v(TAG, "Getting session context...");
        updateAccessedTime();
        return new SelfDescribingJson(TrackerConstants.SESSION_SCHEMA, getSessionValues());
    }

    /**
     * Checks the current Session accessedTime and
     * determines whether the session id should be
     * incremented.
     */
    public void checkAndUpdateSession() {
        Logger.d(TAG, "Checking and updating session information.");

        long checkTime = System.currentTimeMillis();
        long range;

        if (this.isBackground.get()) {
            range = this.backgroundTimeout;
        } else {
            range = this.foregroundTimeout;
        }

        if (!Util.isTimeInRange(this.accessedLast, checkTime, range)) {
            updateSessionInfo();
            updateAccessedTime();
        }
    }

    /**
     * Changes the truth of isBackground.
     *
     * @param isBackground whether the application is in
     *                     the background or not
     */
    public void setIsBackground(boolean isBackground) {
        Logger.d(TAG, "Application is in the background: %s", isBackground);
        this.isBackground.set(isBackground);
    }

    /**
     * Returns the values for the session context.
     *
     * @return a map containing all session values
     */
    private Map getSessionValues() {
        Map<String, Object> sessionValues = new HashMap<>();
        sessionValues.put(Parameters.SESSION_USER_ID,this.userId);
        sessionValues.put(Parameters.SESSION_ID, this.currentSessionId);
        sessionValues.put(Parameters.SESSION_PREVIOUS_ID, this.previousSessionId);
        sessionValues.put(Parameters.SESSION_INDEX, this.sessionIndex);
        sessionValues.put(Parameters.SESSION_STORAGE, this.sessionStorage);
        return sessionValues;
    }

    /**
     * Updates the session id, previous session id
     * and the session index.
     */
    private void updateSessionInfo() {
        this.previousSessionId = this.currentSessionId;
        this.currentSessionId = Util.getEventId();
        this.sessionIndex++;

        Logger.d(TAG, "Session information is updating:");
        Logger.d(TAG, " + Session ID: %s", this.currentSessionId);
        Logger.d(TAG, " + Previous Session ID: %s", this.previousSessionId);
        Logger.d(TAG, " + Session Index: %s", this.sessionIndex);
    }

    /**
     * Updates the time the session was
     * last accessed.
     */
    private void updateAccessedTime() {
        this.accessedLast = System.currentTimeMillis();
    }

    /**
     * @return the session index
     */
    public int getSessionIndex() {
        return this.sessionIndex;
    }

    /**
     * @return the user id
     */
    public String getUserId() {
        return this.userId;
    }

    /**
     * @return the current session id
     */
    public String getCurrentSessionId() {
        return this.currentSessionId;
    }

    /**
     * @return the previous session id or an
     *         empty String
     */
    public String getPreviousSessionId() {
        return this.previousSessionId;
    }

    /**
     * @return the session storage type
     */
    public String getSessionStorage() {
        return this.sessionStorage;
    }
}

