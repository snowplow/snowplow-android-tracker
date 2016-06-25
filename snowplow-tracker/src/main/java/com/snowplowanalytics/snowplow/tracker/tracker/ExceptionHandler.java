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

package com.snowplowanalytics.snowplow.tracker.tracker;

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
    public void uncaughtException(Thread t, Throwable e) {
        Logger.d(TAG, "Uncaught exception being tracked...");

        Map<String, Object> data = new HashMap<>();
        Util.addToMap(Parameters.APP_ERROR_MESSAGE, e.getMessage(), data);
        Util.addToMap(Parameters.APP_ERROR_STACK, Util.stackTraceToString(e), data);
        Util.addToMap(Parameters.APP_ERROR_THREAD_NAME, t.getName(), data);
        Util.addToMap(Parameters.APP_ERROR_THREAD_ID, t.getId(), data);
        Util.addToMap(Parameters.APP_ERROR_LANG, "JAVA", data);

        Tracker.instance().track(SelfDescribing.builder()
                .eventData(new SelfDescribingJson(TrackerConstants.APPLICATION_ERROR_SCHEMA, data))
                .build()
        );

        defaultHandler.uncaughtException(t, e);
    }
}
