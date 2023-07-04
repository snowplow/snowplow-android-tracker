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
import androidx.core.util.Consumer
import androidx.core.util.Pair

import com.snowplowanalytics.snowplow.configuration.ConfigurationBundle
import com.snowplowanalytics.snowplow.configuration.ConfigurationState
import com.snowplowanalytics.snowplow.configuration.RemoteConfiguration

/**
 * This class fetch a configuration from a remote source otherwise it provides a cached configuration.
 * It can manage multiple sources and multiple caches.
 */
class RemoteConfigurationProvider @JvmOverloads constructor(
    private val remoteConfiguration: RemoteConfiguration,
    defaultBundles: List<ConfigurationBundle>? = null,
    defaultBundleVersion: Int = Int.MIN_VALUE
) {
    private val cache: RemoteConfigurationCache = RemoteConfigurationCache(remoteConfiguration)
    private var fetcher: RemoteConfigurationFetcher? = null
    private var defaultBundle: RemoteConfigurationBundle? = null
    private var cacheBundle: RemoteConfigurationBundle? = null

    init {
        if (defaultBundles != null) {
            val bundle = RemoteConfigurationBundle("1.0")
            bundle.configurationVersion = defaultBundleVersion
            bundle.configurationBundle = defaultBundles
            defaultBundle = bundle
        }
    }

    @Synchronized
    fun retrieveConfiguration(
        context: Context,
        onlyRemote: Boolean,
        onFetchCallback: Consumer<Pair<RemoteConfigurationBundle, ConfigurationState>>
    ) {
        if (!onlyRemote) {
            if (cacheBundle == null) {
                cacheBundle = cache.readCache(context)
            }
            if (cacheBundle != null) {
                defaultBundle?.let { cacheBundle?.updateSourceConfig(it) }
                onFetchCallback.accept(Pair(cacheBundle, ConfigurationState.CACHED))
            } else if (defaultBundle != null) {
                onFetchCallback.accept(Pair(defaultBundle, ConfigurationState.DEFAULT))
            }
        }
        fetcher = RemoteConfigurationFetcher(
            context,
            remoteConfiguration,
            object : Consumer<RemoteConfigurationBundle> {
                override fun accept(bundle: RemoteConfigurationBundle) {
                    if (!schemaCompatibility(bundle.schema)) {
                        return
                    }
                    synchronized(this) {
                        val isNewer = (cacheBundle ?: defaultBundle)?.let { it.configurationVersion < bundle.configurationVersion } ?: true
                        if (isNewer) {
                            defaultBundle?.let { bundle.updateSourceConfig(it) }
                            cache.writeCache(context, bundle)
                            cacheBundle = bundle
                            onFetchCallback.accept(
                                Pair(
                                    bundle,
                                    ConfigurationState.FETCHED
                                )
                            )
                        }
                    }
                }
            })
    }

    // Private methods
    private fun schemaCompatibility(schema: String): Boolean {
        return schema.startsWith("http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-")
    }
}
