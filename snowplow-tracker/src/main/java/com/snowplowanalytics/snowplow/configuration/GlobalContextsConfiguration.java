package com.snowplowanalytics.snowplow.configuration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.tracker.contexts.global.GlobalContext;

import java.util.HashMap;
import java.util.Map;

public class GlobalContextsConfiguration implements Configuration {

    @NonNull
    public final Map<String, GlobalContext> contextGenerators;

    // Constructors

    public GlobalContextsConfiguration(@Nullable Map<String, GlobalContext> contextGenerators) {
        this.contextGenerators = contextGenerators != null ? contextGenerators : new HashMap<>();
    }

    // Methods

    public boolean add(@NonNull String tag, @NonNull GlobalContext contextGenerator) {
        if (contextGenerators.get(tag) != null) {
            return false;
        }
        contextGenerators.put(tag, contextGenerator);
        return true;
    }

    @Nullable
    public GlobalContext remove(@NonNull String tag) {
        return contextGenerators.remove(tag);
    }

    // Copyable

    @Override
    @NonNull
    public GlobalContextsConfiguration copy() {
        return new GlobalContextsConfiguration(contextGenerators);
    }
}
