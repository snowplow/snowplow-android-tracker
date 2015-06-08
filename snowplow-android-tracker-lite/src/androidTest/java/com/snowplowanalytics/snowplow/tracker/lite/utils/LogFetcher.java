package com.snowplowanalytics.snowplow.tracker.lite.utils;

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
    private static final String imposter =
            "{\n"+
            "  \"port\": "+ port +",\n"+
            "  \"protocol\": \"http\",\n"+
            "  \"stubs\": [{\n"+
            "      \"responses\": [{\n"+
            "        \"is\": {\n"+
            "          \"statusCode\": 200,\n"+
            "          \"body\": \"Successful Snowplow POST Request\"\n"+
            "        }\n"+
            "      }],\n"+
            "      \"predicates\": [{\n"+
            "        \"contains\": {\n"+
            "          \"path\": \"/com.snowplowanalytics.snowplow/tp2\",\n"+
            "          \"method\": \"POST\",\n"+
            "          \"query\": {},\n"+
            "          \"headers\": {\n"+
            "            \"Content-Type\": \"application/json; charset=utf-8\"\n"+
            "          }\n"+
            "        },\n"+
            "        \"exists\": {\n"+
            "          \"body\": true\n"+
            "        }\n"+
            "      }]\n"+
            "    },\n"+
            "    {\n"+
            "      \"responses\": [{\n"+
            "        \"is\": {\n"+
            "          \"statusCode\": 200,\n"+
            "          \"body\": \"Successful Snowplow GET Request\"\n"+
            "        }\n"+
            "      }],\n"+
            "      \"predicates\": [{\n"+
            "        \"contains\": {\n"+
            "          \"path\": \"/i\",\n"+
            "          \"method\": \"GET\",\n"+
            "          \"body\": \"\"\n"+
            "        },\n"+
            "        \"exists\": {\n"+
            "          \"query\": {\n"+
            "            \"e\": true,\n"+
            "            \"dtm\": true,\n"+
            "            \"p\": true,\n"+
            "            \"eid\": true,\n"+
            "            \"tv\": true\n"+
            "          }\n"+
            "        }\n"+
            "      }]\n"+
            "    },\n"+
            "    {\n"+
            "      \"responses\": [{\n"+
            "        \"is\": {\n"+
            "          \"statusCode\": 404\n"+
            "        }\n"+
            "      }]\n"+
            "    }\n"+
            "  ]\n"+
            "}\n";

    public static void createImposter() {
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
