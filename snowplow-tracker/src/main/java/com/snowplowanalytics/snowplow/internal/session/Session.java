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

package com.snowplowanalytics.snowplow.internal.session;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.controller.SessionController;
import com.snowplowanalytics.snowplow.internal.tracker.Tracker;
import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.internal.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.internal.tracker.Logger;
import com.snowplowanalytics.snowplow.internal.utils.Util;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.util.TimeMeasure;

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
 *
 * @deprecated Use {@link com.snowplowanalytics.snowplow.configuration.SessionConfiguration SessionConfiguration}
 * for the configuration or {@link com.snowplowanalytics.snowplow.controller.SessionController SessionController}
 * for the runtime information and setup.
 */
@Deprecated
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
    private boolean isNewSession;
    private boolean isSessionCheckerEnabled;
    private long foregroundTimeout;
    private long backgroundTimeout;

    // Transition callbacks
    private Runnable foregroundTransitionCallback = null;
    private Runnable backgroundTransitionCallback = null;
    private Runnable foregroundTimeoutCallback = null;
    private Runnable backgroundTimeoutCallback = null;

    private SharedPreferences sharedPreferences;
    private String sessionVarsName;

    @NonNull
    public synchronized static Session getInstance(@NonNull Context context,
                                                   long foregroundTimeout,
                                                   long backgroundTimeout,
                                                   @NonNull TimeUnit timeUnit,
                                                   @Nullable Runnable[] sessionCallbacks)
    {
        return Session.getInstance(context, foregroundTimeout, backgroundTimeout, timeUnit, null, sessionCallbacks);
    }

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
    @Deprecated
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

    @Deprecated
    public Session(long foregroundTimeout, long backgroundTimeout, @NonNull TimeUnit timeUnit, @NonNull Context context) {
        this(foregroundTimeout, backgroundTimeout, timeUnit, null, context);
    }

    @Deprecated
    public Session(long foregroundTimeout, long backgroundTimeout, @NonNull TimeUnit timeUnit, @Nullable String namespace, @NonNull Context context) {
        this.foregroundTimeout = timeUnit.toMillis(foregroundTimeout);
        this.backgroundTimeout = timeUnit.toMillis(backgroundTimeout);
        isSessionCheckerEnabled = true;
        isNewSession = true;

        sessionVarsName = TrackerConstants.SNOWPLOW_SESSION_VARS;
        if (namespace != null && !namespace.isEmpty()) {
            String sessionVarsSuffix = namespace.replaceAll("[^a-zA-Z0-9_]+", "-");
            sessionVarsName = TrackerConstants.SNOWPLOW_SESSION_VARS + "_" + sessionVarsSuffix;
        }

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
        if (isNewSession) {
            return true;
        }
        long now = System.currentTimeMillis();
        long timeout = isBackground.get() ? backgroundTimeout : foregroundTimeout;
        return now < lastSessionCheck || now - lastSessionCheck > timeout;
    }

    private synchronized void updateSession(String eventId) {
        isNewSession = false;
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
        editor.putString(Parameters.SESSION_USER_ID, userId);
        editor.putString(Parameters.SESSION_ID, currentSessionId);
        editor.putString(Parameters.SESSION_PREVIOUS_ID, previousSessionId);
        editor.putInt(Parameters.SESSION_INDEX, sessionIndex);
        editor.putString(Parameters.SESSION_FIRST_ID, firstId);
        editor.putString(Parameters.SESSION_STORAGE, sessionStorage);
        editor.apply();
    }

    /**
     * @deprecated Use {@link SessionController#startNewSession()}
     */
    public void startNewSession() {
        isNewSession = true;
    }

    /**
     * Changes the truth of isBackground.
     * @param isBackground whether the application is in
     *                     the background or not
     */
    public void setIsBackground(boolean isBackground) {
        Logger.d(TAG, "Application is in the background: %s", isBackground);

        // If we are currently in the background and the new state is
        // foreground restart session checking
        boolean currentState = this.isBackground.get();
        if (currentState && !isBackground) {
            Logger.d(TAG, "Application moved to foreground, starting session checking...");
            this.executeEventCallback(this.foregroundTransitionCallback);
            try {
                Tracker.instance().resumeSessionChecking();
            } catch (Exception e) {
                Logger.e(TAG, "Could not resume checking as tracker not setup. Exception: %s", e);
            }
        }

        if (!currentState && isBackground) {
            Logger.d(TAG, "Application moved to background");
            this.executeEventCallback(this.backgroundTransitionCallback);
        }

        this.isBackground.set(isBackground);
    }

    /**
     * @deprecated Use {@link SessionController#isInBackground()}
     */
    public boolean isBackground() {
        return isBackground.get();
    }

    /**
     * Changes the truth of isSuspended
     * @deprecated Use {@link SessionController#pause()}
     * @param isSuspended whether the session tracking is suspended,
     *                    i.e. calls to update session are ignored,
     *                    but access time is changed to current time.
     *
     */
    public void setIsSuspended(boolean isSuspended) {
        Logger.d(TAG, "Session is suspended: %s", isSuspended);
        isSessionCheckerEnabled = !isSuspended;
    }

    void setBackgroundIndex(int backgroundIndex) {
        this.backgroundIndex = backgroundIndex;
    }

    int getBackgroundIndex() {
        return backgroundIndex;
    }

    void setForegroundIndex(int foregroundIndex) {
        this.foregroundIndex = foregroundIndex;
    }

    int getForegroundIndex() {
        return foregroundIndex;
    }

    /**
     * Returns the values for the session context.
     * @deprecated Use {@link SessionController}
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
        SharedPreferences sharedPreferences = context.getSharedPreferences(sessionVarsName, Context.MODE_PRIVATE);
        if (sharedPreferences.contains(Parameters.SESSION_USER_ID)) {
            return sharedPreferences;
        } else {
            sharedPreferences = context.getSharedPreferences(TrackerConstants.SNOWPLOW_SESSION_VARS, Context.MODE_PRIVATE);
            if (sharedPreferences.contains(Parameters.SESSION_USER_ID)) {
                return sharedPreferences;
            }
        }
        return null;
    }

    /**
     * Set session callbacks
     */
    public void setCallbacks(@NonNull Runnable[] callbacks) {
        if (callbacks.length == 4) {
            this.foregroundTransitionCallback = callbacks[0];
            this.backgroundTransitionCallback = callbacks[1];
            this.foregroundTimeoutCallback = callbacks[2];
            this.backgroundTimeoutCallback = callbacks[3];
        }
    }

    /**
     * @deprecated Use {@link SessionController#getSessionIndex()}
     * @return the session index
     */
    public int getSessionIndex() {
        return this.sessionIndex;
    }

    /**
     * @deprecated Use {@link SessionController#getUserId()}
     * @return the user id
     */
    @NonNull
    public String getUserId() {
        return this.userId;
    }

    /**
     * @deprecated Use {@link SessionController#getSessionId()}
     * @return the current session id
     */
    @NonNull
    public String getCurrentSessionId() {
        return this.currentSessionId;
    }

    /**
     * @return the previous session id or an
     *         empty String
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
    @NonNull
    public String getFirstId() {
        return this.firstId;
    }

    /**
     * @deprecated Use {@link SessionController#setForegroundTimeout(TimeMeasure)}
     * Set foreground timeout
     */
    public void setForegroundTimeout(long foregroundTimeout) {
        this.foregroundTimeout = foregroundTimeout;
    }

    /**
     * @deprecated Use {@link SessionController#getForegroundTimeout()}
     * @return the foreground session timeout
     */
    public long getForegroundTimeout() {
        return this.foregroundTimeout;
    }

    /**
     * @deprecated Use {@link SessionController#setBackgroundTimeout(TimeMeasure)}
     * Set background timeout
     */
    public void setBackgroundTimeout(long backgroundTimeout) {
        this.backgroundTimeout = backgroundTimeout;
    }

    /**
     * @deprecated Use {@link SessionController#getBackgroundTimeout()}
     * @return the background session timeout
     */
    public long getBackgroundTimeout() {
        return this.backgroundTimeout;
    }
}
