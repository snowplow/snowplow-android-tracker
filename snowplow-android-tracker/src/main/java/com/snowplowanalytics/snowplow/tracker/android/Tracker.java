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

import android.util.Log;

import com.snowplowanalytics.snowplow.tracker.core.emitter.Emitter;
import com.snowplowanalytics.snowplow.tracker.core.payload.Payload;
import com.snowplowanalytics.snowplow.tracker.core.payload.SchemaPayload;

import java.util.LinkedList;
import java.util.List;

public class Tracker extends com.snowplowanalytics.snowplow.tracker.core.Tracker {

    private Subject subject;
    private String TAG = Tracker.class.getName();

    /**
     * @param emitter Emitter to which events will be sent
     * @param namespace Identifier for the Tracker instance
     * @param appId Application ID
     */
    public Tracker(Emitter emitter, String namespace, String appId) {
        this(emitter, null, namespace, appId, true);
    }

    /**
     * @param emitter Emitter to which events will be sent
     * @param subject Subject to be tracked
     * @param namespace Identifier for the Tracker instance
     * @param appId Application ID
     */
    public Tracker(Emitter emitter, Subject subject, String namespace, String appId) {
        this(emitter, subject, namespace, appId, true);
    }

    /**
     * @param emitter Emitter to which events will be sent
     * @param namespace Identifier for the Tracker instance
     * @param appId Application ID
     * @param base64Encoded Whether JSONs in the payload should be base-64 encoded
     */
    public Tracker(Emitter emitter, String namespace, String appId, boolean base64Encoded) {
        this(emitter, null, namespace, appId, base64Encoded);
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
        this.subject = subject;
    }

    @Override
    protected Payload completePayload(Payload payload, List<SchemaPayload> context,
                                   double timestamp) {


        if (this.subject != null) {

            if (context == null) {
                Log.d(TAG, "No list of user context passed in");
                context = new LinkedList<SchemaPayload>();
            }

            if (!this.subject.getSubjectLocation().isEmpty()) {
                SchemaPayload locationPayload = new SchemaPayload();
                locationPayload.setSchema(Constants.GEOLOCATION_SCHEMA);
                locationPayload.setData(this.subject.getSubjectLocation());
                context.add(locationPayload);
            }

            if (!this.subject.getSubjectMobile().isEmpty()) {
                SchemaPayload mobilePayload = new SchemaPayload();
                mobilePayload.setSchema(Constants.MOBILE_SCHEMA);
                mobilePayload.setData(this.subject.getSubjectMobile());
                context.add(mobilePayload);
            }

        }

        return super.completePayload(payload, context, timestamp);
    }
}
