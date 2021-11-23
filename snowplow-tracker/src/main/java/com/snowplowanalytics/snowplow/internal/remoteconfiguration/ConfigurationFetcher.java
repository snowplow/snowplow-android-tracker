package com.snowplowanalytics.snowplow.internal.remoteconfiguration;

import android.content.Context;
import android.net.TrafficStats;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import com.snowplowanalytics.snowplow.configuration.RemoteConfiguration;
import com.snowplowanalytics.snowplow.internal.emitter.Executor;
import com.snowplowanalytics.snowplow.internal.tracker.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ConfigurationFetcher {
    private final String TAG = ConfigurationFetcher.class.getSimpleName();
    private static final int TRAFFIC_STATS_TAG = 1;

    private Future<ResponseBody> future;

    @NonNull
    private final RemoteConfiguration remoteConfiguration;
    @NonNull
    private final Consumer<FetchedConfigurationBundle> onFetchCallback;

    public ConfigurationFetcher(@NonNull Context context, @NonNull RemoteConfiguration remoteConfiguration, @NonNull Consumer<FetchedConfigurationBundle> onFetchCallback) {
        this.remoteConfiguration = remoteConfiguration;
        this.onFetchCallback = onFetchCallback;
        Executor.execute(getRunnable(context), (Throwable t) -> {
            exceptionHandler(t);
        });
    }

    // Private methods

    private Runnable getRunnable(Context context) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    ResponseBody body = performRequest(context, remoteConfiguration.endpoint);
                    if (body != null) {
                        resolveRequest(context, body, onFetchCallback);
                    }
                } catch (Exception e) {
                    Logger.e(TAG, "Unable to get remote configuration: "+e.getMessage(), e);
                }
            }
        };
    }

    private ResponseBody performRequest(@NonNull Context context, @NonNull String endpoint) throws IOException {
        Uri.Builder uriBuilder = Uri.parse(endpoint).buildUpon();
        String uri = uriBuilder.build().toString();
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
        Request request = new okhttp3.Request.Builder()
                .url(uri)
                .get()
                .build();
        TrafficStats.setThreadStatsTag(TRAFFIC_STATS_TAG);
        Response resp = client.newCall(request).execute();
        ResponseBody body = resp.body();
        if (resp.isSuccessful() && body != null) {
            return body;
        }
        return null;
    }

    private void resolveRequest(@NonNull Context context, @NonNull ResponseBody responseBody, Consumer<FetchedConfigurationBundle> onFetchCallback) throws IOException, JSONException {
        String data = responseBody.string();
        JSONObject jsonObject = new JSONObject(data);
        FetchedConfigurationBundle bundle = new FetchedConfigurationBundle(context, jsonObject);
        onFetchCallback.accept(bundle);
    }

    private void exceptionHandler(@Nullable Throwable t) {
        String message = t.getMessage();
        if (message == null) {
            message = "no message provided";
        }
        Logger.e(TAG, message, t);
    }
}
