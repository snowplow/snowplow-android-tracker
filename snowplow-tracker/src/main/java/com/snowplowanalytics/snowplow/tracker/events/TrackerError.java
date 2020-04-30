package com.snowplowanalytics.snowplow.tracker.events;

import android.support.annotation.NonNull;

import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.payload.Payload;
import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.payload.TrackerPayload;
import com.snowplowanalytics.snowplow.tracker.utils.Preconditions;
import com.snowplowanalytics.snowplow.tracker.utils.Util;

import java.util.Map;

public class TrackerError extends AbstractSelfDescribing {
    private static final int MAX_MESSAGE_LENGTH = 2048;
    private static final int MAX_STACK_LENGTH = 8192;
    private static final int MAX_EXCEPTION_NAME_LENGTH = 1024;

    private final String source;
    private final String message;
    private final Throwable throwable;

    public static abstract class Builder<T extends Builder<T>> extends AbstractEvent.Builder<T> {
        private String source;
        private String message;
        private Throwable throwable;

        public T source(String source) {
            this.source = source;
            return self();
        }

        public T message(String message) {
            this.message = message;
            return self();
        }

        public T throwable(Throwable throwable) {
            this.throwable = throwable;
            return self();
        }

        public TrackerError build() {
            return new TrackerError(this);
        }
    }

    private static class Builder2 extends Builder<Builder2> {
        @Override
        protected Builder2 self() {
            return this;
        }
    }

    public static Builder<?> builder() {
        return new Builder2();
    }

    private TrackerError(Builder<?> builder) {
        super(builder);

        // Precondition checks
        Preconditions.checkNotNull(builder.source);
        Preconditions.checkNotNull(builder.message);

        this.source = builder.source;
        this.message = builder.message;
        this.throwable = builder.throwable;
    }

    @Override
    public @NonNull Map<String, Object> getDataPayload() {
        return getData().getMap();
    }

    @Override
    public @NonNull String getSchema() {
        return TrackerConstants.SCHEMA_DIAGNOSTIC_ERROR;
    }

    private @NonNull TrackerPayload getData() {
        String msg = truncate(message, MAX_MESSAGE_LENGTH);
        if (msg == null || msg.isEmpty()) {
            msg = "Empty message found";
        }

        TrackerPayload payload = new TrackerPayload();
        payload.add(Parameters.DIAGNOSTIC_ERROR_MESSAGE, msg);
        payload.add(Parameters.DIAGNOSTIC_ERROR_CLASS_NAME, source);

        if (throwable != null) {
            String stack = truncate(Util.stackTraceToString(throwable), MAX_STACK_LENGTH);
            String throwableName = truncate(throwable.getClass().getName(), MAX_EXCEPTION_NAME_LENGTH);
            payload.add(Parameters.DIAGNOSTIC_ERROR_STACK, stack);
            payload.add(Parameters.DIAGNOSTIC_ERROR_EXCEPTION_NAME, throwableName);
        }
        return payload;
    }

    private String truncate(String s, int maxLength) {
        if (s == null) return null;
        return s.substring(0, Math.min(s.length(), maxLength));
    }
}
