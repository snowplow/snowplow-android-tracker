package com.snowplowanalytics.snowplow.internal.emitter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.snowplowanalytics.snowplow.controller.NetworkController;
import com.snowplowanalytics.snowplow.network.HttpMethod;
import com.snowplowanalytics.snowplow.network.Protocol;

@RestrictTo(RestrictTo.Scope.LIBRARY)
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
