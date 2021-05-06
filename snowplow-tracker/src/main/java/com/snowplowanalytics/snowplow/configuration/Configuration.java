package com.snowplowanalytics.snowplow.configuration;

import android.os.Parcelable;

import androidx.annotation.NonNull;

public interface Configuration extends Parcelable {

    @NonNull
    Configuration copy();

}
