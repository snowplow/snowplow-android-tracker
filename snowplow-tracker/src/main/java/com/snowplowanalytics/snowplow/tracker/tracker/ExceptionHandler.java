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

package com.snowplowanalytics.snowplow.tracker.tracker;

import androidx.annotation.NonNull;

import com.snowplowanalytics.snowplow.tracker.Tracker;
import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.events.SelfDescribing;
import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.utils.Logger;
import com.snowplowanalytics.snowplow.tracker.utils.Util;

import java.util.HashMap;
import java.util.Map;

public class ExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = ExceptionHandler.class.getSimpleName();

    private static final int MAX_MESSAGE_LENGTH = 2048;
    private static final int MAX_STACK_LENGTH = 8096;
    private static final int MAX_THREAD_NAME_LENGTH = 1024;
    private static final int MAX_CLASS_NAME_LENGTH = 1024;
    private static final int MAX_EXCEPTION_NAME_LENGTH = 1024;

    private final Thread.UncaughtExceptionHandler defaultHandler;

    /**
     * Creates a new SnowplowExceptionHandler which
     * keeps a pointer to the previous handler to
     * re-throw.
     */
    public ExceptionHandler() {
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    /**
     * Sends a Snowplow Event and then re-throws.
     *
     * @param t The thread that crashed
     * @param e The throwable
     */
    public void uncaughtException(@NonNull Thread t, Throwable e) {
        Logger.d(TAG, "Uncaught exception being tracked...");

        // Ensure message is not-null/empty
        String message = truncateString(e.getMessage(), MAX_MESSAGE_LENGTH);
        if (message == null || message.isEmpty()) {
            message = "Android Exception. Null or empty message found";
        }

        String stack = truncateString(Util.stackTraceToString(e), MAX_STACK_LENGTH);
        String threadName = truncateString(t.getName(), MAX_THREAD_NAME_LENGTH);

        Integer lineNumber = null;
        String className = null;
        if (e.getStackTrace().length > 0) {
            StackTraceElement stackElement = e.getStackTrace()[0];

            // Ensure lineNumber is greater than or equal to zero
            lineNumber = stackElement.getLineNumber();
            if (lineNumber < 0) {
                lineNumber = null;
            }

            className = truncateString(stackElement.getClassName(), MAX_CLASS_NAME_LENGTH);
        }

        String exceptionName = truncateString(e.getClass().getName(), MAX_EXCEPTION_NAME_LENGTH);

        Map<String, Object> data = new HashMap<>();
        Util.addToMap(Parameters.APP_ERROR_MESSAGE, message, data);
        Util.addToMap(Parameters.APP_ERROR_STACK, stack, data);
        Util.addToMap(Parameters.APP_ERROR_THREAD_NAME, threadName, data);
        Util.addToMap(Parameters.APP_ERROR_THREAD_ID, t.getId(), data);
        Util.addToMap(Parameters.APP_ERROR_LANG, "JAVA", data);
        Util.addToMap(Parameters.APP_ERROR_LINE, lineNumber, data);
        Util.addToMap(Parameters.APP_ERROR_CLASS_NAME, className, data);
        Util.addToMap(Parameters.APP_ERROR_EXCEPTION_NAME, exceptionName, data);
        Util.addToMap(Parameters.APP_ERROR_FATAL, true, data);

        Tracker.instance().track(SelfDescribing.builder()
                .eventData(new SelfDescribingJson(TrackerConstants.APPLICATION_ERROR_SCHEMA, data))
                .build()
        );

        defaultHandler.uncaughtException(t, e);
    }

    /**
     * Truncates a string at a maximum length
     *
     * @param str The string to truncate
     * @param maxLength The maximum length
     * @return the truncated string
     */
    private String truncateString(String str, int maxLength) {
        if (str == null) {
            return null;
        } else {
            return str.substring(0, Math.min(str.length(), maxLength));
        }
    }
}
