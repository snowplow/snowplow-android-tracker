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

package com.snowplowanalytics.snowplowtrackerdemo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;

import android.util.Log;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.Button;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.text.method.ScrollingMovementMethod;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.util.Consumer;

import com.snowplowanalytics.snowplow.Snowplow;
import com.snowplowanalytics.snowplow.configuration.EmitterConfiguration;
import com.snowplowanalytics.snowplow.configuration.GdprConfiguration;
import com.snowplowanalytics.snowplow.configuration.GlobalContextsConfiguration;
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration;
import com.snowplowanalytics.snowplow.configuration.RemoteConfiguration;
import com.snowplowanalytics.snowplow.configuration.SessionConfiguration;
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration;
import com.snowplowanalytics.snowplow.controller.EmitterController;
import com.snowplowanalytics.snowplow.controller.SessionController;
import com.snowplowanalytics.snowplow.controller.TrackerController;
import com.snowplowanalytics.snowplow.emitter.BufferOption;
import com.snowplowanalytics.snowplow.globalcontexts.GlobalContext;
import com.snowplowanalytics.snowplow.tracker.DevicePlatform;
import com.snowplowanalytics.snowplow.tracker.LoggerDelegate;
import com.snowplowanalytics.snowplow.network.HttpMethod;
import com.snowplowanalytics.snowplow.network.RequestCallback;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.LogLevel;
import com.snowplowanalytics.core.utils.Util;
import com.snowplowanalytics.snowplow.util.Basis;
import com.snowplowanalytics.snowplow.util.TimeMeasure;
import com.snowplowanalytics.snowplowtrackerdemo.utils.DemoUtils;
import com.snowplowanalytics.snowplowtrackerdemo.utils.TrackerEvents;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.snowplowanalytics.core.utils.Util.addToMap;

/**
 * Classic Demo Activity.
 */
public class Demo extends Activity implements LoggerDelegate {

    // Example schema for global contexts
    final String SCHEMA_IDENTIFY = "iglu:com.snowplowanalytics.snowplow/identify/jsonschema/1-0-0";

    private Button _startButton, _tabButton, _loadWebViewButton;
    private EditText _uriField, _webViewUriField;
    private RadioGroup _type, _remoteConfig, _collection;
    private RadioButton _radioGet, _radioRemoteConfig;
    private TextView _logOutput, _eventsCreated, _eventsSent, _emitterOnline, _emitterStatus,
            _databaseSize, _sessionIndex;
    private WebView _webView;

    private int eventsCreated = 0;
    private int eventsSent = 0;

    private Consumer<Boolean> callbackIsPermissionGranted;
    private final static int APP_PERMISSION_REQUEST_LOCATION = 1;

    private static final String namespace = "SnowplowAndroidTrackerDemo";
    private static final String appId = "DemoID";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        _startButton   = (Button)findViewById(R.id.btn_lite_start);
        _tabButton     = (Button)findViewById(R.id.btn_lite_tab);
        _uriField      = (EditText)findViewById(R.id.uri_field);
        _type          = (RadioGroup)findViewById(R.id.radio_send_type);
        _remoteConfig  = (RadioGroup)findViewById(R.id.radio_config_type);
        _collection    = (RadioGroup)findViewById(R.id.radio_data_collection);
        _radioGet      = (RadioButton)findViewById(R.id.radio_get);
        _radioRemoteConfig = (RadioButton)findViewById(R.id.radio_remote_config);
        _logOutput     = (TextView)findViewById(R.id.log_output);
        _eventsCreated = (TextView)findViewById(R.id.created_events);
        _eventsSent    = (TextView)findViewById(R.id.sent_events);
        _emitterOnline = (TextView)findViewById(R.id.online_status);
        _emitterStatus = (TextView)findViewById(R.id.emitter_status);
        _databaseSize  = (TextView)findViewById(R.id.database_size);
        _sessionIndex  = (TextView)findViewById(R.id.session_index);
        _webViewUriField = (EditText)findViewById(R.id.web_view_uri_field);
        _webView       = (WebView) findViewById(R.id.web_view);
        _loadWebViewButton = (Button)findViewById(R.id.btn_load_webview);

        _logOutput.setMovementMethod(new ScrollingMovementMethod());
        _logOutput.setText("");

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String uri = sharedPreferences.getString("uri", "");
        _uriField.setText(uri);
        String webViewUri = sharedPreferences.getString("webViewUri", "");
        _webViewUriField.setText(webViewUri);

        _webView.getSettings().setJavaScriptEnabled(true);

