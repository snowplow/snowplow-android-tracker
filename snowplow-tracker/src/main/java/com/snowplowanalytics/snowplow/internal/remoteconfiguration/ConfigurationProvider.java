package com.snowplowanalytics.snowplow.internal.remoteconfiguration;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.core.util.Pair;

import com.snowplowanalytics.snowplow.configuration.RemoteConfiguration;

import java.util.List;

/**
 * This class fetch a configuration from a remote source otherwise it provides a cached configuration.
 * It can manage multiple sources and multiple caches.
 */
public class ConfigurationProvider {

    @NonNull
    private RemoteConfiguration remoteConfiguration;
    @NonNull
    private ConfigurationCache cache;
    @Nullable
    private ConfigurationFetcher fetcher;
    @Nullable
    private FetchedConfigurationBundle defaultBundle;
    @Nullable
    private FetchedConfigurationBundle cacheBundle;

    public ConfigurationProvider(@NonNull RemoteConfiguration remoteConfiguration) {
        this(remoteConfiguration, null);
    }

    public ConfigurationProvider(@NonNull RemoteConfiguration remoteConfiguration, @Nullable List<ConfigurationBundle> defaultBundles) {
        this.remoteConfiguration = remoteConfiguration;
        this.cache = new ConfigurationCache(remoteConfiguration);
        if (defaultBundles != null) {
            FetchedConfigurationBundle bundle = new FetchedConfigurationBundle("1.0");
            bundle.configurationVersion = Integer.MIN_VALUE;
            bundle.configurationBundle = defaultBundles;
            defaultBundle = bundle;
        }
    }

    public synchronized void retrieveConfiguration(@NonNull Context context, boolean onlyRemote, @NonNull Consumer<Pair<FetchedConfigurationBundle, ConfigurationState>> onFetchCallback) {
        if (!onlyRemote) {
            if (cacheBundle == null) {
                cacheBundle = cache.readCache(context);
            }
            if (cacheBundle != null) {
                onFetchCallback.accept(new Pair<>(cacheBundle, ConfigurationState.CACHED));
            } else if (defaultBundle != null) {
                onFetchCallback.accept(new Pair<>(defaultBundle, ConfigurationState.DEFAULT));
            }
        }
        fetcher = new ConfigurationFetcher(context, remoteConfiguration, new Consumer<FetchedConfigurationBundle>() {
            @Override
            public void accept(FetchedConfigurationBundle fetchedConfigurationBundle) {
                if (!schemaCompatibility(fetchedConfigurationBundle.schema)) {
                    return;
                }
                synchronized (this) {
                    if (cacheBundle != null && cacheBundle.configurationVersion >= fetchedConfigurationBundle.configurationVersion) {
                        return;
                    }
                    cache.writeCache(context, fetchedConfigurationBundle);
                    cacheBundle = fetchedConfigurationBundle;
                    onFetchCallback.accept(new Pair<>(fetchedConfigurationBundle, ConfigurationState.FETCHED));
                }
            }
        });
    }

    // Private methods

    private boolean schemaCompatibility(@NonNull String schema) {
        return schema.startsWith("http://iglucentral.com/schemas/com.snowplowanalytics.mobile/remote_config/jsonschema/1-");
    }
}
