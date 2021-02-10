package com.snowplowanalytics.snowplow.internal.globalcontexts;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.controller.GlobalContextsController;
import com.snowplowanalytics.snowplow.globalcontexts.GlobalContext;
import com.snowplowanalytics.snowplow.internal.tracker.Tracker;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class GlobalContextsControllerImpl implements GlobalContextsController {

    @NonNull
    private final Tracker tracker;

    public GlobalContextsControllerImpl(@NonNull Tracker tracker) {
        this.tracker = tracker;
    }

    @NonNull
    @Override
    public Set<String> getTags() {
        return tracker.getGlobalContextTags();
    }

    @Override
    public boolean add(@NonNull String tag, @NonNull GlobalContext contextGenerator) {
        return tracker.addGlobalContext(contextGenerator, tag);
    }

    @Nullable
    @Override
    public GlobalContext remove(@NonNull String tag) {
        return tracker.removeGlobalContext(tag);
    }
}
