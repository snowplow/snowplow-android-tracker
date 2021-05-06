package com.snowplowanalytics.snowplow.internal.remoteconfiguration;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.configuration.Configuration;
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration;
import com.snowplowanalytics.snowplow.configuration.SessionConfiguration;
import com.snowplowanalytics.snowplow.configuration.SubjectConfiguration;
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration;

import org.json.JSONException;
import org.json.JSONObject;

public class ConfigurationBundle implements Configuration {

    @NonNull
    public final String namespace;

    @Nullable
    public NetworkConfiguration networkConfiguration;
    @Nullable
    public TrackerConfiguration trackerConfiguration;
    @Nullable
    public SubjectConfiguration subjectConfiguration;
    @Nullable
    public SessionConfiguration sessionConfiguration;

    private ConfigurationBundle(String namespace) {
        this.namespace = namespace;
    }

    public ConfigurationBundle(@NonNull JSONObject jsonObject) throws JSONException {
        this(jsonObject.getString("namespace"));
        JSONObject json = jsonObject.optJSONObject("networkConfiguration");
        if (json != null) {
            networkConfiguration = new NetworkConfiguration(json);
        }
        json = jsonObject.optJSONObject("trackerConfiguration");
        if (json != null) {
            trackerConfiguration = new TrackerConfiguration(json);
        }
        json = jsonObject.optJSONObject("subjectConfiguration");
        if (json != null) {
            subjectConfiguration = new SubjectConfiguration(json);
        }
        json = jsonObject.optJSONObject("sessionConfiguration");
        if (json != null) {
            sessionConfiguration = new SessionConfiguration(json);
        }
    }

    // Copyable

    @NonNull
    @Override
    public Configuration copy() {
        ConfigurationBundle copy = new ConfigurationBundle(namespace);
        copy.networkConfiguration = networkConfiguration;
        copy.trackerConfiguration = trackerConfiguration;
        copy.subjectConfiguration = subjectConfiguration;
        copy.sessionConfiguration = sessionConfiguration;
        return copy;
    }

    // Parcelable

    protected ConfigurationBundle(@NonNull Parcel in) {
        namespace = in.readString();
        networkConfiguration = in.readParcelable(NetworkConfiguration.class.getClassLoader());
        trackerConfiguration = in.readParcelable(TrackerConfiguration.class.getClassLoader());
        subjectConfiguration = in.readParcelable(SubjectConfiguration.class.getClassLoader());
        sessionConfiguration = in.readParcelable(SessionConfiguration.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(namespace);
        dest.writeParcelable(networkConfiguration, flags);
        dest.writeParcelable(trackerConfiguration, flags);
        dest.writeParcelable(subjectConfiguration, flags);
        dest.writeParcelable(sessionConfiguration, flags);
    }

    public static final Creator<ConfigurationBundle> CREATOR = new Parcelable.Creator<ConfigurationBundle>() {
        @Override
        public ConfigurationBundle createFromParcel(Parcel in) {
            return new ConfigurationBundle(in);
        }

        @Override
        public ConfigurationBundle[] newArray(int size) {
            return new ConfigurationBundle[size];
        }
    };
}
