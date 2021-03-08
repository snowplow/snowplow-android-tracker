package com.snowplowanalytics.snowplow.configuration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.globalcontexts.GlobalContext;
import com.snowplowanalytics.snowplow.internal.globalcontexts.GlobalContextsConfigurationInterface;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GlobalContextsConfiguration implements Configuration, GlobalContextsConfigurationInterface {

    @NonNull
    public final Map<String, GlobalContext> contextGenerators;

    // Constructors

    public GlobalContextsConfiguration(@Nullable Map<String, GlobalContext> contextGenerators) {
        this.contextGenerators = contextGenerators != null ? contextGenerators : new HashMap<>();
    }

    // Methods

    @NonNull
    @Override
    public Set<String> getTags() {
        return contextGenerators.keySet();
    }

    @Override
    public boolean add(@NonNull String tag, @NonNull GlobalContext contextGenerator) {
        if (contextGenerators.get(tag) != null) {
            return false;
        }
        contextGenerators.put(tag, contextGenerator);
        return true;
    }

    @Override
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
