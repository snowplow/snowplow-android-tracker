package com.snowplowanalytics.snowplow.tracker;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.snowplowanalytics.snowplow.tracker.emitter.HttpMethod;
import com.snowplowanalytics.snowplow.tracker.emitter.RequestResult;
import com.snowplowanalytics.snowplow.tracker.networkconnection.Request;

import java.util.List;

/**
 * Interface for the component that
 * sends events to the collector.
 */
public interface NetworkConnection {

    /**
     * Send requests to the collector.
     * @param requests to send.
     * @return results of the sending operation.
     */
    @NonNull List<RequestResult> sendRequests(@NonNull List<Request> requests);

    /**
     * @return http method used to send requests to the collector.
     */
    @NonNull HttpMethod getHttpMethod();

    /**
     * @return URI of the collector.
     */
    @NonNull Uri getUri();
}
