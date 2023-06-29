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
package com.snowplowanalytics.snowplow.configuration

import android.content.Context
import org.json.JSONObject

/**
 * For remote configuration. Bundles [Configuration] objects to apply to a tracker.
 */
class ConfigurationBundle @JvmOverloads constructor(
    val namespace: String,
    networkConfiguration: NetworkConfiguration? = null
) : Configuration {
    var networkConfiguration: NetworkConfiguration? = null
    var trackerConfiguration: TrackerConfiguration? = null
    var subjectConfiguration: SubjectConfiguration? = null
    var sessionConfiguration: SessionConfiguration? = null
    var emitterConfiguration: EmitterConfiguration? = null
    
    val configurations: List<Configuration>
        get() {
            val array: MutableList<Configuration> = ArrayList()
            networkConfiguration?.let { array.add(it) }
            trackerConfiguration?.let { array.add(it) }
            subjectConfiguration?.let { array.add(it) }
            sessionConfiguration?.let { array.add(it) }
            emitterConfiguration?.let { array.add(it) }
            return array
        }

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

        json = jsonObject.optJSONObject("emitterConfiguration")
        json?.let { emitterConfiguration = EmitterConfiguration(it) }
    }

    fun updateSourceConfig(sourceBundle: ConfigurationBundle) {
        sourceBundle.networkConfiguration?.let {
            if (networkConfiguration == null) networkConfiguration = NetworkConfiguration()
            networkConfiguration?.sourceConfig = it
        }
        sourceBundle.trackerConfiguration?.let {
            if (trackerConfiguration == null) trackerConfiguration = TrackerConfiguration()
            trackerConfiguration?.sourceConfig = it
        }
        sourceBundle.subjectConfiguration?.let {
            if (subjectConfiguration == null) subjectConfiguration = SubjectConfiguration()
            subjectConfiguration?.sourceConfig = it
        }
        sourceBundle.sessionConfiguration?.let {
            if (sessionConfiguration == null) sessionConfiguration = SessionConfiguration()
            sessionConfiguration?.sourceConfig = it
        }
        sourceBundle.emitterConfiguration?.let {
            if (emitterConfiguration == null) emitterConfiguration = EmitterConfiguration()
            emitterConfiguration?.sourceConfig = it
        }
    }

    // Copyable
    override fun copy(): Configuration {
        val copy = ConfigurationBundle(namespace)
        copy.networkConfiguration = networkConfiguration
        copy.trackerConfiguration = trackerConfiguration
        copy.subjectConfiguration = subjectConfiguration
        copy.sessionConfiguration = sessionConfiguration
        copy.emitterConfiguration = emitterConfiguration
        return copy
    }
}
