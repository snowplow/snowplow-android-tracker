package com.snowplowanalytics.snowplow.internal.remoteconfiguration;

import androidx.annotation.NonNull;

import com.snowplowanalytics.snowplow.configuration.Configuration;
import com.snowplowanalytics.snowplow.configuration.RemoteConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * This class fetch a configuration from a remote source otherwise it provides a cached configuration.
 * It can manage multiple sources and multiple caches.
 */
public class ConfigurationProvider {

    public void registerRemoteSource(@NonNull RemoteConfiguration remoteConfiguration, @NonNull Consumer<List<Configuration>> onFetchCallback) {
        // To complete
    }

    @NonNull
    public ArrayList<Configuration> configurationsForNamespace(@NonNull String namespace) {
        // To complete
        return new ArrayList<>();
    }

}
