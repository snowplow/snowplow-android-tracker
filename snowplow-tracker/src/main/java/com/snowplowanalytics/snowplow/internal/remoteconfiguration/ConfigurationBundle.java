package com.snowplowanalytics.snowplow.internal.remoteconfiguration;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.configuration.Configuration;
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration;
import com.snowplowanalytics.snowplow.configuration.SessionConfiguration;
import com.snowplowanalytics.snowplow.configuration.SubjectConfiguration;
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

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

    ConfigurationBundle(@NonNull String namespace) {
        this(namespace, null);
    }

    public ConfigurationBundle(@NonNull String namespace, @Nullable NetworkConfiguration networkConfiguration) {
        this.namespace = namespace;
        this.networkConfiguration = networkConfiguration;
    }

    public ConfigurationBundle(@NonNull Context context, @NonNull JSONObject jsonObject) throws JSONException {
        this(jsonObject.getString("namespace"));
        JSONObject json = jsonObject.optJSONObject("networkConfiguration");
        if (json != null) {
            networkConfiguration = new NetworkConfiguration(json);
        }
        json = jsonObject.optJSONObject("trackerConfiguration");
        if (json != null) {
            trackerConfiguration = new TrackerConfiguration(context.getPackageName(), json);
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

    @NonNull
    public List<Configuration> getConfigurations() {
        List<Configuration> array = new ArrayList<>();
        if (networkConfiguration != null) array.add(networkConfiguration);
        if (trackerConfiguration != null) array.add(trackerConfiguration);
        if (subjectConfiguration != null) array.add(subjectConfiguration);
        if (sessionConfiguration != null) array.add(sessionConfiguration);
        return array;
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
}
