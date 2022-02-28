/*
 * Copyright (c) 2015-2022 Snowplow Analytics Ltd. All rights reserved.
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

package com.snowplowanalytics.snowplow.payload;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

/**
 * Payload interface
 * The Payload is used to store all the parameters and configurations that are used
 * to send data via the HTTP GET/POST request.
 *
 * @version 0.5.0
 * @author Jonathan Almeida
 */
public interface Payload {

    /**
     * Add a basic parameter.
     *
     * @param key The parameter key
     * @param value The parameter value as a String
     */
    void add(@NonNull String key, @Nullable String value);

    /**
     * Add a basic parameter.
     *
     * @param key The parameter key
     * @param value The parameter value
     */
    void add(@NonNull String key, @Nullable Object value);

    /**
     * Add all the mappings from the specified map. The effect is the equivalent to that of calling
     * add(String key, Object value) for each mapping for each key.
     *
     * @param map Mappings to be stored in this map
     */
    void addMap(@NonNull Map<String, Object> map);

    /**
     * Add a map to the Payload with a key dependent on the base 64 encoding option you choose using the
     * two keys provided.
     *  @param map Mapping to be stored
     * @param base64_encoded The option you choose to encode the data
     * @param type_encoded The key that would be set if the encoding option was set to true
     * @param type_no_encoded They key that would be set if the encoding option was set to false
     */
    void addMap(@NonNull Map map, @NonNull Boolean base64_encoded, @Nullable String type_encoded, @Nullable String type_no_encoded);

    /**
     * Returns the Payload as a HashMap.
     *
     * @return A HashMap
     */
    @NonNull
    Map getMap();

    /**
     * Returns the Payload as a string. This is essentially the toString from the ObjectNode used
     * to store the Payload.
     *
     * @return A string value of the Payload.
     */
    @NonNull
    String toString();

    /**
     * Returns the byte size of a payload.
     *
     * @return A long representing the byte size of the payload.
     */
    long getByteSize();
}
