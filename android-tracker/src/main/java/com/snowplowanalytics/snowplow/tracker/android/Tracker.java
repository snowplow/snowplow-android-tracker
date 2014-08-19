/*
 * Copyright (c) 2014 Snowplow Analytics Ltd. All rights reserved.
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

package com.snowplowanalytics.snowplow.tracker.android;

import com.snowplowanalytics.snowplow.tracker.core.emitter.Emitter;

public class Tracker extends com.snowplowanalytics.snowplow.tracker.core.Tracker {

    /**
     * @param emitter Emitter to which events will be sent
     * @param namespace Identifier for the Tracker instance
     * @param appId Application ID
     */
    public Tracker(Emitter emitter, String namespace, String appId) {
        super(emitter, namespace, appId);
        super.setTrackerVersion(Version.TRACKER);
    }

    /**
     * @param emitter Emitter to which events will be sent
     * @param subject Subject to be tracked
     * @param namespace Identifier for the Tracker instance
     * @param appId Application ID
     */
    public Tracker(Emitter emitter, Subject subject, String namespace, String appId) {
        super(emitter, subject, namespace, appId);
        super.setTrackerVersion(Version.TRACKER);
    }

    /**
     * @param emitter Emitter to which events will be sent
     * @param namespace Identifier for the Tracker instance
     * @param appId Application ID
     * @param base64Encoded Whether JSONs in the payload should be base-64 encoded
     */
    public Tracker(Emitter emitter, String namespace, String appId, boolean base64Encoded) {
        super(emitter, namespace, appId, base64Encoded);
        super.setTrackerVersion(Version.TRACKER);
    }

    /**
     * @param emitter Emitter to which events will be sent
     * @param subject Subject to be tracked
     * @param namespace Identifier for the Tracker instance
     * @param appId Application ID
     * @param base64Encoded Whether JSONs in the payload should be base-64 encoded
     */
    public Tracker(Emitter emitter, Subject subject, String namespace, String appId,
                   boolean base64Encoded) {
        super(emitter, subject, namespace, appId, base64Encoded);
        super.setTrackerVersion(Version.TRACKER);
    }
}
