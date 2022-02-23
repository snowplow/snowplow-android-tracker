package com.snowplowanalytics.snowplow.tracker;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.snowplowanalytics.snowplow.internal.tracker.Logger;
import com.snowplowanalytics.snowplow.network.HttpMethod;
import com.snowplowanalytics.snowplow.network.NetworkConnection;
import com.snowplowanalytics.snowplow.network.Request;
import com.snowplowanalytics.snowplow.network.RequestResult;

import java.util.ArrayList;
import java.util.List;

class MockNetworkConnection implements NetworkConnection {
    public boolean successfulConnection;
    public HttpMethod httpMethod;

    public final List<List<RequestResult>> previousResults = new ArrayList<>();

    public MockNetworkConnection(HttpMethod httpMethod, boolean successfulConnection) {
        this.httpMethod = httpMethod;
        this.successfulConnection = successfulConnection;
    }

    public int sendingCount() {
        return previousResults.size();
    }

    @NonNull
    @Override
    public List<RequestResult> sendRequests(@NonNull List<Request> requests) {
        List<RequestResult> requestResults = new ArrayList<>(requests.size());
        for (Request request : requests) {
            boolean isSuccessful = request.oversize || successfulConnection;
            RequestResult result = new RequestResult(isSuccessful, request.emitterEventIds);
            Logger.v("MockNetworkConnection", "Sent: %s with success: %s", request.emitterEventIds, Boolean.valueOf(isSuccessful).toString());
            requestResults.add(result);
        }
        previousResults.add(requestResults);
        return requestResults;
    }

    @NonNull
    @Override
    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    @NonNull
    @Override
    public Uri getUri() {
        return Uri.parse("http://fake-url.com");
    }
}
