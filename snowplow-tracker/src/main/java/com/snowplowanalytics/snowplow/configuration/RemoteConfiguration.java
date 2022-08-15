package com.snowplowanalytics.snowplow.configuration;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.snowplowanalytics.snowplow.network.HttpMethod;

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents the configuration for fetching configurations from a remote source.
 */
public class RemoteConfiguration implements Configuration {

    /**
     * URL of the remote configuration.
     */
    @NonNull
    public final String endpoint;

    /**
     * The method used to send the requests (GET or POST).
     */
    @NonNull
    public final HttpMethod method;

    /**
     * @param endpoint URL of the remote configuration.
     *                 The URL can include the schema/protocol (e.g.: `http://remote-config-url.xyz`).
     *                 In case the URL doesn't include the schema/protocol, the HTTPS protocol is
     *                 automatically selected.
     * @param method   The method used to send the requests (GET or POST).
     */
    public RemoteConfiguration(@NonNull String endpoint, @NonNull HttpMethod method) {
        Objects.requireNonNull(method);
        this.method = method;
        Objects.requireNonNull(endpoint);
        Uri uri = Uri.parse(endpoint);
        String scheme = uri.getScheme();
        if (scheme != null && Arrays.asList("https","http").contains(scheme)) {
            this.endpoint = endpoint;
        } else {
            this.endpoint = "https://" + endpoint;
        }
    }

    // Copyable

    @NonNull
    @Override
    public Configuration copy() {
        return new RemoteConfiguration(endpoint, method);
    }
}
