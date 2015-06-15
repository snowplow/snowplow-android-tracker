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

package com.snowplowanalytics.snowplow.tracker.rx.utils;

import android.util.Log;

import java.io.IOException;
import java.util.LinkedList;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;

import org.json.JSONObject;
import org.json.JSONArray;

/**
 * A helper class to fetch logs back
 * from mountebank.
 */
public class LogFetcher {

    private static final String TAG = LogFetcher.class.getSimpleName();
    private static final OkHttpClient client = new OkHttpClient();
    private static final String port = "4545";
    private static final String mbUrl = "http://10.0.2.2:2525/imposters/";

    public static void createImposter(String imposter) {
        MediaType JSON = MediaType.parse("application/json");
        RequestBody reqBody = RequestBody.create(JSON, imposter);
        Request req = new Request.Builder()
                .url(mbUrl)
                .post(reqBody)
                .build();
        try {
            client.newCall(req).execute();
        } catch (IOException e) {
            Log.d(TAG, "Mountebank could not be reached: " + e.toString());
        }
    }

    public static void deleteImposter() {
        Request req = new Request.Builder()
                .url(mbUrl+port)
                .delete()
                .build();
        try {
            client.newCall(req).execute();
        } catch (IOException e) {
            Log.d(TAG, "Mountebank could not be reached: " + e.toString());
        }
    }

    public static LinkedList<JSONObject> getMountebankPostRequests() {
        try {
            // Pulls out all valid matches
            JSONArray requests = getMountebankLogs().getJSONObject(0).getJSONArray("matches");

            // Create a LinkedList to store the wanted results
            LinkedList<JSONObject> logRange = new LinkedList<>();

            // Store the events
            for (int i = 0; i < requests.length(); i++) {
                logRange.add(requests.getJSONObject(i));
            }

            return logRange;
        } catch (Exception e) {
            return new LinkedList<>();
        }
    }

    public static LinkedList<JSONObject> getMountebankGetRequests() {
        try {
            // Pulls out all valid matches
            JSONArray requests = getMountebankLogs().getJSONObject(1).getJSONArray("matches");

            // Create a LinkedList to store the wanted results
            LinkedList<JSONObject> logRange = new LinkedList<>();

            // Store the events
            for (int i = 0; i < requests.length(); i++) {
                logRange.add(requests.getJSONObject(i));
            }

            return logRange;
        } catch (Exception e) {
            return new LinkedList<>();
        }
    }

    public static LinkedList<JSONObject> getMountebankFailedRequests() {
        try {
            // Pulls out all valid matches
            JSONArray requests = getMountebankLogs().getJSONObject(2).getJSONArray("matches");

            // Create a LinkedList to store the wanted results
            LinkedList<JSONObject> logRange = new LinkedList<>();

            // Store the events
            for (int i = 0; i < requests.length(); i++) {
                logRange.add(requests.getJSONObject(i));
            }

            return logRange;
        } catch (Exception e) {
            return new LinkedList<>();
        }
    }

    private static JSONArray getMountebankLogs() throws Exception {

        // Fetch the logs from mountebank
        Request req = new Request.Builder()
                .url(mbUrl+port)
                .get()
                .build();
        String results = client.newCall(req).execute().body().string();

        // Process the returned string into a JSONArray
        return new JSONObject(results).getJSONArray("stubs");
    }
}
