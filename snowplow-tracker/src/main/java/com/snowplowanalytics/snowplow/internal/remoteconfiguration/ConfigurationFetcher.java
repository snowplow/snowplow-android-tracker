package com.snowplowanalytics.snowplow.internal.remoteconfiguration;

import android.net.TrafficStats;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import com.snowplowanalytics.snowplow.configuration.RemoteConfiguration;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ConfigurationFetcher {
    private static final int TRAFFIC_STATS_TAG = 1;

    @NonNull
    private final RemoteConfiguration remoteConfiguration;
    @NonNull
    private final Consumer<FetchedConfigurationBundle> onFetchCallback;

    public ConfigurationFetcher(@NonNull RemoteConfiguration remoteConfiguration, @NonNull Consumer<FetchedConfigurationBundle> onFetchCallback) {
        this.remoteConfiguration = remoteConfiguration;
        this.onFetchCallback = onFetchCallback;
        performRequest();
    }

    // Private methods

    private void performRequest() {
        Uri.Builder uriBuilder = Uri.parse(remoteConfiguration.endpoint).buildUpon();
        String uri = uriBuilder.build().toString();
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
        Request request = new okhttp3.Request.Builder()
                .url(uri)
                .get()
                .build();
        try {
            TrafficStats.setThreadStatsTag(TRAFFIC_STATS_TAG);
            Response resp = client.newCall(request).execute();
            ResponseBody body = resp.body();
            if (resp.isSuccessful() && body != null) {
                resolveRequest(body);
            }
        } catch (IOException e) {
            e.printStackTrace();
            // TODO: Handle exception
        }
    }

    private void resolveRequest(ResponseBody responseBody) {
        String data = null;
        try {
            data = responseBody.string();
            JSONObject jsonObject = new JSONObject(data);
            FetchedConfigurationBundle bundle = new FetchedConfigurationBundle(jsonObject);
            if (bundle != null) {
                onFetchCallback.accept(bundle);
            }
        } catch (IOException e) {
            // TODO: Handle exception
        } catch (JSONException e) {
            // TODO: Handle exception
        }
    }
}
