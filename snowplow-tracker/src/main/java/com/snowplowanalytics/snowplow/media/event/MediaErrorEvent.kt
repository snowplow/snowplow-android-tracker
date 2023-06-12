/*
 * Copyright (c) 2015-2023 Snowplow Analytics Ltd. All rights reserved.
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

package com.snowplowanalytics.snowplow.media.event

import com.snowplowanalytics.core.media.MediaSchemata
import com.snowplowanalytics.snowplow.event.AbstractSelfDescribing

/**
 * Media player event tracked when the resource could not be loaded due to an error.
 *
 * @param errorCode Error-identifying code for the playback issue. E.g. E522.
 * @param errorName Name for the type of error that occurred in the playback. E.g. forbidden.
 * @param errorDescription Longer description for the error that occurred in the playback.
 */
class MediaErrorEvent @JvmOverloads constructor(
    var errorCode: String? = null,
    var errorName: String? = null,
    var errorDescription: String? = null
) : AbstractSelfDescribing() {
    override val schema: String
        get() = MediaSchemata.eventSchema("error")

    override val dataPayload: Map<String, Any?>
        get() = mapOf(
            "errorCode" to errorCode,
            "errorName" to errorName,
            "errorDescription" to errorDescription,
        ).filterValues { it != null }
}
