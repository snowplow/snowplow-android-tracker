package com.snowplowanalytics.core.globalcontexts;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.snowplowanalytics.snowplow.controller.GlobalContextsController;
import com.snowplowanalytics.snowplow.globalcontexts.GlobalContext;
import com.snowplowanalytics.core.Controller;
import com.snowplowanalytics.core.tracker.ServiceProviderInterface;
import com.snowplowanalytics.core.tracker.Tracker;

import java.util.Set;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class GlobalContextsControllerImpl extends Controller implements GlobalContextsController {

    public GlobalContextsControllerImpl(@NonNull ServiceProviderInterface serviceProvider) {
        super(serviceProvider);
    }

    @NonNull
    @Override
    public Set<String> getTags() {
        return getTracker().getGlobalContextTags();
    }

    @Override
    public boolean add(@NonNull String tag, @NonNull GlobalContext contextGenerator) {
        return getTracker().addGlobalContext(contextGenerator, tag);
    }

    @Nullable
    @Override
    public GlobalContext remove(@NonNull String tag) {
        return getTracker().removeGlobalContext(tag);
    }

    // Private methods

    private Tracker getTracker() {
        return this.getServiceProvider().getOrMakeTracker();
    }
}
