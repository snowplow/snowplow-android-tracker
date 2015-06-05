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

package com.snowplowanalytics.snowplow.tracker;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Payload interface
 * The Payload is used to store all the parameters and configurations that are used
 * to send data via the HTTP GET/POST request.
 * @version 0.5.0
 * @author Jonathan Almeida
 */
public interface Payload {

    /**
     * Add a basic parameter.
     * @param key The parameter key
     * @param value The parameter value as a String
     */
    public void add(String key, String value);

    /**
     * Add a basic parameter.
     * @param key The parameter key
     * @param value The parameter value
     */
    public void add(String key, Object value);

    /**
     * Add all the mappings from the specified map. The effect is the equivalent to that of calling
     * add(String key, Object value) for each mapping for each key.
     * @param map Mappings to be stored in this map
     */
    public void addMap(Map<String, Object> map);

    /**
     * Add a map to the Payload with a key dependent on the base 64 encoding option you choose using the
     * two keys provided.
     * @param map Mapping to be stored
     * @param base64_encoded The option you choose to encode the data
     * @param type_encoded The key that would be set if the encoding option was set to true
     * @param type_no_encoded They key that would be set if the encoding option was set to false
     */
    public void addMap(Map map, Boolean base64_encoded, String type_encoded, String type_no_encoded);

    /**
     * Returns the Payload as a HashMap.
     * @return A HashMap
     */
    public Map getMap();

    /**
     * Returns the Payload using Jackson JSON to return a JsonNode.
     * @return A JsonNode
     */
    public JsonNode getNode();

    /**
     * Returns the Payload as a string. This is essentially the toString from the ObjectNode used
     * to store the Payload.
     * @return A string value of the Payload.
     */
    public String toString();

    /**
     * Returns the byte size of a payload.
     * @return A long representing the byte size of the payload.
     */
    public long getByteSize();
}
