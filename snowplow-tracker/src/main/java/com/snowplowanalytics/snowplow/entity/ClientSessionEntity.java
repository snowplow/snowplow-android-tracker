package com.snowplowanalytics.snowplow.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.internal.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;

import java.util.Map;

public class ClientSessionEntity extends SelfDescribingJson  {

    private final Map<String, Object> values;

    public ClientSessionEntity(@NonNull Map<String, Object> values) {
        super(TrackerConstants.SESSION_SCHEMA, values);

        this.values = values;
    }

    @Nullable
    public String getUserId() {
        return (String) values.get(Parameters.SESSION_USER_ID);
    }
}
