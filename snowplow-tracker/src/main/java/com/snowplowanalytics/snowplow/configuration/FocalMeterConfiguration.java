package com.snowplowanalytics.snowplow.configuration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.internal.tracker.FocalMeterConfigurationInterface;

/**
 * This configuration tells the tracker to send requests with the user ID in session context entity
 * to a Kantar endpoint used with Focal Meter.
 * The request is made when the first event with a new user ID is tracked.
 * The requests are only made if session context is enabled (default).
 */
public class FocalMeterConfiguration implements FocalMeterConfigurationInterface, Configuration {

    /**
     * @see #kantarEndpoint(String)
     */
    @Nullable
    public String kantarEndpoint;

    /**
     * Creates a configuration for the Kantar Focal Meter.
     * @param kantarEndpoint URL of the Kantar endpoint to send the requests to
     */
    public FocalMeterConfiguration(@Nullable String kantarEndpoint) {
        this.kantarEndpoint = kantarEndpoint;
    }

    /**
     * @return URL of the Kantar endpoint to send the requests to
     */
    @Nullable
    @Override
    public String getKantarEndpoint() {
        return kantarEndpoint;
    }

    /**
     * Set the Kantar endpoint
     * @param kantarEndpoint URL of the Kantar endpoint to send the requests to
     */
    @Override
    public void setKantarEndpoint(@Nullable String kantarEndpoint) {
        this.kantarEndpoint = kantarEndpoint;
    }

    @NonNull
    @Override
    public Configuration copy() {
        return new FocalMeterConfiguration(kantarEndpoint);
    }

    /**
     * The Kantar URI endpoint including the HTTP protocol.
     */
    @NonNull
    public FocalMeterConfiguration kantarEndpoint(@Nullable String kantarEndpoint) {
        this.kantarEndpoint = kantarEndpoint;
        return this;
    }
}
