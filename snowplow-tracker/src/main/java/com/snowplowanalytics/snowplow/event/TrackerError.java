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

package com.snowplowanalytics.snowplow.event;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.internal.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.internal.utils.Util;

import java.util.HashMap;
import java.util.Map;

/** An error event representing an exception, error or warning message in the app. */
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

    // Tracker methods

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

    // Private methods

    private String truncate(String s, int maxLength) {
        if (s == null) return null;
        return s.substring(0, Math.min(s.length(), maxLength));
    }
}
