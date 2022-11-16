package com.snowplowanalytics.snowplow.internal.emitter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.snowplowanalytics.snowplow.controller.NetworkController;
import com.snowplowanalytics.snowplow.internal.Controller;
import com.snowplowanalytics.snowplow.internal.tracker.ServiceProviderInterface;
import com.snowplowanalytics.snowplow.network.HttpMethod;
import com.snowplowanalytics.snowplow.network.NetworkConnection;
import com.snowplowanalytics.snowplow.network.OkHttpNetworkConnection;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class NetworkControllerImpl extends Controller implements NetworkController {

    // Constructors

    public NetworkControllerImpl(@NonNull ServiceProviderInterface serviceProvider) {
        super(serviceProvider);
    }

    // Getters and Setters

    public boolean isCustomNetworkConnection() {
        NetworkConnection networkConnection = getEmitter().getNetworkConnection();
        return networkConnection != null && !(networkConnection instanceof OkHttpNetworkConnection);
    }

    @Override
    public void setEndpoint(@NonNull String endpoint) {
        getEmitter().setEmitterUri(endpoint);
    }

    @NonNull
    @Override
    public String getEndpoint() {
        return getEmitter().getEmitterUri();
    }

    @Override
    public void setMethod(@NonNull HttpMethod method) {
        getEmitter().setHttpMethod(method);
    }

    @NonNull
    @Override
    public HttpMethod getMethod() {
        return getEmitter().getHttpMethod();
    }

    @Override
    public void setCustomPostPath(@Nullable String customPostPath) {
        getDirtyConfig().customPostPath = customPostPath;
        getDirtyConfig().customPostPathUpdated = true;
        getEmitter().setCustomPostPath(customPostPath);
    }

    @Nullable
    @Override
    public String getCustomPostPath() {
        return getEmitter().getCustomPostPath();
    }

    @Override
    public void setTimeout(int timeout) {
        getEmitter().setEmitTimeout(timeout);
    }

    @Override
    public int getTimeout() {
        return getEmitter().getEmitTimeout();
    }

    // Private methods

    private Emitter getEmitter() {
        return serviceProvider.getOrMakeEmitter();
    }

    private NetworkConfigurationUpdate getDirtyConfig() {
        return serviceProvider.getNetworkConfigurationUpdate();
    }
}
