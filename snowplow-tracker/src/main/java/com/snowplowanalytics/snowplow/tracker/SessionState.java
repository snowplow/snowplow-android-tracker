package com.snowplowanalytics.snowplow.tracker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.internal.tracker.Logger;
import com.snowplowanalytics.snowplow.internal.tracker.State;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SessionState implements State {

    @NonNull
    private final String firstEventId;
    @NonNull
    private final String firstEventTimestamp;
    @NonNull
    private final String sessionId;
    @Nullable
    private final String previousSessionId;
    private final int sessionIndex;
    private AtomicInteger eventIndex;
    @NonNull
    private final String userId;
    @NonNull
    private final String storage;

    @NonNull
    private Map<String, Object> sessionContext;

    public SessionState(
            @NonNull String firstEventId,
            @NonNull String firstEventTimestamp,
            @NonNull String currentSessionId,
            @Nullable String previousSessionId, //$ On iOS it has to be set nullable on constructor
            int sessionIndex,
            int eventIndex,
            @NonNull String userId,
            @NonNull String storage
    ) {
        this.firstEventId = firstEventId;
        this.firstEventTimestamp = firstEventTimestamp;
        this.sessionId = currentSessionId;
        this.previousSessionId = previousSessionId;
        this.sessionIndex = sessionIndex;
        this.eventIndex = new AtomicInteger(eventIndex);
        this.userId = userId;
        this.storage = storage;

        sessionContext = new HashMap<String, Object>();
        sessionContext.put(Parameters.SESSION_FIRST_ID, firstEventId);
        sessionContext.put(Parameters.SESSION_FIRST_TIMESTAMP, firstEventTimestamp);
        sessionContext.put(Parameters.SESSION_ID, sessionId);
        sessionContext.put(Parameters.SESSION_PREVIOUS_ID, previousSessionId);
        sessionContext.put(Parameters.SESSION_INDEX, sessionIndex); //$ should be Number?!
        sessionContext.put(Parameters.SESSION_EVENT_INDEX, this.eventIndex.get());
        sessionContext.put(Parameters.SESSION_USER_ID, userId);
        sessionContext.put(Parameters.SESSION_STORAGE, storage);
    }

    @Nullable
    public static SessionState build(@NonNull Map<String, Object> storedState) {
        Object value = storedState.get(Parameters.SESSION_FIRST_ID);
        if (!(value instanceof String)) return null;
        String firstEventId = (String) value;

        value = storedState.get(Parameters.SESSION_FIRST_TIMESTAMP);
        if (!(value instanceof Long)) return null;
        String firstEventTimestamp = (String) value;

        value = storedState.get(Parameters.SESSION_ID);
        if (!(value instanceof String)) return null;
        String sessionId = (String) value;

        value = storedState.get(Parameters.SESSION_PREVIOUS_ID);
        if (!(value instanceof String)) {
            value = null;
        };
        String previousSessionId = (String) value;

        value = storedState.get(Parameters.SESSION_INDEX);
        if (!(value instanceof Integer)) return null;
        int sessionIndex = (Integer) value;

        value = storedState.get(Parameters.SESSION_EVENT_INDEX);
        if (!(value instanceof Integer)) return null;
        int eventIndex = (Integer) value;

        value = storedState.get(Parameters.SESSION_USER_ID);
        if (!(value instanceof String)) return null;
        String userId = (String) value;

        value = storedState.get(Parameters.SESSION_STORAGE);
        if (!(value instanceof String)) return null;
        String storage = (String) value;

        return new SessionState(firstEventId, firstEventTimestamp, sessionId, previousSessionId, sessionIndex, eventIndex, userId, storage);
    }

    public void incrementEventIndex() {
        Logger.d("SessionState ❌", "incrementEventIndex called. EventIndex is: " + eventIndex.get());
        sessionContext.put(Parameters.SESSION_EVENT_INDEX, eventIndex.incrementAndGet());
    }

    // Getters

    @NonNull
    public String getFirstEventId() {
        return firstEventId;
    }

    @NonNull
    public String getFirstEventTimestamp() {
        return firstEventTimestamp;
    }

    @Nullable
    public String getPreviousSessionId() {
        return previousSessionId;
    }

    @NonNull
    public String getSessionId() {
        return sessionId;
    }

    public int getSessionIndex() {
        return sessionIndex;
    }

    public int getEventIndex() { return eventIndex.get(); }

    @NonNull
    public String getStorage() {
        return storage;
    }

    @NonNull
    public String getUserId() {
        return userId;
    }

    @NonNull
    public Map<String, Object> getSessionValues() {
        return sessionContext;
    }
}
