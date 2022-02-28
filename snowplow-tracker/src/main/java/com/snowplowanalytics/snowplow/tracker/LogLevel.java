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
package com.snowplowanalytics.snowplow.tracker;

/**
 * LogLevel contains the different levels of configurable logging in the Tracker.
 */
public enum LogLevel {

    /**
     * Nothing from the Tracker is logged.
     */
    OFF(0),

    /**
     * All errors are logged for the Tracker.
     */
    ERROR(1),

    /**
     * All debugging errors are logged for the Tracker.
     */
    DEBUG(2),

    /**
     * Everything that can be logged is logged.
     */
    VERBOSE(3);

    private int level;

    LogLevel(int c) {
        level = c;
    }

    public int getLevel() {
        return level;
    }
}
