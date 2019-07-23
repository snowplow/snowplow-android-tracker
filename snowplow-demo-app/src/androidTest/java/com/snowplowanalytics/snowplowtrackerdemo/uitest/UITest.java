package com.snowplowanalytics.snowplowtrackerdemo.uitest;

import android.view.WindowManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.snowplowanalytics.snowplowtrackerdemo.Demo;
import com.snowplowanalytics.snowplowtrackerdemo.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants.SCHEMA_APPLICATION;
import static com.snowplowanalytics.snowplowtrackerdemo.BuildConfig.MICRO_ENDPOINT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Ignore("Automation isn't completed yet, see #328 at github repo.")
@RunWith(AndroidJUnit4.class)
@LargeTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UITest {

    private String micro_uri;
    private String micro_all_url;
    private String micro_good_url;
    private String micro_bad_url;
    private String micro_reset_url;

    private OkHttpClient client;
    private Request resetRequest;

    private JsonParser parser = new JsonParser();

    @Rule
    public ActivityTestRule<Demo> activityRule
            = new ActivityTestRule<>(Demo.class);

    @Before
    public void beforeAll() {

        // unlock screen
        final Demo activity = activityRule.getActivity();
        Runnable wakeUpDevice = new Runnable() {
            public void run() {
                activity.getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        };
        activity.runOnUiThread(wakeUpDevice);

        // set micro endpoints
        micro_uri = MICRO_ENDPOINT;
        micro_all_url = "https://".concat(MICRO_ENDPOINT.concat("/micro/all"));
        micro_good_url = "https://".concat(MICRO_ENDPOINT.concat("/micro/good"));
        micro_bad_url = "https://".concat(MICRO_ENDPOINT.concat("/micro/bad"));
        micro_reset_url = "https://".concat(MICRO_ENDPOINT.concat("/micro/reset"));

        // init okhttp client
        client = new OkHttpClient();

        resetRequest = new Request.Builder()
                .url(micro_reset_url)
                .build();
    }

    @Test
    public void t1_sendDemoEvents() throws InterruptedException, IOException, JSONException {
        client.newCall(resetRequest).execute();

        Espresso.closeSoftKeyboard();
        onView(withId(R.id.emitter_uri_field)).perform(replaceText(micro_uri), closeSoftKeyboard());
        onView(withId(R.id.emitter_uri_field)).check(matches(withText(micro_uri)));
        onView(withId(R.id.btn_lite_start)).perform(click());

        // TODO this is not best practice for idling resources
        Thread.sleep(20000);

        Request request = new Request.Builder()
                .url(micro_all_url)
                .build();

        Response response = client.newCall(request).execute();
        String body = response.body().string();
        JSONObject jsonObject = new JSONObject(body);
        assertEquals(jsonObject.getString("total"), jsonObject.getString("good"));
        assertEquals(jsonObject.getString("bad"), "0");
    }

    @Test
    public void t2_nativeContexts() throws IOException {
        Request request = new Request.Builder()
                .url(micro_good_url)
                .build();

        Response response = client.newCall(request).execute();
        String body = response.body().string();

        JsonArray jsonArray = parser.parse(body).getAsJsonArray();

        for (int i = 0; i < jsonArray.size(); ++i) {

            String co = jsonArray.get(i).getAsJsonObject().get("event").getAsJsonObject()
                        .get("parameters").getAsJsonObject().get("co").getAsString();

            JsonArray contexts = jsonArray.get(i).getAsJsonObject().get("contexts").getAsJsonArray();
            boolean duplicateCheck = checkIfContextsAreDuplicated(contexts);
            assertTrue(duplicateCheck);

            // delete all occurrences of escape char, '\', from the string before parsing
            co = co.replaceAll("\\\\", "");

            JsonArray co_data = parser.parse(co).getAsJsonObject().get("data").getAsJsonArray();

            for (int j = 0; j < co_data.size(); ++j) {
                JsonObject context = co_data.get(j).getAsJsonObject();
                String schema = context.get("schema").getAsString();
                JsonObject data = context.get("data").getAsJsonObject();
                if (schema.equals(SCHEMA_APPLICATION)) {
                    String build = data.get("build").getAsString();
                    String version = data.get("version").getAsString();
                    assertEquals(build, "3");
                    assertEquals(version, "0.3.0");
                }
            }
        }
    }

    public boolean checkIfContextsAreDuplicated(JsonArray contexts) {
        ArrayList<String> schemas = new ArrayList<>();
        for (int i = 0; i < contexts.size(); ++i) {
            String schema = contexts.get(i).getAsString();
            schemas.add(schema);
        }
        Set<String> schemaSet = new HashSet<>(schemas);
        return schemaSet.size() == contexts.size();
    }
}
