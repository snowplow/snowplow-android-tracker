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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.snowplowanalytics.snowplow.tracker.Payload;
import com.snowplowanalytics.snowplow.tracker.utils.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TrackerPayload implements Payload {

    private final ObjectMapper objectMapper = Util.defaultMapper();
    private final Logger logger = LoggerFactory.getLogger(TrackerPayload.class);
    private ObjectNode objectNode = objectMapper.createObjectNode();

    @Override
    public void add(String key, String value) {
        if (value == null || value.isEmpty()) {
            logger.debug("kv-value is empty. Returning out without adding key..");
            return;
        }

        logger.debug("Adding new key: {} with value: {}", key, value);
        objectNode.put(key, value);
    }

    @Override
    public void add(String key, Object value) {
        if (value == null) {
            logger.debug("kv-value is empty. Returning out without adding key..");
            return;
        }

        logger.debug("Adding new key: {} with object value: {}", key, value);
        try {
            objectNode.putPOJO(key, objectMapper.writeValueAsString(value));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addMap(Map<String, Object> map) {
        // Return if we don't have a map
        if (map == null) {
            logger.debug("Map passed in is null. Returning without adding map..");
            return;
        }

        Set<String> keys = map.keySet();
        for(String key : keys) {
            add(key, map.get(key));
        }
    }

    @Override
    public void addMap(Map map, Boolean base64_encoded, String type_encoded, String type_no_encoded) {
        // Return if we don't have a map
        if (map == null) {
            logger.debug("Map passed in is null. Returning nothing..");
            return;
        }

        String mapString;
        try {
            mapString = objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return; // Return because we can't continue
        }

        if (base64_encoded) { // base64 encoded data
            objectNode.put(type_encoded, Util.base64Encode(mapString));
        } else { // add it as a child node
            add(type_no_encoded, mapString);
        }
    }

    public JsonNode getNode() {
        return objectNode;
    }

    @Override
    public Map getMap() {
        HashMap<String, String> map = new HashMap<String, String>();
        try {
            logger.debug("Attempting to create a Map structure from ObjectNode.");
            map = objectMapper.readValue(objectNode.toString(), new TypeReference<Map>(){});
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    @Override
    public String toString() {
        return objectNode.toString();
    }
}
