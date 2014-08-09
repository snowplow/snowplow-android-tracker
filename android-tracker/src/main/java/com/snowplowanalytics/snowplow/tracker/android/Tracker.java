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

import com.snowplowanalytics.snowplow.tracker.emitter.Emitter;

public class Tracker extends com.snowplowanalytics.snowplow.tracker.Tracker {

    public Tracker(Emitter emitter, String namespace, String appId) {
        super(emitter, namespace, appId);
    }

    public Tracker(Emitter emitter, Subject subject, String namespace, String appId) {
        super(emitter, subject, namespace, appId);
    }

    public Tracker(Emitter emitter, String namespace, String appId, boolean base64Encoded) {
        super(emitter, namespace, appId, base64Encoded);
    }

    public Tracker(Emitter emitter, Subject subject, String namespace, String appId,
                   boolean base64Encoded) {
        super(emitter, subject, namespace, appId, base64Encoded);
    }
}
