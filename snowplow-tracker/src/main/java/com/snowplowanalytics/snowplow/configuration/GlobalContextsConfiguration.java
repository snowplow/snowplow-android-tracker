package com.snowplowanalytics.snowplow.configuration;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.globalcontexts.GlobalContext;
import com.snowplowanalytics.snowplow.internal.globalcontexts.GlobalContextsConfigurationInterface;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class allows the setup of Global Contexts which are attached to selected events.
 */
public class GlobalContextsConfiguration implements Configuration, GlobalContextsConfigurationInterface {

    @NonNull
    public final Map<String, GlobalContext> contextGenerators;

    // Constructors

    /**
     * Allows for the creation of a map of tags and associated {@link GlobalContext GlobalContext} generators.
     * They are used by the tracker based on the filter settings defined on each {@link GlobalContext GlobalContext}.
     * @param contextGenerators
     */
    public GlobalContextsConfiguration(@Nullable Map<String, GlobalContext> contextGenerators) {
        this.contextGenerators = contextGenerators != null ? contextGenerators : new HashMap<>();
    }

    // Methods

    /**
     * @return Set of tags associated to added GlobalContexts.
     */
    @NonNull
    @Override
    public Set<String> getTags() {
        return contextGenerators.keySet();
    }

    /**
     * Add a GlobalContext generator to the configuration of the tracker.
     * @param tag The label identifying the generator in the tracker.
     * @param contextGenerator The GlobalContext generator.
     * @return Whether the adding operation has succeeded.
     */
    @Override
    public boolean add(@NonNull String tag, @NonNull GlobalContext contextGenerator) {
        if (contextGenerators.get(tag) != null) {
            return false;
        }
        contextGenerators.put(tag, contextGenerator);
        return true;
    }

    /**
     * Remove a GlobalContext generator from the configuration of the tracker.
     * @param tag The label identifying the generator in the tracker.
     * @return Whether the removing operation has succeded.
     */
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

    // Parcelable

    protected GlobalContextsConfiguration(@NonNull Parcel in) {
        // Not possible as we can't encode and decode contextGenerators
        this(new HashMap<>());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
    }

    public static final Creator<GlobalContextsConfiguration> CREATOR = new Parcelable.Creator<GlobalContextsConfiguration>() {
        @Override
        public GlobalContextsConfiguration createFromParcel(Parcel in) {
            return new GlobalContextsConfiguration(in);
        }

        @Override
        public GlobalContextsConfiguration[] newArray(int size) {
            return new GlobalContextsConfiguration[size];
        }
    };

}
