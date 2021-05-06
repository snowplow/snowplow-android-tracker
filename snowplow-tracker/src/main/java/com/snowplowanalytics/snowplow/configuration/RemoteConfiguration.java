package com.snowplowanalytics.snowplow.configuration;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.snowplowanalytics.snowplow.network.HttpMethod;
import com.snowplowanalytics.snowplow.network.Protocol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/**
 * Represents the configuration for fetching configurations from a remote source.
 */
public class RemoteConfiguration implements Configuration {

    /**
     * URL (without schema/protocol) used to send events to the collector.
     */
    @NonNull
    public final String endpoint;

    /**
     * Method used to send events to the collector.
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

    // Parcelable

    protected RemoteConfiguration(@NonNull Parcel in) {
        endpoint = in.readString();
        method = HttpMethod.valueOf(in.readString());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(endpoint);
        dest.writeString(method.name());
    }

    public static final Creator<RemoteConfiguration> CREATOR = new Parcelable.Creator<RemoteConfiguration>() {
        @Override
        public RemoteConfiguration createFromParcel(Parcel in) {
            return new RemoteConfiguration(in);
        }

        @Override
        public RemoteConfiguration[] newArray(int size) {
            return new RemoteConfiguration[size];
        }
    };
}
