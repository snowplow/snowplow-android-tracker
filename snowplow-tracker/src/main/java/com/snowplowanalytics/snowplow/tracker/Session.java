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

import android.content.Context;

import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.utils.Logger;
import com.snowplowanalytics.snowplow.tracker.utils.Util;
import com.snowplowanalytics.snowplow.tracker.utils.FileStore;
import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A Session object which gets appended to each
 * event sent from the Tracker and changes based
 * on:
 * - Timeout of use while app is in foreground
 * - Timeout of use while app is in background
 *
 * Session data is maintained for the life of the
 * application being installed on a device.
 *
 * Essentially will update if it is not accessed within
 * a configurable timeout.
 */
public class Session {

    private static String TAG = Session.class.getSimpleName();

    // Session Variables
    private String userId;
    private String currentSessionId = null;
    private String previousSessionId;
    private int sessionIndex = 0;
    private String sessionStorage = "SQLITE";
    private String firstId = null;

    // Variables to control Session Updates
    private AtomicBoolean isBackground = new AtomicBoolean(false);
    private long accessedLast;
    private long foregroundTimeout;
    private long backgroundTimeout;
    private Context context;

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
     * @param timeUnit the time units of the timeout measurements
     * @param context the android context
     */
    public Session(long foregroundTimeout, long backgroundTimeout, TimeUnit timeUnit, Context context) {
        this.foregroundTimeout = timeUnit.toMillis(foregroundTimeout);
        this.backgroundTimeout = timeUnit.toMillis(backgroundTimeout);
        this.context = context;
        Map sessionInfo = getSessionFromFile();
        if (sessionInfo == null) {
            this.userId = Util.getEventId();
        } else {
            try {
                String uid = sessionInfo.get(Parameters.SESSION_USER_ID).toString();
                String sid = sessionInfo.get(Parameters.SESSION_ID).toString();
                int si = (int) sessionInfo.get(Parameters.SESSION_INDEX);

                this.userId = uid;
                this.sessionIndex = si;
                this.currentSessionId = sid;
            } catch (Exception e){
                Logger.e(TAG, "Exception occurred retrieving session info from file: %s", e.getMessage());
                this.userId = Util.getEventId();
            }
        }

        updateSessionInfo();
        updateAccessedTime();

        Logger.v(TAG, "Tracker Session Object created.");
    }

    /**
     * Returns the session context
     *
     * @return a SelfDescribingJson containing the session context
     */
    public synchronized SelfDescribingJson getSessionContext(String firstId) {
        Logger.v(TAG, "Getting session context...");
        updateAccessedTime();
        if (this.firstId == null) {
            this.firstId = firstId;
        }
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
    public Map getSessionValues() {
        Map<String, Object> sessionValues = new HashMap<>();
        sessionValues.put(Parameters.SESSION_USER_ID,this.userId);
        sessionValues.put(Parameters.SESSION_ID, this.currentSessionId);
        sessionValues.put(Parameters.SESSION_PREVIOUS_ID, this.previousSessionId);
        sessionValues.put(Parameters.SESSION_INDEX, this.sessionIndex);
        sessionValues.put(Parameters.SESSION_STORAGE, this.sessionStorage);
        sessionValues.put(Parameters.SESSION_FIRST_ID, this.firstId);
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

        Logger.d(TAG, "Session information is updated:");
        Logger.d(TAG, " + Session ID: %s", this.currentSessionId);
        Logger.d(TAG, " + Previous Session ID: %s", this.previousSessionId);
        Logger.d(TAG, " + Session Index: %s", this.sessionIndex);

        saveSessionToFile();
    }

    /**
     * Saves the session information to internal storage.
     *
     * @return truth on save success.
     */
    private boolean saveSessionToFile() {
        return FileStore.saveMapToFile(
                TrackerConstants.SNOWPLOW_SESSION_VARS,
                getSessionValues(),
                context);
    }

    /**
     * Gets the session information from a file.
     *
     * @return a map or null.
     */
    private Map getSessionFromFile() {
        return FileStore.getMapFromFile(
                TrackerConstants.SNOWPLOW_SESSION_VARS,
                context);
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

    /**
     * @return the first event id
     */
    public String getFirstId() {
        return this.firstId;
    }

    /**
     * @return the foreground session timeout
     */
    public long getForegroundTimeout() {
        return this.foregroundTimeout;
    }

    /**
     * @return the background session timeout
     */
    public long getBackgroundTimeout() {
        return this.backgroundTimeout;
    }
}
