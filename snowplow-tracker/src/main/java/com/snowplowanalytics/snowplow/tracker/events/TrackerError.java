package com.snowplowanalytics.snowplow.tracker.events;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.utils.Util;

import java.util.HashMap;
import java.util.Map;

public class TrackerError extends AbstractSelfDescribing {
    private static final int MAX_MESSAGE_LENGTH = 2048;
    private static final int MAX_STACK_LENGTH = 8192;
    private static final int MAX_EXCEPTION_NAME_LENGTH = 1024;

    private final String source;
    private final String message;
    private final Throwable throwable;

    public TrackerError(@NonNull String source, @NonNull String message) {
        this(source, message, null);
    }

    public TrackerError(@NonNull String source, @NonNull String message, @Nullable Throwable throwable) {
        super();
        this.source = source;
        this.message = message;
        this.throwable = throwable;
    }

    @Override
    public @NonNull Map<String, Object> getDataPayload() {
        String msg = truncate(message, MAX_MESSAGE_LENGTH);
        if (msg == null || msg.isEmpty()) {
            msg = "Empty message found";
        }

        HashMap<String, Object> payload = new HashMap<>();
        payload.put(Parameters.DIAGNOSTIC_ERROR_CLASS_NAME, source);
        payload.put(Parameters.DIAGNOSTIC_ERROR_MESSAGE, msg);

        if (throwable != null) {
            String stack = truncate(Util.stackTraceToString(throwable), MAX_STACK_LENGTH);
            String throwableName = truncate(throwable.getClass().getName(), MAX_EXCEPTION_NAME_LENGTH);
            payload.put(Parameters.DIAGNOSTIC_ERROR_STACK, stack);
            payload.put(Parameters.DIAGNOSTIC_ERROR_EXCEPTION_NAME, throwableName);
        }
        return payload;
    }

    @Override
    public @NonNull String getSchema() {
        return TrackerConstants.SCHEMA_DIAGNOSTIC_ERROR;
    }

    private String truncate(String s, int maxLength) {
        if (s == null) return null;
        return s.substring(0, Math.min(s.length(), maxLength));
    }
}
