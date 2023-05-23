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
package com.snowplowanalytics.core.session

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.StrictMode
import androidx.core.util.Consumer
import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.core.tracker.Logger
import com.snowplowanalytics.core.utils.Util
import com.snowplowanalytics.snowplow.entity.ClientSessionEntity
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow.tracker.SessionState
import com.snowplowanalytics.snowplow.tracker.SessionState.Companion.build
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

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
class Session @SuppressLint("ApplySharedPref") constructor(
    foregroundTimeout: Long,
    backgroundTimeout: Long,
    timeUnit: TimeUnit,
    namespace: String?,
    context: Context
) {
    // Session Variables
    var userId: String
        private set
    private var eventIndex = 0

    @Volatile
    var backgroundIndex = 0
        private set
    @Volatile
    var foregroundIndex = 0
        private set
    var state: SessionState? = null
        private set

    // Variables to control Session Updates
    private val _isBackground = AtomicBoolean(false)
    val isBackground: Boolean
        get() = _isBackground.get()

    private var lastSessionCheck: Long = 0
    private val isNewSession = AtomicBoolean(true)

    @Volatile
    private var isSessionCheckerEnabled: Boolean
    
    var foregroundTimeout: Long
    var backgroundTimeout: Long

    // Callbacks
    private var foregroundTransitionCallback: Runnable? = null
    private var backgroundTransitionCallback: Runnable? = null
    private var foregroundTimeoutCallback: Runnable? = null
    private var backgroundTimeoutCallback: Runnable? = null
    var onSessionUpdate: Consumer<SessionState>? = null

    // Session values persistence
    private var sharedPreferences: SharedPreferences

    init {
        this.foregroundTimeout = timeUnit.toMillis(foregroundTimeout)
        this.backgroundTimeout = timeUnit.toMillis(backgroundTimeout)
        isSessionCheckerEnabled = true
        
        var sessionVarsName = TrackerConstants.SNOWPLOW_SESSION_VARS
        if (namespace != null && namespace.isNotEmpty()) {
            val sessionVarsSuffix = namespace.replace("[^a-zA-Z0-9_]+".toRegex(), "-")
            sessionVarsName = TrackerConstants.SNOWPLOW_SESSION_VARS + "_" + sessionVarsSuffix
        }
        
        val oldPolicy = StrictMode.allowThreadDiskReads()
        try {
            val sessionInfo = getSessionMap(context, sessionVarsName)
            if (sessionInfo == null) {
                Logger.track(TAG, "No previous session info available")
            } else {
                state = build(sessionInfo)
            }
            userId = retrieveUserId(context, state)
            sharedPreferences = context.getSharedPreferences(sessionVarsName, Context.MODE_PRIVATE)
            lastSessionCheck = System.currentTimeMillis()
        } finally {
            StrictMode.setThreadPolicy(oldPolicy)
        }
        Logger.v(TAG, "Tracker Session Object created.")
    }

    /**
     * Returns the session context
     *
     * @return a SelfDescribingJson containing the session context
     */
    @Synchronized
    fun getSessionContext(
        eventId: String,
        eventTimestamp: Long,
        userAnonymisation: Boolean
    ): SelfDescribingJson? {
        Logger.v(TAG, "Getting session context...")
        if (isSessionCheckerEnabled) {
            if (shouldUpdateSession()) {
                Logger.d(TAG, "Update session information.")
                updateSession(eventId, eventTimestamp)
                if (isBackground) { // timed out in background
                    executeEventCallback(backgroundTimeoutCallback)
                } else { // timed out in foreground
                    executeEventCallback(foregroundTimeoutCallback)
                }
            }
            lastSessionCheck = System.currentTimeMillis()
        }
        eventIndex += 1
        
        val state = state ?: run {
            Logger.v(TAG, "Session state not present")
            return null 
        }
        
        val sessionValues = state.sessionValues
        val sessionCopy: MutableMap<String, Any?> = HashMap(sessionValues)
        sessionCopy[Parameters.SESSION_EVENT_INDEX] = eventIndex
        if (userAnonymisation) {
            sessionCopy[Parameters.SESSION_USER_ID] =
                "00000000-0000-0000-0000-000000000000"
            sessionCopy[Parameters.SESSION_PREVIOUS_ID] = null
        }
        return ClientSessionEntity(sessionCopy)
    }

    private fun shouldUpdateSession(): Boolean {
        if (isNewSession.get()) {
            return true
        }
        val now = System.currentTimeMillis()
        val timeout = if (isBackground) backgroundTimeout else foregroundTimeout
        return now < lastSessionCheck || now - lastSessionCheck > timeout
    }

    @Synchronized
    private fun updateSession(eventId: String, eventTimestamp: Long) {
        isNewSession.set(false)
        val currentSessionId = Util.uUIDString()
        val eventTimestampDateTime = Util.getDateTimeFromTimestamp(eventTimestamp)
        
        var sessionIndex = 1
        eventIndex = 0
        var previousSessionId: String? = null
        var storage = "LOCAL_STORAGE"
        state?.let {
            sessionIndex = it.sessionIndex + 1
            previousSessionId = it.sessionId
            storage = it.storage
        }
        state = SessionState(
            eventId,
            eventTimestampDateTime,
            currentSessionId,
            previousSessionId,
            sessionIndex,
            userId,
            storage
        )
        state?.let {
            storeSessionState(it)
            callOnSessionUpdateCallback(it)
        }
    }

    private fun storeSessionState(state: SessionState) {
        val jsonObject = JSONObject(state.sessionValues)
        val jsonString = jsonObject.toString()
        val editor = sharedPreferences.edit()
        editor.putString(TrackerConstants.SESSION_STATE, jsonString)
        editor.apply()
    }

    private fun callOnSessionUpdateCallback(state: SessionState) {
        val onSessionUpdate = onSessionUpdate ?: return
        
        val thread = Thread { onSessionUpdate.accept(state) }
        thread.isDaemon = true
        thread.start()
    }

    private fun executeEventCallback(callback: Runnable?) {
        if (callback == null) return
        try {
            callback.run()
        } catch (e: Exception) {
            Logger.e(TAG, "Session event callback failed")
        }
    }

    fun startNewSession() {
        isNewSession.set(true)
    }

    /**
     * Updates the session timeout and the indexes.
     * Note: Internal use only.
     * @suppress
     * @param isBackground whether or not the application moved to background.
     */
    fun setBackground(isBackground: Boolean) {
        if (!this._isBackground.compareAndSet(!isBackground, isBackground)) {
            return
        }
        if (!isBackground) {
            Logger.d(TAG, "Application moved to foreground")
            executeEventCallback(foregroundTransitionCallback)
            try {
                setIsSuspended(false)
            } catch (e: Exception) {
                Logger.e(TAG, "Could not resume checking as tracker not setup. Exception: %s", e)
            }
            foregroundIndex++
        } else {
            Logger.d(TAG, "Application moved to background")
            executeEventCallback(backgroundTransitionCallback)
            backgroundIndex++
        }
    }

    /**
     * Changes the truth of isSuspended
     * @param isSuspended whether the session tracking is suspended,
     * i.e. calls to update session are ignored,
     * but access time is changed to current time.
     */
    fun setIsSuspended(isSuspended: Boolean) {
        Logger.d(TAG, "Session is suspended: %s", isSuspended)
        isSessionCheckerEnabled = !isSuspended
    }

    private fun getSessionMap(
        context: Context,
        sessionVarsName: String
    ): Map<String?, Any?>? {
        val oldPolicy = StrictMode.allowThreadDiskReads()
        try {
            val sharedPreferences =
                context.getSharedPreferences(sessionVarsName, Context.MODE_PRIVATE)
            if (!sharedPreferences.contains(TrackerConstants.SESSION_STATE)) {
                return null
            }
            val sessionMap: MutableMap<String?, Any?> = HashMap()
            val jsonString = sharedPreferences.getString(TrackerConstants.SESSION_STATE, null)
            val jsonObject = jsonString?.let { JSONObject(it) }
            val iterator = jsonObject?.keys()
            while (iterator?.hasNext() == true) {
                val key = iterator.next()
                val value = jsonObject[key]
                sessionMap[key] = value
            }
            return sessionMap
        } catch (e: JSONException) {
            e.printStackTrace()
        } finally {
            StrictMode.setThreadPolicy(oldPolicy)
        }
        return null
    }
    
    /**
     * @return the session index
     */
    val sessionIndex: Int?
        get() = state?.sessionIndex //$ to remove

    companion object {
        private val TAG = Session::class.java.simpleName

        /**
         * Creates a new Session object which will
         * update itself overtime.
         *
         * @param context the android context
         * @param foregroundTimeout the amount of time that can elapse before the
         * session id is updated while the app is in the
         * foreground.
         * @param backgroundTimeout the amount of time that can elapse before the
         * session id is updated while the app is in the
         * background.
         * @param timeUnit the time units of the timeout measurements
         * @param namespace the namespace used by the session.
         * @param sessionCallbacks Called when the app change state or when timeout is triggered.
         */
        @JvmStatic
        @Synchronized
        fun getInstance(
            context: Context,
            foregroundTimeout: Long,
            backgroundTimeout: Long,
            timeUnit: TimeUnit,
            namespace: String?,
            sessionCallbacks: Array<Runnable?>?
        ): Session {
            val session =
                Session(foregroundTimeout, backgroundTimeout, timeUnit, namespace, context)
            var callbacks: Array<Runnable?>? = arrayOf(null, null, null, null)
            if (sessionCallbacks != null && sessionCallbacks.size == 4) {
                callbacks = sessionCallbacks
            }
            session.foregroundTransitionCallback = callbacks?.get(0)
            session.backgroundTransitionCallback = callbacks?.get(1)
            session.foregroundTimeoutCallback = callbacks?.get(2)
            session.backgroundTimeoutCallback = callbacks?.get(3)
            return session
        }

        @Synchronized
        private fun retrieveUserId(context: Context, state: SessionState?): String {
            var userId: String = state?.userId ?: Util.uUIDString()
            
            // Session_UserID is available only if the session context is enabled.
            // In a future version we would like to make it available even if the session context is disabled.
            // For this reason, we store the Session_UserID in a separate storage (decoupled by session values)
            // calling it Installation_UserID in order to remark that it isn't related to the session context.
            // Although, for legacy, we need to copy its value in the Session_UserID of the session context
            // as the session context schema (and related data modelling) requires it.
            // For further details: https://discourse.snowplow.io/t/rfc-mobile-trackers-v2-0
            
            val generalPref = context.getSharedPreferences(
                TrackerConstants.SNOWPLOW_GENERAL_VARS,
                Context.MODE_PRIVATE
            )
            val storedUserId = generalPref.getString(TrackerConstants.INSTALLATION_USER_ID, null)
            if (storedUserId != null) {
                userId = storedUserId
            } else {
                generalPref.edit()
                    .putString(TrackerConstants.INSTALLATION_USER_ID, userId)
                    .commit()
            }
            return userId
        }
    }
}
