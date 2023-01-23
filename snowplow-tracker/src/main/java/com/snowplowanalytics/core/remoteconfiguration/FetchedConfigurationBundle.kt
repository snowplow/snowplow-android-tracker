package com.snowplowanalytics.core.remoteconfiguration

import android.content.Context
import com.snowplowanalytics.snowplow.configuration.Configuration
import com.snowplowanalytics.snowplow.configuration.ConfigurationBundle
import org.json.JSONObject

class FetchedConfigurationBundle : Configuration {
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
        val copy = FetchedConfigurationBundle(schema)
        copy.configurationVersion = configurationVersion
        
        val tempBundle = ArrayList<ConfigurationBundle>()
        for (bundle in configurationBundle) {
            (bundle.copy() as? ConfigurationBundle)?.let { tempBundle.add(it) }
        }
        copy.configurationBundle = tempBundle.toList()
        return copy
    }
}
