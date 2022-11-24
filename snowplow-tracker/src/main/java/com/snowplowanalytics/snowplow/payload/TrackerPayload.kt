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

import com.snowplowanalytics.core.tracker.Logger
import com.snowplowanalytics.core.utils.Util
import org.json.JSONObject

/**
 * Returns a standard Tracker Payload consisting of
 * many key - pair values.
 */
class TrackerPayload : Payload {
    private val TAG = TrackerPayload::class.java.simpleName
    
    override val map = HashMap<String, Any>()
    
    override fun add(key: String, value: String?) {
        if (value == null || value.isEmpty()) {
            Logger.v(TAG, "The keys value is empty, removing the key: %s", key)
            map.remove(key)
            return
        }
        Logger.v(TAG, "Adding new kv pair: $key->%s", value)
        map[key] = value
    }

    override fun add(key: String, value: Any?) {
        if (value == null) {
            Logger.v(TAG, "The value is empty, removing the key: %s", key)
            map.remove(key)
            return
        }
        Logger.v(TAG, "Adding new kv pair: $key->%s", value)
        map[key] = value
    }

    override fun addMap(map: Map<String, Any>?) {
        Logger.v(TAG, "Adding new map: %s", map)
        if (map != null) {
            for ((key, value) in map) {
                add(key, value)
            }
        }
    }

    override fun addMap(
        map: Map<*, *>?,
        base64_encoded: Boolean,
        type_encoded: String?,
        type_no_encoded: String?
    ) {
        if (map != null) {
            val mapString = JSONObject(map).toString()
            Logger.v(TAG, "Adding new map: %s", map)

            if (base64_encoded) { // base64 encoded data
                add(type_encoded!!, Util.base64Encode(mapString))
            } else { // add it as a child node
                add(type_no_encoded!!, mapString)
            }
        }
    }

    override fun toString(): String {
        return JSONObject(map as Map<*, *>).toString()
    }

    override val byteSize: Long
        get() = Util.getUTF8Length(toString())
}
