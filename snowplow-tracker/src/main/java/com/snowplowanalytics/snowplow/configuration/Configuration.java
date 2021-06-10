package com.snowplowanalytics.snowplow.configuration;

import androidx.annotation.NonNull;

import java.io.Serializable;

public interface Configuration extends Serializable {

    @NonNull
    Configuration copy();

}
