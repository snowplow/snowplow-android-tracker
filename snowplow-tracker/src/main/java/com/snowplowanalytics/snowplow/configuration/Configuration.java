package com.snowplowanalytics.snowplow.configuration;

import androidx.annotation.NonNull;

public interface Configuration {

    @NonNull
    public Configuration copy();

}
