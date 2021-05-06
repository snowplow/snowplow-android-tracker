package com.snowplowanalytics.snowplow.internal.remoteconfiguration;

import androidx.annotation.NonNull;

import com.snowplowanalytics.snowplow.configuration.Configuration;
import com.snowplowanalytics.snowplow.configuration.RemoteConfiguration;

import java.util.List;
import java.util.function.Consumer;

public class ConfigurationFetcher {

    @NonNull
    private final RemoteConfiguration remoteConfiguration;
    @NonNull
    private final Consumer<List<Configuration>> onFetchCallback;

    public ConfigurationFetcher(@NonNull RemoteConfiguration remoteConfiguration, @NonNull Consumer<List<Configuration>> onFetchCallback) {

        this.remoteConfiguration = remoteConfiguration;
        this.onFetchCallback = onFetchCallback;
    }

}
