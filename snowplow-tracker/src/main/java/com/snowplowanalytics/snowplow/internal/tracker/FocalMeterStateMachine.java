package com.snowplowanalytics.snowplow.internal.tracker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.entity.ClientSessionEntity;
import com.snowplowanalytics.snowplow.event.Event;
import com.snowplowanalytics.snowplow.internal.emitter.Executor;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.InspectableEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FocalMeterStateMachine implements StateMachineInterface {
    private final String TAG = FocalMeterStateMachine.class.getSimpleName();

    @NonNull
    private final String endpoint;
    @Nullable
    private String lastUserId;

    public FocalMeterStateMachine(@NonNull String endpoint) {
        this.endpoint = endpoint;
    }

    @NonNull
    @Override
    public List<String> subscribedEventSchemasForTransitions() {
        return new ArrayList<>();
    }

    @NonNull
    @Override
    public List<String> subscribedEventSchemasForEntitiesGeneration() {
        return Collections.singletonList("*");
    }

    @NonNull
    @Override
    public List<String> subscribedEventSchemasForPayloadUpdating() {
        return new ArrayList<>();
    }

    @Nullable
    @Override
    public State transition(@NonNull Event event, @Nullable State state) {
        return null;
    }

    /*
    Note: this is a workaround that abuses the entities(event, state) function to check the
    client session context entity for changes. We should provide a dedicated endpoint for
    this purpose in future versions.
     */
    @Nullable
    @Override
    public List<SelfDescribingJson> entities(@NonNull InspectableEvent event, @Nullable State state) {
        if (event instanceof TrackerEvent) {
            TrackerEvent trackerEvent = (TrackerEvent) event;

            for (SelfDescribingJson entity : trackerEvent.contexts) {
                if (entity instanceof ClientSessionEntity) {
                    ClientSessionEntity clientSessionEntity = (ClientSessionEntity) entity;
                    if (shouldUpdate(clientSessionEntity.getUserId())) {
                        makeRequest(clientSessionEntity);
                    }
                }
            }
        }

        return null;
    }

    @Nullable
    @Override
    public Map<String, Object> payloadValues(@NonNull InspectableEvent event, @Nullable State state) {
        return null;
    }

    private synchronized boolean shouldUpdate(@Nullable String newUserId) {
        if (Objects.equals(newUserId, lastUserId)) { return false; }
        lastUserId = newUserId;
        return true;
    }

    private void makeRequest(@NonNull ClientSessionEntity entity) {
        Executor.execute(TAG, () -> {
            HttpUrl url = HttpUrl.parse(endpoint);
            if (url == null) {
                Logger.e(TAG, "Failed to parse Kantar endpoint URL", endpoint);
                return;
            }
            String userId = entity.getUserId();
            HttpUrl.Builder urlBuilder = url.newBuilder();
            urlBuilder.addQueryParameter("vendor", "snowplow");
            urlBuilder.addQueryParameter("cs_fpid", userId);
            urlBuilder.addQueryParameter("c12", "not_set");
            url = urlBuilder.build();

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build();
            Request request = new okhttp3.Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try {
                Response resp = client.newCall(request).execute();
                if (resp.isSuccessful()) {
                    Logger.d(TAG, "Request to Kantar endpoint sent with user ID: " + userId);
                } else {
                    Logger.e(TAG, "Request to Kantar endpoint was not successful");
                }
            } catch (IOException e) {
                Logger.e(TAG, "Failed to request Kantar endpoint", e);
            }
        });
    }
}
