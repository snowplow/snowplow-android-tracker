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

public class MockNetworkConnection implements NetworkConnection {
    public int statusCode;
    public HttpMethod httpMethod;

    public final List<List<Request>> previousRequests = new ArrayList<>();
    public final List<List<RequestResult>> previousResults = new ArrayList<>();

    public MockNetworkConnection(HttpMethod httpMethod, int statusCode) {
        this.httpMethod = httpMethod;
        this.statusCode = statusCode;
    }

    public int sendingCount() {
        return previousResults.size();
    }

    @NonNull
    @Override
    public List<RequestResult> sendRequests(@NonNull List<Request> requests) {
        List<RequestResult> requestResults = new ArrayList<>(requests.size());
        for (Request request : requests) {
            RequestResult result = new RequestResult(statusCode, request.oversize, request.emitterEventIds);
            Logger.v("MockNetworkConnection", "Sent: %s with success: %s", request.emitterEventIds, Boolean.valueOf(result.isSuccessful()).toString());
            requestResults.add(result);
        }
        previousRequests.add(requests);
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

    public List<Request> getAllRequests() {
        List<Request> flattened = new ArrayList<>();
        for (List<Request> requests : previousRequests) {
            flattened.addAll(requests);
        }
        return flattened;
    }

    public int countRequests() {
        return getAllRequests().size();
    }
}
