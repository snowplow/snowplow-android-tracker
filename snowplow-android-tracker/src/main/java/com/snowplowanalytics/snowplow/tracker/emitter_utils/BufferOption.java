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

package com.snowplowanalytics.snowplow.tracker.emitter_utils;

/**
 * BufferOption is used to set the buffer size of your Emitter.
 */
public enum BufferOption {
    /**
     * Sends events immediately when being tracked. This may cause a lot of network traffic
     * depending on it's usage.
     */
    Instant(1),

    /**
     * Sends events in a group only after collecting 10 events. In a POST request, this is
     * sent in one payload. For a GET request, individual requests are sent following each other.
     */
    Default(10);

    private int code;

    private BufferOption(int c) {
        code = c;
    }

    public int getCode() {
        return code;
    }
}
