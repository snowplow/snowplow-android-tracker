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
class ConfigurationProvider @JvmOverloads constructor(
    private val remoteConfiguration: RemoteConfiguration,
    defaultBundles: List<ConfigurationBundle>? = null
) {
    private val cache: ConfigurationCache = ConfigurationCache(remoteConfiguration)
    private var fetcher: ConfigurationFetcher? = null
    private var defaultBundle: FetchedConfigurationBundle? = null
    private var cacheBundle: FetchedConfigurationBundle? = null

    init {
        if (defaultBundles != null) {
            val bundle = FetchedConfigurationBundle("1.0")
            bundle.configurationVersion = Int.MIN_VALUE
            bundle.configurationBundle = defaultBundles
            defaultBundle = bundle
        }
    }

    @Synchronized
    fun retrieveConfiguration(
        context: Context,
        onlyRemote: Boolean,
        onFetchCallback: Consumer<Pair<FetchedConfigurationBundle, ConfigurationState>>
    ) {
        if (!onlyRemote) {
            if (cacheBundle == null) {
                cacheBundle = cache.readCache(context)
            }
            if (cacheBundle != null) {
                onFetchCallback.accept(Pair(cacheBundle, ConfigurationState.CACHED))
            } else if (defaultBundle != null) {
                onFetchCallback.accept(Pair(defaultBundle, ConfigurationState.DEFAULT))
            }
        }
        fetcher = ConfigurationFetcher(
            context,
            remoteConfiguration,
            object : Consumer<FetchedConfigurationBundle> {
                override fun accept(fetchedConfigurationBundle: FetchedConfigurationBundle) {
                    if (!schemaCompatibility(fetchedConfigurationBundle.schema)) {
                        return
                    }
                    synchronized(this) {
                        if (cacheBundle != null && cacheBundle!!.configurationVersion >= fetchedConfigurationBundle.configurationVersion) {
                            return
                        }
                        cache.writeCache(context, fetchedConfigurationBundle)
                        cacheBundle = fetchedConfigurationBundle
                        onFetchCallback.accept(
                            Pair(
                                fetchedConfigurationBundle,
                                ConfigurationState.FETCHED
                            )
                        )
                    }
                }
            })
    }

    // Private methods
    private fun schemaCompatibility(schema: String): Boolean {
        return schema.startsWith("http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-")
    }
}
