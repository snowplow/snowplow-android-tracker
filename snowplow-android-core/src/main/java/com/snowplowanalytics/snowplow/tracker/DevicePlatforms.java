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

package com.snowplowanalytics.snowplow.tracker;

/**
 * The different platforms available to the Tracker.
 */
public enum DevicePlatforms {
    Web {
        public String toString() {
            return "web";
        }
    },
    Mobile {
        public String toString() {
            return "mob";
        }
    },
    Desktop {
        public String toString() {
            return "pc";
        }
    },
    ServerSideApp {
        public String toString() {
            return "srv";
        }
    },
    General {
        public String toString() {
            return "app";
        }
    },
    ConnectedTV {
        public String toString() {
            return "tv";
        }
    },
    GameConsole {
        public String toString() {
            return "cnsl";
        }
    },
    InternetOfThings {
        public String toString() {
            return "iot";
        }
    }
}
