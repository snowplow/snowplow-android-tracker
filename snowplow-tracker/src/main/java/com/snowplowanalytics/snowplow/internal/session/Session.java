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

package com.snowplowanalytics.snowplow.internal.session;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.StrictMode;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.internal.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.internal.tracker.Logger;
import com.snowplowanalytics.snowplow.internal.utils.Util;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.SessionState;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
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
    private volatile int backgroundIndex = 0;
    private volatile int foregroundIndex = 0;
    private int eventIndex = 0;
    private SessionState state = null;

    // Variables to control Session Updates
    private final AtomicBoolean isBackground = new AtomicBoolean(false);
    private long lastSessionCheck;
    private final AtomicBoolean isNewSession = new AtomicBoolean(true);
    private volatile boolean isSessionCheckerEnabled;
    private long foregroundTimeout;
    private long backgroundTimeout;

    // Callbacks
    private Runnable foregroundTransitionCallback = null;
    private Runnable backgroundTransitionCallback = null;
    private Runnable foregroundTimeoutCallback = null;
    private Runnable backgroundTimeoutCallback = null;
    @Nullable
    public Consumer<SessionState> onSessionUpdate;

    // Session values persistence
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
            Map<String, Object> sessionInfo = getSessionMapFromLegacyTrackerV3(context, sessionVarsName);
            if (sessionInfo == null) {
                sessionInfo = getSessionMapFromLegacyTrackerV2(context, sessionVarsName);
                if (sessionInfo == null) {
                    try {
                        sessionInfo = getSessionMapFromLegacyTrackerV1(context);
                    } catch (Exception e) {
                        Logger.track(TAG, String.format("Exception occurred retrieving session info from file: %s", e), e);
                    }
                }
            }
            if (sessionInfo == null) {
                Logger.track(TAG, "No previous session info available");
            } else {
                state = SessionState.build(sessionInfo);
            }
            userId = retrieveUserId(context, state);
            sharedPreferences = context.getSharedPreferences(sessionVarsName, Context.MODE_PRIVATE);
            lastSessionCheck = System.currentTimeMillis();
        } finally {
            StrictMode.setThreadPolicy(oldPolicy);
        }
        Logger.v(TAG, "Tracker Session Object created.");
    }

    private synchronized static String retrieveUserId(Context context, SessionState state) {
        String userId = state != null ? state.getUserId() : Util.getUUIDString();
        // Session_UserID is available only if the session context is enabled.
        // In a future version we would like to make it available even if the session context is disabled.
        // For this reason, we store the Session_UserID in a separate storage (decoupled by session values)
        // calling it Installation_UserID in order to remark that it isn't related to the session context.
        // Although, for legacy, we need to copy its value in the Session_UserID of the session context
        // as the session context schema (and related data modelling) requires it.
        // For further details: https://discourse.snowplow.io/t/rfc-mobile-trackers-v2-0
        SharedPreferences generalPref = context.getSharedPreferences(TrackerConstants.SNOWPLOW_GENERAL_VARS, Context.MODE_PRIVATE);
        String storedUserId = generalPref.getString(TrackerConstants.INSTALLATION_USER_ID, null);
        if (storedUserId != null) {
            userId = storedUserId;
        } else {
            generalPref.edit()
                    .putString(TrackerConstants.INSTALLATION_USER_ID, userId)
                    .commit();
        }
        return userId;
    }

    /**
     * Returns the session context
     *
     * @return a SelfDescribingJson containing the session context
     */
    @NonNull
    public synchronized SelfDescribingJson getSessionContext(@NonNull String eventId, long eventTimestamp, boolean userAnonymisation) {
        Logger.v(TAG, "Getting session context...");
        if (isSessionCheckerEnabled) {
            if (shouldUpdateSession()) {
                Logger.d(TAG, "Update session information.");
                updateSession(eventId, eventTimestamp);

                if (isBackground.get()) { // timed out in background
                    this.executeEventCallback(backgroundTimeoutCallback);
                } else { // timed out in foreground
                    this.executeEventCallback(foregroundTimeoutCallback);
                }
            }
            lastSessionCheck = System.currentTimeMillis();
        }
        eventIndex += 1;

        Map<String, Object> sessionValues = state.getSessionValues();
        Map<String, Object> sessionCopy = new HashMap<>(sessionValues);
        sessionCopy.put(Parameters.SESSION_EVENT_INDEX, eventIndex);
        if (userAnonymisation) {
            sessionCopy.put(Parameters.SESSION_USER_ID, "00000000-0000-0000-0000-000000000000");
            sessionCopy.put(Parameters.SESSION_PREVIOUS_ID, null);
        }

        return new SelfDescribingJson(TrackerConstants.SESSION_SCHEMA, sessionCopy);
    }

    private boolean shouldUpdateSession() {
        if (isNewSession.get()) {
            return true;
        }
        long now = System.currentTimeMillis();
        long timeout = isBackground.get() ? backgroundTimeout : foregroundTimeout;
        return now < lastSessionCheck || now - lastSessionCheck > timeout;
    }

    private synchronized void updateSession(String eventId, long eventTimestamp) {
        isNewSession.set(false);
        String currentSessionId = Util.getUUIDString();
        String eventTimestampDateTime = Util.getDateTimeFromTimestamp(eventTimestamp);

        int sessionIndex = 1;
        eventIndex = 0;
        String previousSessionId = null;
        String storage = "LOCAL_STORAGE";
        if (state != null) {
            sessionIndex = state.getSessionIndex() + 1;
            previousSessionId = state.getSessionId();
            storage = state.getStorage();
        }
        state = new SessionState(eventId, eventTimestampDateTime, currentSessionId, previousSessionId, sessionIndex, userId, storage);
        storeSessionState(state);
        callOnSessionUpdateCallback(state);
    }

    private void storeSessionState(SessionState state) {
        JSONObject jsonObject = new JSONObject(state.getSessionValues());
        String jsonString = jsonObject.toString();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(TrackerConstants.SESSION_STATE, jsonString);
        editor.apply();
    }

    private void callOnSessionUpdateCallback(SessionState state) {
        if (onSessionUpdate != null) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    onSessionUpdate.accept(state);
                }
            });
            thread.setDaemon(true);
            thread.start();
        }
    }

    private void executeEventCallback(Runnable callback) {
        if (callback == null) return;
        try {
            callback.run();
        } catch (Exception e) {
            Logger.e(TAG, "Session event callback failed");
        }
    }

    public void startNewSession() {
        isNewSession.set(true);
    }

    /**
     * Updates the session timeout and the indexes.
     * Note: Internal use only.
     * @param isBackground whether or not the application moved to background.
     */
    public void setBackground(boolean isBackground) {
        if (!this.isBackground.compareAndSet(!isBackground, isBackground)) {
            return;
        }

        if (!isBackground) {
            Logger.d(TAG, "Application moved to foreground");
            this.executeEventCallback(this.foregroundTransitionCallback);
            try {
                setIsSuspended(false);
            } catch (Exception e) {
                Logger.e(TAG, "Could not resume checking as tracker not setup. Exception: %s", e);
            }
            foregroundIndex++;
        } else {
            Logger.d(TAG, "Application moved to background");
            this.executeEventCallback(this.backgroundTransitionCallback);
            backgroundIndex++;
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

    public int getBackgroundIndex() {
        return backgroundIndex;
    }

    public int getForegroundIndex() {
        return foregroundIndex;
    }

    /**
     * Gets the session information from a file.
     *
     * @return a map or null.
     */
    @Nullable
    private Map<String, Object> getSessionMapFromLegacyTrackerV1(@NonNull Context context) {
        Map<String, Object> sessionMap = FileStore.getMapFromFile(
                TrackerConstants.SNOWPLOW_SESSION_VARS,
                context);
        sessionMap.put(Parameters.SESSION_FIRST_ID, "");
        sessionMap.put(Parameters.SESSION_PREVIOUS_ID, null);
        sessionMap.put(Parameters.SESSION_STORAGE, "LOCAL_STORAGE");
        return sessionMap;
    }

    @Nullable
    private Map<String, Object> getSessionMapFromLegacyTrackerV2(@NonNull Context context, @NonNull String sessionVarsName) {
        StrictMode.ThreadPolicy oldPolicy = StrictMode.allowThreadDiskReads();
        try {
            SharedPreferences sharedPreferences = context.getSharedPreferences(sessionVarsName, Context.MODE_PRIVATE);
            if (!sharedPreferences.contains(Parameters.SESSION_ID)) {
                sharedPreferences = context.getSharedPreferences(TrackerConstants.SNOWPLOW_SESSION_VARS, Context.MODE_PRIVATE);
                if (!sharedPreferences.contains(Parameters.SESSION_ID)) {
                    return null;
                }
            }
            // Create map used as initial session state
            Map<String, Object> sessionMap = new HashMap<>();
            String sessionId = sharedPreferences.getString(Parameters.SESSION_ID, null);
            if (sessionId == null) return null;
            sessionMap.put(Parameters.SESSION_ID, sessionId);

            String userId = sharedPreferences.getString(Parameters.SESSION_USER_ID, null);
            if (userId == null) return null;
            sessionMap.put(Parameters.SESSION_USER_ID, userId);

            int sessionIndex = sharedPreferences.getInt(Parameters.SESSION_INDEX, 0);
            sessionMap.put(Parameters.SESSION_INDEX, sessionIndex);

            sessionMap.put(Parameters.SESSION_FIRST_ID, "");
            sessionMap.put(Parameters.SESSION_PREVIOUS_ID, null);
            sessionMap.put(Parameters.SESSION_STORAGE, "LOCAL_STORAGE");
            return sessionMap;
        } finally {
            StrictMode.setThreadPolicy(oldPolicy);
        }
    }

    @Nullable
    private Map<String, Object> getSessionMapFromLegacyTrackerV3(@NonNull Context context, @NonNull String sessionVarsName) {
        StrictMode.ThreadPolicy oldPolicy = StrictMode.allowThreadDiskReads();
        try {
            SharedPreferences sharedPreferences = context.getSharedPreferences(sessionVarsName, Context.MODE_PRIVATE);
            if (!sharedPreferences.contains(TrackerConstants.SESSION_STATE)) {
                return null;
            }
            Map<String, Object> sessionMap = new HashMap<>();
            String jsonString = sharedPreferences.getString(TrackerConstants.SESSION_STATE, null);
            JSONObject jsonObject = new JSONObject(jsonString);
            Iterator<String> iterator = jsonObject.keys();
            while (iterator.hasNext()) {
                String key = iterator.next();
                Object value = jsonObject.get(key);
                sessionMap.put(key, value);
            }
            return sessionMap;
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            StrictMode.setThreadPolicy(oldPolicy);
        }
        return null;
    }

    /**
     * @return the session index
     */
    public int getSessionIndex() { //$ to remove
        return this.state.getSessionIndex();
    }

    /**
     * @return the user id
     */
    @NonNull
    public String getUserId() {
        return this.userId;
    }

    /**
     * @return the session state
     */
    @Nullable
    public SessionState getState() {
        return state;
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
