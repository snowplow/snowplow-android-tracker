package com.snowplowanalytics.snowplow.configuration;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.snowplowanalytics.snowplow.internal.session.SessionConfigurationInterface;
import com.snowplowanalytics.snowplow.util.TimeMeasure;

/**
 * This class allows the tracker configuration from the session perspective.
 * The SessionConfiguration can be used to setup the behaviour of sessions.
 *
 * A session is a context which is appended to each event sent.
 * The values it brings can change based on:
 * - the timeout set for the inactivity of app when in foreground;
 * - the timeout set for the inactivity of app when in background.
 *
 * Session data is maintained for the life of the application being installed on a device.
 * Essentially will update if it is not accessed within a configurable timeout.
 */
public class SessionConfiguration implements SessionConfigurationInterface, Configuration {

    /**
     * The timeout set for the inactivity of app when in foreground.
     */
    @NonNull
    public TimeMeasure foregroundTimeout;
    /**
     * The timeout set for the inactivity of app when in background.
     */
    @NonNull
    public TimeMeasure backgroundTimeout;

    // Constructors

    /**
     * It setup the behaviour of sessions in the tracker.
     * @param foregroundTimeout The timeout set for the inactivity of app when in foreground.
     * @param backgroundTimeout The timeout set for the inactivity of app when in background.
     */
    public SessionConfiguration(@NonNull TimeMeasure foregroundTimeout, @NonNull TimeMeasure backgroundTimeout) {
        this.foregroundTimeout = foregroundTimeout;
        this.backgroundTimeout = backgroundTimeout;
    }

    // Getters and Setters

    /**
     * @see #foregroundTimeout
     */
    @Override
    @NonNull
    public TimeMeasure getForegroundTimeout() {
        return foregroundTimeout;
    }

    /**
     * @see #foregroundTimeout
     */
    @Override
    public void setForegroundTimeout(@NonNull TimeMeasure foregroundTimeout) {
        this.foregroundTimeout = foregroundTimeout;
    }

    /**
     * @see #backgroundTimeout
     */
    @Override
    @NonNull
    public TimeMeasure getBackgroundTimeout() {
        return backgroundTimeout;
    }

    /**
     * @see #backgroundTimeout
     */
    @Override
    public void setBackgroundTimeout(@NonNull TimeMeasure backgroundTimeout) {
        this.backgroundTimeout = backgroundTimeout;
    }

    // Copyable

    @Override
    @NonNull
    public Configuration copy() {
        return new SessionConfiguration(foregroundTimeout, backgroundTimeout);
    }
}
