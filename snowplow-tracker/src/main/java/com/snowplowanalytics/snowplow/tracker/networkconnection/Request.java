package com.snowplowanalytics.snowplow.tracker.networkconnection;

import android.support.annotation.NonNull;

import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.payload.Payload;
import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Request class that contains the payloads to send
 * to the collector.
 */
public class Request {
    public final Payload payload;
    public final List<Long> emitterEventIds;
    public final boolean oversize;
    public final String customUserAgent;

    /**
     * Create a request object.
     * @param payload to send to the collector.
     * @param id as reference of the event to send.
     */
    public Request(Payload payload, long id) {
        this(payload, id, false);
    }

    /**
     * Create a request object.
     * @param payload to send to the collector.
     * @param id as reference of the event to send.
     * @param oversize indicates if the payload exceeded the maximum size allowed.
     */
    public Request(Payload payload, long id, boolean oversize) {
        List<Long> ids = new ArrayList<>();
        ids.add(id);
        emitterEventIds = ids;
        this.payload = payload;
        this.oversize = oversize;
        customUserAgent = getUserAgent(payload);
    }

    /**
     * Create a request object.
     * @param payloads to send to the collector as a payload bundle.
     * @param emitterEventIds as reference of the events to send.
     * @param oversize indicates if the payload bundle exceeded the maximum size allowed.
     */
    public Request(List<Payload> payloads, List<Long> emitterEventIds, boolean oversize) {
        String tempUserAgent = null;
        ArrayList<Map> payloadData = new ArrayList<>();
        for (Payload payload : payloads) {
            payloadData.add(payload.getMap());
            tempUserAgent = getUserAgent(payload);
        }
        this.payload = new SelfDescribingJson(TrackerConstants.SCHEMA_PAYLOAD_DATA, payloadData);
        this.emitterEventIds = emitterEventIds;
        this.oversize = oversize;
        customUserAgent = tempUserAgent;
    }

    /**
     * Get the User-Agent string for the request's header.
     *
     * @param payload The payload where to get the `ua` parameter.
     * @return User-Agent string from subject settings or the default one.
     */
    private String getUserAgent(@NonNull Payload payload) {
        HashMap hashMap = (HashMap) payload.getMap();
        return (String) hashMap.get(Parameters.USERAGENT);
    }
}
