package com.snowplowanalytics.snowplow.internal.globalcontexts;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.globalcontexts.GlobalContext;

import java.util.Set;

public interface GlobalContextsConfigurationInterface {

    /**
     * @return Set of tags associated to added GlobalContexts.
     */
    @NonNull
    Set<String> getTags();

    /**
     * Add a GlobalContext generator to the configuration of the tracker.
     * @param tag The label identifying the generator in the tracker.
     * @param contextGenerator The GlobalContext generator.
     * @return Whether the adding operation has succeeded.
     */
    boolean add(@NonNull String tag, @NonNull GlobalContext contextGenerator);

    /**
     * Remove a GlobalContext generator from the configuration of the tracker.
     * @param tag The label identifying the generator in the tracker.
     * @return Whether the removing operation has succeded.
     */
    @Nullable
    GlobalContext remove(@NonNull String tag);
}
