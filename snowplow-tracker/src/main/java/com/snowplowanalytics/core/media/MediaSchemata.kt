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

package com.snowplowanalytics.core.media

object MediaSchemata {
    private val schemaPrefix = "iglu:com.snowplowanalytics.snowplow.media/"
    private val schemaSuffix = "/jsonschema/1-0-0"

    val playerSchema = "${schemaPrefix}player$schemaSuffix"
    val sessionSchema = "${schemaPrefix}session$schemaSuffix"
    val adSchema = "${schemaPrefix}ad$schemaSuffix"
    val adBreakSchema = "${schemaPrefix}ad_break$schemaSuffix"

    fun eventSchema(eventName: String) = "${schemaPrefix}${eventName}_event$schemaSuffix"
}
