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

import androidx.annotation.NonNull;

import java.util.EnumSet;

/**
 * The different platforms available to the Tracker.
 */
public enum DevicePlatform {

    Web("web"),
    Mobile("mob"),
    Desktop("pc"),
    ServerSideApp("srv"),
    General("app"),
    ConnectedTV("tv"),
    GameConsole("cnsl"),
    InternetOfThings("iot");

    private final String value;

    DevicePlatform(final String value) {
        this.value = value;
    }

    @NonNull
    public String getValue() {
        return value;
    }

    @NonNull
    public static DevicePlatform getByValue(@NonNull String value){
        for (final DevicePlatform element : EnumSet.allOf(DevicePlatform.class)) {
            if (element.toString().equals(value)) {
                return element;
            }
        }
        return Mobile;
    }
}
