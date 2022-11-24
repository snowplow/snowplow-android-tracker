package com.snowplowanalytics.snowplow.configuration

import android.content.Context
import org.json.JSONObject

class ConfigurationBundle @JvmOverloads constructor(
    @JvmField val namespace: String,
    networkConfiguration: NetworkConfiguration? = null
) : Configuration {
    @JvmField
    var networkConfiguration: NetworkConfiguration? = null
    @JvmField
    var trackerConfiguration: TrackerConfiguration? = null
    @JvmField
    var subjectConfiguration: SubjectConfiguration? = null
    @JvmField
    var sessionConfiguration: SessionConfiguration? = null

    init {
        this.networkConfiguration = networkConfiguration
    }

    constructor(
        context: Context,
        jsonObject: JSONObject
    ) : this(jsonObject.getString("namespace")) {
        var json = jsonObject.optJSONObject("networkConfiguration")
        json?.let { networkConfiguration = NetworkConfiguration(it) }

        json = jsonObject.optJSONObject("trackerConfiguration")
        json?.let { trackerConfiguration = TrackerConfiguration(context.packageName, it) }

        json = jsonObject.optJSONObject("subjectConfiguration")
        json?.let { subjectConfiguration = SubjectConfiguration(it) }
        
        json = jsonObject.optJSONObject("sessionConfiguration")
        json?.let { sessionConfiguration = SessionConfiguration(it) }
    }

    val configurations: List<Configuration>
        get() {
            val array: MutableList<Configuration> = ArrayList()
            networkConfiguration?.let { array.add(it) }
            trackerConfiguration?.let { array.add(it) }
            subjectConfiguration?.let { array.add(it) }
            sessionConfiguration?.let { array.add(it) }
            return array
        }

    // Copyable
    override fun copy(): Configuration {
        val copy = ConfigurationBundle(namespace)
        copy.networkConfiguration = networkConfiguration
        copy.trackerConfiguration = trackerConfiguration
        copy.subjectConfiguration = subjectConfiguration
        copy.sessionConfiguration = sessionConfiguration
        return copy
    }
}
