package com.snowplowanalytics.snowplow.internal.tracker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.controller.FocalMeterController;
import com.snowplowanalytics.snowplow.internal.Controller;

public class FocalMeterControllerImpl extends Controller implements FocalMeterController {

    public FocalMeterControllerImpl(@NonNull ServiceProviderInterface serviceProvider) {
        super(serviceProvider);
    }

    @Nullable
    @Override
    public String getKantarEndpoint() {
        return getTracker().getFocalMeterEndpoint();
    }

    @Override
    public void setKantarEndpoint(@Nullable String kantarEndpoint) {
        getTracker().setFocalMeterEndpoint(kantarEndpoint);
    }

    // Private methods

    private Tracker getTracker() {
        return serviceProvider.getOrMakeTracker();
    }

}
