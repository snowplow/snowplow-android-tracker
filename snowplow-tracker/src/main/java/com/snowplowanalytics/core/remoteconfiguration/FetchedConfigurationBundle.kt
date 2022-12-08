package com.snowplowanalytics.core.remoteconfiguration

import android.content.Context
import com.snowplowanalytics.snowplow.configuration.Configuration
import com.snowplowanalytics.snowplow.configuration.ConfigurationBundle
import org.json.JSONObject

class FetchedConfigurationBundle : Configuration {
    var schema: String
    var configurationVersion: Int
    var configurationBundle: MutableList<ConfigurationBundle>

    constructor(schema: String) {
        this.schema = schema
        configurationVersion = -1
        configurationBundle = ArrayList()
    }

    // JSON formatter
    constructor(context: Context, jsonObject: JSONObject) {
        schema = jsonObject.getString("\$schema")
        configurationVersion = jsonObject.getInt("configurationVersion")
        configurationBundle = ArrayList()
        val array = jsonObject.getJSONArray("configurationBundle")
        for (i in 0 until array.length()) {
            val bundleJson = array.getJSONObject(i)
            val bundle = ConfigurationBundle(context, bundleJson)
            configurationBundle.add(bundle)
        }
    }

    // Copyable
    override fun copy(): Configuration {
        val copy = FetchedConfigurationBundle(schema)
        copy.configurationVersion = configurationVersion
        for (bundle in configurationBundle) {
            copy.configurationBundle.add(bundle.copy() as ConfigurationBundle)
        }
        return copy
    }
}
