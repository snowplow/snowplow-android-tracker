package com.snowplowanalytics.snowplow.internal.globalcontexts;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.globalcontexts.GlobalContext;

import java.util.Set;

public interface GlobalContextsConfigurationInterface {

    @NonNull
    Set<String> getTags();

    boolean add(@NonNull String tag, @NonNull GlobalContext contextGenerator);

    @Nullable
    GlobalContext remove(@NonNull String tag);
}
