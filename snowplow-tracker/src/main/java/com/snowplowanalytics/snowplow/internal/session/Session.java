/*
 * Copyright (c) 2015-2021 Snowplow Analytics Ltd. All rights reserved.
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

package com.snowplowanalytics.snowplow.internal.session;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.StrictMode;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.internal.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.internal.tracker.Logger;
import com.snowplowanalytics.snowplow.internal.utils.Util;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Component that generate a Session context
 * which gets appended to each event sent from
 * the Tracker and changes based on:
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
    private static final String TAG = Session.class.getSimpleName();

    // Session Variables
    private String userId;
    private String currentSessionId = null;
    private String previousSessionId;
    private int sessionIndex = 0;
    private int backgroundIndex = 0;
    private int foregroundIndex = 0;
    private final String sessionStorage = "LOCAL_STORAGE";
    private String firstId = null;

    // Variables to control Session Updates
    private final AtomicBoolean isBackground = new AtomicBoolean(false);
    private long lastSessionCheck;
    private final AtomicBoolean isNewSession = new AtomicBoolean(true);
    private boolean isSessionCheckerEnabled;
    private long foregroundTimeout;
    private long backgroundTimeout;

    // Transition callbacks
    private Runnable foregroundTransitionCallback = null;
    private Runnable backgroundTransitionCallback = null;
    private Runnable foregroundTimeoutCallback = null;
    private Runnable backgroundTimeoutCallback = null;

    private SharedPreferences sharedPreferences;

    /**
     * Creates a new Session object which will
     * update itself overtime.
     *
     * @param context the android context
     * @param foregroundTimeout the amount of time that can elapse before the
     *                          session id is updated while the app is in the
     *                          foreground.
     * @param backgroundTimeout the amount of time that can elapse before the
     *                          session id is updated while the app is in the
     *                          background.
     * @param timeUnit the time units of the timeout measurements
     * @param namespace the namespace used by the session.
     * @param sessionCallbacks Called when the app change state or when timeout is triggered.
     */
    @NonNull
    public synchronized static Session getInstance(@NonNull Context context,
                                                   long foregroundTimeout,
                                                   long backgroundTimeout,
                                                   @NonNull TimeUnit timeUnit,
                                                   @Nullable String namespace,
                                                   @Nullable Runnable[] sessionCallbacks)
    {
        Session session = new Session(foregroundTimeout, backgroundTimeout, timeUnit, namespace, context);
        Runnable[] callbacks = {null, null, null, null};
        if (sessionCallbacks.length == 4) {
            callbacks = sessionCallbacks;
        }
        session.foregroundTransitionCallback = callbacks[0];
        session.backgroundTransitionCallback = callbacks[1];
        session.foregroundTimeoutCallback = callbacks[2];
        session.backgroundTimeoutCallback = callbacks[3];
        return session;
    }

    @SuppressLint("ApplySharedPref")
    public Session(long foregroundTimeout, long backgroundTimeout, @NonNull TimeUnit timeUnit, @Nullable String namespace, @NonNull Context context) {
        this.foregroundTimeout = timeUnit.toMillis(foregroundTimeout);
        this.backgroundTimeout = timeUnit.toMillis(backgroundTimeout);
        isSessionCheckerEnabled = true;

        String sessionVarsName = TrackerConstants.SNOWPLOW_SESSION_VARS;
        if (namespace != null && !namespace.isEmpty()) {
            String sessionVarsSuffix = namespace.replaceAll("[^a-zA-Z0-9_]+", "-");
            sessionVarsName = TrackerConstants.SNOWPLOW_SESSION_VARS + "_" + sessionVarsSuffix;
        }

        StrictMode.ThreadPolicy oldPolicy = StrictMode.allowThreadDiskReads();
        try {
            sharedPreferences = getSessionFromSharedPreferences(context, sessionVarsName);
            if (sharedPreferences != null) {
                userId = sharedPreferences.getString(Parameters.SESSION_USER_ID, Util.getUUIDString());
                currentSessionId = sharedPreferences.getString(Parameters.SESSION_ID, null);
                sessionIndex = sharedPreferences.getInt(Parameters.SESSION_INDEX, 0);
            } else {
                Map<String, Object> sessionInfo = getSessionFromFile(context);
                if (sessionInfo != null) {
                    try {
                        userId = sessionInfo.get(Parameters.SESSION_USER_ID).toString();
                        currentSessionId = sessionInfo.get(Parameters.SESSION_ID).toString();
                        sessionIndex = (int) sessionInfo.get(Parameters.SESSION_INDEX);
                    } catch (Exception e) {
                        Logger.track(TAG, String.format("Exception occurred retrieving session info from file: %s", e), e);
                        userId = Util.getUUIDString();
                    }
                } else {
                    userId = Util.getUUIDString();
                }
            }
            // Force sharedPreferences to be the correct one.
            sharedPreferences = context.getSharedPreferences(TrackerConstants.SNOWPLOW_SESSION_VARS, Context.MODE_PRIVATE);
            lastSessionCheck = System.currentTimeMillis();
        } finally {
            StrictMode.setThreadPolicy(oldPolicy);
        }

        // Get or Set the Session UserID
        SharedPreferences generalPref = context.getSharedPreferences(TrackerConstants.SNOWPLOW_GENERAL_VARS, Context.MODE_PRIVATE);
        String storedUserId = generalPref.getString(TrackerConstants.INSTALLATION_USER_ID, null);
        if (storedUserId != null) {
            userId = storedUserId;
        } else if (userId != null) {
            generalPref.edit()
                    .putString(TrackerConstants.INSTALLATION_USER_ID, userId)
                    .commit();
        }

        Logger.v(TAG, "Tracker Session Object created.");
    }

    /**
     * Returns the session context
     *
     * @return a SelfDescribingJson containing the session context
     */
    @NonNull
    public synchronized SelfDescribingJson getSessionContext(@NonNull String eventId) {
        Logger.v(TAG, "Getting session context...");
        if (!isSessionCheckerEnabled) {
            return new SelfDescribingJson(TrackerConstants.SESSION_SCHEMA, getSessionValues());
        }
        if (shouldUpdateSession()) {
            Logger.d(TAG, "Update session information.");
            updateSession(eventId);
        }
        lastSessionCheck = System.currentTimeMillis();
        return new SelfDescribingJson(TrackerConstants.SESSION_SCHEMA, getSessionValues());
    }

    private void executeEventCallback(Runnable callback) {
        if (callback != null) {
            try {
                callback.run();
            } catch (Exception e) {
                Logger.e(TAG, "Session event callback failed");
            }
        }
    }

    private boolean shouldUpdateSession() {
        if (isNewSession.get()) {
            return true;
        }
        long now = System.currentTimeMillis();
        long timeout = isBackground.get() ? backgroundTimeout : foregroundTimeout;
        return now < lastSessionCheck || now - lastSessionCheck > timeout;
    }

    private synchronized void updateSession(String eventId) {
        isNewSession.set(false);
        firstId = eventId;
        previousSessionId = this.currentSessionId;
        currentSessionId = Util.getUUIDString();
        sessionIndex++;

        Logger.d(TAG, "Session information is updated:");
        Logger.d(TAG, " + Session ID: %s", currentSessionId);
        Logger.d(TAG, " + Previous Session ID: %s", previousSessionId);
        Logger.d(TAG, " + Session Index: %s", sessionIndex);

        boolean isBackground = this.isBackground.get();

        if (isBackground) { // timed out in background
            this.executeEventCallback(backgroundTimeoutCallback);
        } else { // timed out in foreground
            this.executeEventCallback(foregroundTimeoutCallback);
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Parameters.SESSION_ID, currentSessionId);
        editor.putString(Parameters.SESSION_PREVIOUS_ID, previousSessionId);
        editor.putInt(Parameters.SESSION_INDEX, sessionIndex);
        editor.putString(Parameters.SESSION_FIRST_ID, firstId);
        editor.putString(Parameters.SESSION_STORAGE, sessionStorage);
        editor.apply();
    }

    public void startNewSession() {
        isNewSession.set(true);
    }

    /**
     * Updates the session with information about lifecycle tracking.
     * Note: Internal use only.
     * @param isForeground whether or not the application moved to foreground.
     * @return foreground or background index. Returns -1 if the lifecycle state is not changed.
     */
    public synchronized int updateLifecycleNotification(boolean isForeground) {
        boolean toBackground = !isForeground;
        // if the new lifecycle state confirms the session state, there isn't any lifecycle transition
        if (isBackground.get() == toBackground) {
            return -1;
        }
        Logger.d(TAG, "Application is in the background: %s", toBackground);
        isBackground.set(toBackground);

        if (!toBackground) {
            Logger.d(TAG, "Application moved to foreground, starting session checking...");
            this.executeEventCallback(this.foregroundTransitionCallback);
            try {
                setIsSuspended(false);
            } catch (Exception e) {
                Logger.e(TAG, "Could not resume checking as tracker not setup. Exception: %s", e);
            }
            foregroundIndex++;
            return foregroundIndex;
        } else {
            Logger.d(TAG, "Application moved to background");
            this.executeEventCallback(this.backgroundTransitionCallback);
            backgroundIndex++;
            return backgroundIndex;
        }
    }

    public boolean isBackground() {
        return isBackground.get();
    }

    /**
     * Changes the truth of isSuspended
     * @param isSuspended whether the session tracking is suspended,
     *                    i.e. calls to update session are ignored,
     *                    but access time is changed to current time.
     *
     */
    public void setIsSuspended(boolean isSuspended) {
        Logger.d(TAG, "Session is suspended: %s", isSuspended);
        isSessionCheckerEnabled = !isSuspended;
    }

    int getBackgroundIndex() {
        return backgroundIndex;
    }

    int getForegroundIndex() {
        return foregroundIndex;
    }

    /**
     * Returns the values for the session context.
     * @return a map containing all session values
     */
    @NonNull
    public Map<String, Object> getSessionValues() {
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
     * Gets the session information from a file.
     *
     * @return a map or null.
     */
    @Nullable
    private Map<String, Object> getSessionFromFile(@NonNull Context context) {
        return FileStore.getMapFromFile(
                TrackerConstants.SNOWPLOW_SESSION_VARS,
                context);
    }

    @Nullable
    private SharedPreferences getSessionFromSharedPreferences(@NonNull Context context, @NonNull String sessionVarsName) {
        StrictMode.ThreadPolicy oldPolicy = StrictMode.allowThreadDiskReads();
        try {
            SharedPreferences sharedPreferences = context.getSharedPreferences(sessionVarsName, Context.MODE_PRIVATE);
            if (sharedPreferences.contains(Parameters.SESSION_ID)) {
                return sharedPreferences;
            } else {
                sharedPreferences = context.getSharedPreferences(TrackerConstants.SNOWPLOW_SESSION_VARS, Context.MODE_PRIVATE);
                if (sharedPreferences.contains(Parameters.SESSION_ID)) {
                    return sharedPreferences;
                }
            }
        } finally {
            StrictMode.setThreadPolicy(oldPolicy);
        }
        return null;
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
    @NonNull
    public String getUserId() {
        return this.userId;
    }

    /**
     * @return the current session id
     */
    @NonNull
    public String getCurrentSessionId() {
        return this.currentSessionId;
    }

    /**
     * @return the previous session id or an empty String
     */
    @Nullable
    public String getPreviousSessionId() {
        return this.previousSessionId;
    }

    /**
     * @return the session storage type
     */
    @NonNull
    public String getSessionStorage() {
        return this.sessionStorage;
    }

    /**
     * @return the first event id
     */
    @Nullable
    public String getFirstId() {
        return this.firstId;
    }

    /**
     * Set foreground timeout
     */
    public void setForegroundTimeout(long foregroundTimeout) {
        this.foregroundTimeout = foregroundTimeout;
    }

    /**
     * @return the foreground session timeout
     */
    public long getForegroundTimeout() {
        return this.foregroundTimeout;
    }

    /**
     * Set background timeout
     */
    public void setBackgroundTimeout(long backgroundTimeout) {
        this.backgroundTimeout = backgroundTimeout;
    }

    /**
     * @return the background session timeout
     */
    public long getBackgroundTimeout() {
        return this.backgroundTimeout;
    }
}
