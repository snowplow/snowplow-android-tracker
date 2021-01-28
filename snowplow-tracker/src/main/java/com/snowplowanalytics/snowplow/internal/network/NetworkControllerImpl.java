package com.snowplowanalytics.snowplow.internal.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration;
import com.snowplowanalytics.snowplow.controller.NetworkController;
import com.snowplowanalytics.snowplow.tracker.Emitter;
import com.snowplowanalytics.snowplow.tracker.emitter.HttpMethod;
import com.snowplowanalytics.snowplow.tracker.emitter.Protocol;

public class NetworkControllerImpl implements NetworkController {

    @NonNull
    private final Emitter emitter;

    // Constructors

    public NetworkControllerImpl(@NonNull Emitter emitter) {
        this.emitter = emitter;
    }

    // Getters and Setters

    @Override
    public void setEndpoint(@NonNull String endpoint) {
        emitter.setEmitterUri(endpoint);
    }

    @NonNull
    @Override
    public String getEndpoint() {
        return emitter.getEmitterUri();
    }

    @Override
    public void setMethod(@NonNull HttpMethod method) {
        emitter.setHttpMethod(method);
    }

    @NonNull
    @Override
    public HttpMethod getMethod() {
        return emitter.getHttpMethod();
    }

    @Override
    public void setProtocol(@NonNull Protocol protocol) {
        emitter.setRequestSecurity(protocol);
    }

    @NonNull
    @Override
    public Protocol getProtocol() {
        return emitter.getRequestSecurity();
    }

    @Override
    public void setCustomPostPath(@Nullable String customPostPath) {
        emitter.setCustomPostPath(customPostPath);
    }

    @Nullable
    @Override
    public String getCustomPostPath() {
        return emitter.getCustomPostPath();
    }

    @Override
    public void setTimeout(int timeout) {
        emitter.setEmitTimeout(timeout);
    }

    @Override
    public int getTimeout() {
        return emitter.getEmitTimeout();
    }
}
