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
package com.snowplowanalytics.core.remoteconfiguration

import android.content.Context
import com.snowplowanalytics.snowplow.configuration.Configuration
import com.snowplowanalytics.snowplow.configuration.ConfigurationBundle
import org.json.JSONObject

class RemoteConfigurationBundle : Configuration {
    var schema: String
    var configurationVersion: Int
    var configurationBundle: List<ConfigurationBundle>

    constructor(schema: String) {
        this.schema = schema
        configurationVersion = -1
        configurationBundle = listOf()
    }

    // JSON formatter
    constructor(context: Context, jsonObject: JSONObject) {
        schema = jsonObject.getString("\$schema")
        configurationVersion = jsonObject.getInt("configurationVersion")
        val tempBundle = ArrayList<ConfigurationBundle>()
        
        val array = jsonObject.getJSONArray("configurationBundle")
        for (i in 0 until array.length()) {
            val bundleJson = array.getJSONObject(i)
            val bundle = ConfigurationBundle(context, bundleJson)
            tempBundle.add(bundle)
        }
        configurationBundle = tempBundle.toList()
    }

    // Copyable
    override fun copy(): Configuration {
        val copy = RemoteConfigurationBundle(schema)
        copy.configurationVersion = configurationVersion
        
        val tempBundle = ArrayList<ConfigurationBundle>()
        for (bundle in configurationBundle) {
            (bundle.copy() as? ConfigurationBundle)?.let { tempBundle.add(it) }
        }
        copy.configurationBundle = tempBundle.toList()
        return copy
    }
}
