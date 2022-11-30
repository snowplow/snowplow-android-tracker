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
package com.snowplowanalytics.snowplow.payload

import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.utils.Preconditions
import com.snowplowanalytics.core.utils.Util
import org.json.JSONObject

/**
 * Returns a SelfDescribingJson object which will contain
 * both the Schema and Data.
 */
open class SelfDescribingJson {
    private val payload = HashMap<String?, Any?>()
    
    /**
     * Builds a SelfDescribingJson object
     *
     * @param schema the schema string
     * @param data to nest into the object
     *          as a TrackerPayload
     */
    constructor(schema: String, data: TrackerPayload) {
        setSchema(schema)
        setData(data)
    }

    /**
     * Builds a SelfDescribingJson object
     *
     * @param schema the schema string
     * @param data to nest into the object
     *          as a SelfDescribingJson
     */
    constructor(schema: String, data: SelfDescribingJson) {
        setSchema(schema)
        setData(data)
    }

    /**
     * Builds a SelfDescribingJson object
     *
     * @param schema the schema string
     * @param data to nest into the object
     *          as a POJO. Default is an empty HashMap
     */
    constructor(schema: String, data: Any = HashMap<Any, Any>()) {
        setSchema(schema)
        setData(data)
    }

    /**
     * Builds a SelfDescribingJson object
     *
     * @param schema the schema string
     */
    constructor(schema: String) {
        setSchema(schema)
        setData(HashMap<Any, Any>())
    }

    /**
     * Sets the Schema for the SelfDescribingJson
     *
     * @param schema a valid schema string
     * @return itself if it passes precondition checks
     */
    fun setSchema(schema: String): SelfDescribingJson {
        Preconditions.checkArgument(schema.isNotEmpty(), "schema cannot be empty.")
        
        payload[Parameters.SCHEMA] = schema
        return this
    }

    /**
     * Adds data to the SelfDescribingJson
     * - Accepts a TrackerPayload object
     *
     * @param trackerPayload the data to be added to the SelfDescribingJson
     * @return itself
     */
    fun setData(trackerPayload: TrackerPayload?): SelfDescribingJson {
        trackerPayload?.let { payload[Parameters.DATA] = it.map }
        return this
    }

    /**
     * Adds data to the SelfDescribingJson
     * - Accepts a POJO
     *
     * @param data the data to be added to the SelfDescribingJson
     * @return itself
     */
    fun setData(data: Any?): SelfDescribingJson {
        data?.let { payload[Parameters.DATA] = it }
        return this
    }

    /**
     * Allows us to add data from one SelfDescribingJson into another
     * without copying over the Schema.
     *
     * @param selfDescribingJson the payload to add to the SelfDescribingJson
     * @return itself
     */
    fun setData(selfDescribingJson: SelfDescribingJson?): SelfDescribingJson {
        selfDescribingJson?.let { payload[Parameters.DATA] = it.map }
        return this
    }

    val map: Map<String?, Any?>
        get() = payload

    override fun toString(): String {
        return JSONObject(payload).toString()
    }

    val byteSize: Long
        get() = Util.getUTF8Length(toString())
}
