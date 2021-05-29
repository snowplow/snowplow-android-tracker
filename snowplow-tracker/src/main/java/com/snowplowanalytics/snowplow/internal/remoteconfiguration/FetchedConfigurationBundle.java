package com.snowplowanalytics.snowplow.internal.remoteconfiguration;

import androidx.annotation.NonNull;

import com.snowplowanalytics.snowplow.configuration.Configuration;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class FetchedConfigurationBundle implements Configuration {

    @NonNull
    public String formatVersion;

    public int configurationVersion;

    @NonNull
    public List<ConfigurationBundle> configurationBundle;

    FetchedConfigurationBundle(@NonNull String formatVersion) {
        this.formatVersion = formatVersion;
        this.configurationVersion = -1;
        this.configurationBundle = new ArrayList<>();
    }

    // JSON formatter

    public FetchedConfigurationBundle(@NonNull JSONObject jsonObject) throws JSONException {
        formatVersion = jsonObject.getString("formatVersion");
        configurationVersion = jsonObject.getInt("configurationVersion");
        configurationBundle = new ArrayList<>();
        JSONArray array = jsonObject.getJSONArray("configurationBundle");
        for (int i = 0; i < array.length(); i++) {
            JSONObject bundleJson = array.getJSONObject(i);
            ConfigurationBundle bundle = new ConfigurationBundle(bundleJson);
            configurationBundle.add(bundle);
        }
    }

    // Copyable

    @NonNull
    @Override
    public Configuration copy() {
        FetchedConfigurationBundle copy = new FetchedConfigurationBundle(formatVersion);
        copy.configurationVersion = configurationVersion;
        for (ConfigurationBundle bundle : configurationBundle) {
            copy.configurationBundle.add((ConfigurationBundle)bundle.copy());
        }
        return copy;
    }
}
