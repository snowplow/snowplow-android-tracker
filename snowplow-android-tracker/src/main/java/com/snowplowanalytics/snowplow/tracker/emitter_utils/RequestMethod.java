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
 * RequestMethod is used to choose how network requests should be sent.
 * This can be used by <code>setRequestMethod(RequestMethod option)</code> to set accordingly.
 */
public enum RequestMethod {
    /**
     * Requests are sent synchronously, so it should be used with caution.
     */
    Synchronous,
    /**
     * Requests are sent asynchronously using a background thread.
     */
    Asynchronous
}
