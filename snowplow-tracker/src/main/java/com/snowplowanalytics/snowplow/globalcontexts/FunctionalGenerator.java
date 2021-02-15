package com.snowplowanalytics.snowplow.globalcontexts;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.tracker.InspectableEvent;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;

import java.util.List;

public abstract class FunctionalGenerator {

    @Nullable
    public abstract List<SelfDescribingJson> apply(@NonNull InspectableEvent event);

}

