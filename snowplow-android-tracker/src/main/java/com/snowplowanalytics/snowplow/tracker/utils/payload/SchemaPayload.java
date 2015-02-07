/*
 * Copyright (c) 2015 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */

package com.snowplowanalytics.snowplow.tracker.utils.payload;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.snowplowanalytics.snowplow.tracker.Payload;
import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.utils.Util;
import com.snowplowanalytics.snowplow.tracker.utils.Preconditions;
import com.snowplowanalytics.snowplow.tracker.utils.Logger;

public class SchemaPayload implements Payload {

    private final String TAG = SchemaPayload.class.getSimpleName();
    private final ObjectMapper objectMapper = Util.getObjectMapper();
    private ObjectNode objectNode = objectMapper.createObjectNode();

    public SchemaPayload() {}

    public SchemaPayload(Payload payload) {
        ObjectNode data;

        if (payload.getClass() == TrackerPayload.class) {
            Logger.ifDebug(TAG, "Payload class is a TrackerPayload instance.");
            Logger.ifDebug(TAG, "Trying getNode()");
            data = (ObjectNode) payload.getNode();
        } else {
            Logger.ifDebug(TAG, "Converting Payload map to ObjectNode.");
            data = objectMapper.valueToTree(payload.getMap());
        }
        objectNode.set(Parameters.DATA, data);
    }

    public SchemaPayload setSchema(String schema) {
        Preconditions.checkNotNull(schema, "schema cannot be null");
        Preconditions.checkArgument(!schema.isEmpty(), "schema cannot be empty.");

        Logger.ifDebug(TAG, "Setting schema: %s", schema);
        objectNode.put(Parameters.SCHEMA, schema);
        return this;
    }

    public SchemaPayload setData(Payload data) {
        try {
            objectNode.putPOJO(Parameters.DATA, objectMapper.writeValueAsString(data.getMap()));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return this;
    }

    public SchemaPayload setData(Object data) {
        try {
            objectNode.putPOJO(Parameters.DATA, objectMapper.writeValueAsString(data));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return this;
    }

    @Deprecated
    @Override
    public void add(String key, String value) {
        /*
         * We intentionally do nothing because we do not want our SchemaPayload
         * to do anything except accept a 'data' and 'schema'
         */
        Logger.ifDebug(TAG, "add(String, String) method called: Doing nothing.");
    }

    @Deprecated
    @Override
    public void add(String key, Object value) {
        /*
         * We intentionally do nothing because we do not want our SchemaPayload
         * to do anything except accept a 'data' and 'schema'
         */
        Logger.ifDebug(TAG, "add(String, Object) method called: Doing nothing.");
    }

    @Deprecated
    @Override
    public void addMap(Map<String, Object> map) {
        /*
         * We intentionally do nothing because we do not want our SchemaPayload
         * to do anything except accept a 'data' and 'schema'
         */
        Logger.ifDebug(TAG, "addMap(Map<String, Object>) method called: Doing nothing.");
    }

    @Deprecated
    @Override
    public void addMap(Map map, Boolean base64_encoded, String type_encoded,
                       String type_no_encoded) {
        /*
         * We intentionally do nothing because we do not want our SchemaPayload
         * to do anything except accept a 'data' and 'schema'
         */
        Logger.ifDebug(TAG, "addMap(Map, Boolean, String, String) method called: Doing nothing.");
    }

    public Map<String, Object> getMap() {
        HashMap<String, Object> map = new HashMap<String, Object>();
        try {
            map = objectMapper.readValue(objectNode.toString(),
                    new TypeReference<HashMap>(){});
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    public JsonNode getNode() {
        return objectNode;
    }

    public String toString() {
        return objectNode.toString();
    }
}