        // Setup Listeners
        setupTrackerListener();
        setupTabListener();
        setupWebViewListener();
    }

    @Override
    protected void onDestroy() {
        DemoUtils.resetExecutor();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        TrackerController tracker = Snowplow.getDefaultTracker();
        if (tracker == null) return;
        SessionController session = tracker.getSession();
        if (session == null) return;
        session.resume();
    }

    /**
     * Setups listener for tabs.
     */
    private void setupTabListener() {
        _tabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TrackerController trackerController = Snowplow.getDefaultTracker();
                if (trackerController == null) return;
                SessionController sessionController = trackerController.getSession();
                if (sessionController != null) {
                    sessionController.pause();
                }
                String url = "https://snowplow.io/";
                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                CustomTabsIntent customTabsIntent = builder.build();
                customTabsIntent.launchUrl(Demo.this, Uri.parse(url));
            }
        });
    }

    /**
     * Builds and sets up the Tracker listener for the demo.
     */
    private void setupTrackerListener() {
        makePollingUpdater(getApplicationContext());

        _collection.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final RadioGroup radioGroup, final int i) {
                TrackerController tracker = Snowplow.getDefaultTracker();
                if (i == R.id.radio_data_on) {
                    tracker.resume();
                } else if (i == R.id.radio_data_off) {
                    tracker.pause();
                }
            }
        });

        _startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT < 24) {
                    setupTracker(aBoolean -> trackEvents());
                }
                requestPermissions(isGranted -> {
                    if (isGranted) {
                        setupTracker(callbackTrackerReady -> trackEvents());
                        trackEvents();
                    }
                });
            }
        });
    }

    private void setupWebViewListener() {
        _loadWebViewButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String uri = _webViewUriField.getText().toString();
                if (uri.isEmpty()) {
                    updateLogger("Web view URI is empty!");
                } else {
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
                    editor.putString("webViewUri", uri).apply();

                    _webView.loadUrl(uri);
                }
            }
        });
    }

    // Configuration

    private void setupTracker(@NonNull Consumer<Boolean> callbackTrackerReady) {
        boolean isRemoteConfig = _remoteConfig.getCheckedRadioButtonId() == _radioRemoteConfig.getId();
        if (isRemoteConfig) {
            setupWithRemoteConfig(callbackTrackerReady);
        } else {
            setupWithLocalConfig();
        }
    }

    private boolean setupWithRemoteConfig(@NonNull Consumer<Boolean> callbackTrackerReady) {
        String uri = _uriField.getText().toString();
        if (uri.isEmpty()) {
            updateLogger("URI field empty!");
            return false;
        }
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
        editor.putString("uri", uri).apply();
        HttpMethod method = _type.getCheckedRadioButtonId() ==
                _radioGet.getId() ? HttpMethod.GET : HttpMethod.POST;

        RemoteConfiguration remoteConfig = new RemoteConfiguration(uri, method);
        Snowplow.setup(getApplicationContext(), remoteConfig, null, configurationPair -> {
            List<String> namespaces = configurationPair.first;
            updateLogger("Created namespaces: " + namespaces);
            switch (configurationPair.second) {
                case CACHED:
                    updateLogger("Configuration retrieved from cache");
                case FETCHED:
                    updateLogger("Configuration fetched from remote endpoint");
            }
            Snowplow.getDefaultTracker().getEmitter().setRequestCallback(getRequestCallback());
            callbackTrackerReady.accept(true);
        });
        return true;
    }

    private boolean setupWithLocalConfig() {
        String uri = _uriField.getText().toString();
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
        editor.putString("uri", uri).apply();
        if (uri.isEmpty()) {
            updateLogger("URI field empty!");
            return false;
        }
        HttpMethod method = _type.getCheckedRadioButtonId() ==
                _radioGet.getId() ? HttpMethod.GET : HttpMethod.POST;

        NetworkConfiguration networkConfiguration = new NetworkConfiguration(uri, method);
        EmitterConfiguration emitterConfiguration = new EmitterConfiguration()
                .requestCallback(getRequestCallback())
                .bufferOption(BufferOption.DefaultGroup)
                .threadPoolSize(20)
                .emitRange(500)
                .byteLimitPost(52000);
        TrackerConfiguration trackerConfiguration = new TrackerConfiguration(appId)
                .logLevel(LogLevel.VERBOSE)
                .loggerDelegate(this)
                .base64encoding(false)
                .devicePlatform(DevicePlatform.Mobile)
                .sessionContext(true)
                .platformContext(true)
                .applicationContext(true)
                .geoLocationContext(true)
                .lifecycleAutotracking(true)
                .screenViewAutotracking(true)
                .screenContext(true)
                .exceptionAutotracking(true)
                .installAutotracking(true)
                .diagnosticAutotracking(true);
        SessionConfiguration sessionConfiguration = new SessionConfiguration(
                new TimeMeasure(6, TimeUnit.SECONDS),
                new TimeMeasure(30, TimeUnit.SECONDS)
        )
                .onSessionUpdate(state -> updateLogger(
                        "Session: " + state.getSessionId()
                                + "\r\nprevious: " + state.getPreviousSessionId()
                                + "\r\neventId: " + state.getFirstEventId()
                                + "\r\nindex: " + state.getSessionIndex()
                                + "\r\nuserId: " + state.getUserId()
                ));
        GdprConfiguration gdprConfiguration = new GdprConfiguration(
                Basis.CONSENT,
                "someId",
                "0.1.0",
                "this is a demo document description"
        );
        GlobalContextsConfiguration gcConfiguration = new GlobalContextsConfiguration(null);
        Map<String, Object> pairs = new HashMap<>();
        addToMap("id", "snowplow", pairs);
        addToMap("email", "info@snowplow.io", pairs);
        gcConfiguration.add("ruleSetExampleTag", new GlobalContext(Collections.singletonList(new SelfDescribingJson(SCHEMA_IDENTIFY, pairs))));

        Snowplow.createTracker(getApplicationContext(),
                namespace,
                networkConfiguration,
                trackerConfiguration,
                emitterConfiguration,
                sessionConfiguration,
                gdprConfiguration,
                gcConfiguration
        );
        Snowplow.subscribeToWebViewEvents(_webView);
        return true;
    }

    private void trackEvents() {
        TrackerController tracker = Snowplow.getDefaultTracker();
        if (tracker == null) {
            updateLogger("TrackerController not ready!");
            return;
        }
        TrackerEvents.trackAll(tracker);
        eventsCreated += 11;
        final String made = "Made: " + eventsCreated;
        runOnUiThread(() -> _eventsCreated.setText(made));
    }

    private void requestPermissions(@NonNull Consumer<Boolean> callbackIsGranted) {
        callbackIsPermissionGranted = callbackIsGranted;
        int permissionState = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionState == PackageManager.PERMISSION_GRANTED) {
            callbackIsGranted.accept(true);
            return;
        }
        final String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
        ActivityCompat.requestPermissions(this, permissions, APP_PERMISSION_REQUEST_LOCATION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (Build.VERSION.SDK_INT < 24) {
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == APP_PERMISSION_REQUEST_LOCATION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            callbackIsPermissionGranted.accept(true);
            return;
        }
        callbackIsPermissionGranted.accept(false);
    }

    /**
     * Updates the logger with a message.
     *
     * @param message the message to add to the log.
     */
    private void updateLogger(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _logOutput.append(message + "\n");

            }
        });
    }

    /**
     * Updates the events sent counter.
     *
     * @param count the amount of successful events
     */
    private void updateEventsSent(final int count) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                eventsSent += count;
                final String sent = "Sent: " + eventsSent;
                _eventsSent.setText(sent);
            }
        });
    }

    /**
     * Updates the various UI elements based on information
     * about the Tracker and Emitter.
     *
     * @param isOnline is the device online
     * @param isRunning is the emitter running
     * @param dbSize the database event size
     */
    private void updateEmitterStats(final boolean isOnline, final boolean isRunning, final long dbSize,
                                    final int sessionIndex) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String online = isOnline ? "Online: yes" : "Online: no";
                _emitterOnline.setText(online);
                String status = isRunning ? "Running: yes" : "Running: no";
                _emitterStatus.setText(status);
                final String dbSizeStr = "DB Size: " + dbSize;
                _databaseSize.setText(dbSizeStr);
                final String sessionIndexStr = "Session #: " + sessionIndex;
                _sessionIndex.setText(sessionIndexStr);

                if (isRunning) {
                    String startButtonText = _startButton.getText().toString();
                    switch (startButtonText) {
                        case "Start!":
                        case "Running  .":
                            _startButton.setText(R.string.running_1);
                            break;
                        case "Running.  ":
                            _startButton.setText(R.string.running_2);
                            break;
                        default:
                            _startButton.setText(R.string.running_3);
                            break;
                    }
                } else {
                    _startButton.setText(R.string.start);
                }
            }
        });
    }

    /**
     * Starts a polling updater which will fetch
     * and update the UI.
     *
     * @param context the activity context
     */
    private void makePollingUpdater(final Context context) {
        DemoUtils.scheduleRepeating(new Runnable() {
            @Override
            public void run() {
                boolean isOnline = Util.isOnline(context);
                TrackerController tracker = Snowplow.getDefaultTracker();
                if (tracker == null) return;
                EmitterController e = tracker.getEmitter();
                boolean isRunning = e.isSending();
                long dbSize = e.getDbCount();
                SessionController session = tracker.getSession();
                int sessionIndex = session == null ? -1 : session.getSessionIndex();
                updateEmitterStats(isOnline, isRunning, dbSize, sessionIndex);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Returns the Emitter Request Callback.
     */
    private RequestCallback getRequestCallback() {
        return new RequestCallback() {
            @Override
            public void onSuccess(int successCount) {
                updateLogger("Emitter Send Success:\n " +
                        "- Events sent: " + successCount + "\n");
                updateEventsSent(successCount);
            }
            @Override
            public void onFailure(int successCount, int failureCount) {
                updateLogger("Emitter Send Failure:\n " +
                        "- Events sent: " + successCount + "\n " +
                        "- Events failed: " + failureCount + "\n");
                updateEventsSent(successCount);
            }
        };
    }

    /// - Implements LoggerDelegate

    @Override
    public void error(@NonNull String tag, @NonNull String msg) {
        Log.e("[" + tag + "]", msg);
    }

    @Override
    public void debug(@NonNull String tag, @NonNull String msg) {
        Log.d("[" + tag + "]", msg);
    }

    @Override
    public void verbose(@NonNull String tag, @NonNull String msg) {
        Log.v("[" + tag + "]", msg);
    }
}
