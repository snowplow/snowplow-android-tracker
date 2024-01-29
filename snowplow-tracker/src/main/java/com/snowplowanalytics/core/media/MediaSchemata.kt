/*
 * Copyright (c) 2015-present Snowplow Analytics Ltd. All rights reserved.
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

package com.snowplowanalytics.core.media

object MediaSchemata {
    private val schemaPrefix = "iglu:com.snowplowanalytics.snowplow.media/"
    private val schemaSuffix = "/jsonschema/1-0-0"

    // NOTE: The player schema has a different vendor than the other media schemas because it builds on an older version of the same schema. Versions 5.3 to 5.4.1 of the tracker used a conflicting schema URI which has since been removed from Iglu Central.
    val playerSchema = "iglu:com.snowplowanalytics.snowplow/media_player/jsonschema/2-0-0"
    val sessionSchema = "${schemaPrefix}session$schemaSuffix"
    val adSchema = "${schemaPrefix}ad$schemaSuffix"
    val adBreakSchema = "${schemaPrefix}ad_break$schemaSuffix"

    fun eventSchema(eventName: String) = "${schemaPrefix}${eventName}_event$schemaSuffix"
}
