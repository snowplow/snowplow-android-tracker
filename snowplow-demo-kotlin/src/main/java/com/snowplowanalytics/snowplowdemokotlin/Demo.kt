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
package com.snowplowanalytics.snowplowdemokotlin

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.preference.PreferenceManager
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.widget.*
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.core.util.Pair

import com.snowplowanalytics.snowplow.configuration.ConfigurationState
import com.snowplowanalytics.core.utils.Util
import com.snowplowanalytics.snowplow.Snowplow.createTracker
import com.snowplowanalytics.snowplow.Snowplow.defaultTracker
import com.snowplowanalytics.snowplow.Snowplow.setup
import com.snowplowanalytics.snowplow.Snowplow.subscribeToWebViewEvents
import com.snowplowanalytics.snowplow.configuration.*
import com.snowplowanalytics.snowplow.emitter.BufferOption
import com.snowplowanalytics.snowplow.globalcontexts.GlobalContext
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.network.RequestCallback
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow.tracker.DevicePlatform
import com.snowplowanalytics.snowplow.tracker.LogLevel
import com.snowplowanalytics.snowplow.tracker.LoggerDelegate
import com.snowplowanalytics.snowplow.tracker.SessionState
import com.snowplowanalytics.snowplow.util.Basis
import com.snowplowanalytics.snowplow.util.TimeMeasure
import com.snowplowanalytics.snowplowdemokotlin.utils.DemoUtils
import com.snowplowanalytics.snowplowdemokotlin.utils.TrackerEvents

import java.util.concurrent.TimeUnit

/**
 * Classic Demo Activity.
 */
class Demo : Activity(), LoggerDelegate {
    // Example schema for global contexts
    private val SCHEMA_IDENTIFY = "iglu:com.snowplowanalytics.snowplow/identify/jsonschema/1-0-0"
    private var _startButton: Button? = null
    private var _tabButton: Button? = null
    private var _loadWebViewButton: Button? = null
    private var _uriField: EditText? = null
    private var _webViewUriField: EditText? = null
    private var _type: RadioGroup? = null
    private var _remoteConfig: RadioGroup? = null
    private var _collection: RadioGroup? = null
    private var _radioGet: RadioButton? = null
    private var _radioRemoteConfig: RadioButton? = null
    private var _logOutput: TextView? = null
    private var _eventsCreated: TextView? = null
    private var _eventsSent: TextView? = null
    private var _emitterOnline: TextView? = null
    private var _emitterStatus: TextView? = null
    private var _databaseSize: TextView? = null
    private var _sessionIndex: TextView? = null
    private var _webView: WebView? = null
    private var eventsCreated = 0
    private var eventsSent = 0
    private var callbackIsPermissionGranted: Consumer<Boolean>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo)

        _startButton = findViewById<View>(R.id.btn_lite_start) as Button
        _tabButton = findViewById<View>(R.id.btn_lite_tab) as Button
        _uriField = findViewById<View>(R.id.uri_field) as EditText
        _type = findViewById<View>(R.id.radio_send_type) as RadioGroup
        _remoteConfig = findViewById<View>(R.id.radio_config_type) as RadioGroup
        _collection = findViewById<View>(R.id.radio_data_collection) as RadioGroup
        _radioGet = findViewById<View>(R.id.radio_get) as RadioButton
        _radioRemoteConfig = findViewById<View>(R.id.radio_remote_config) as RadioButton
        _logOutput = findViewById<View>(R.id.log_output) as TextView
        _eventsCreated = findViewById<View>(R.id.created_events) as TextView
        _eventsSent = findViewById<View>(R.id.sent_events) as TextView
        _emitterOnline = findViewById<View>(R.id.online_status) as TextView
        _emitterStatus = findViewById<View>(R.id.emitter_status) as TextView
        _databaseSize = findViewById<View>(R.id.database_size) as TextView
        _sessionIndex = findViewById<View>(R.id.session_index) as TextView
        _webViewUriField = findViewById<View>(R.id.web_view_uri_field) as EditText
        _webView = findViewById<View>(R.id.web_view) as WebView
        _loadWebViewButton = findViewById<View>(R.id.btn_load_webview) as Button

        _logOutput?.movementMethod = ScrollingMovementMethod()
        _logOutput?.text = ""

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val uri = sharedPreferences.getString("uri", "")
        _uriField?.setText(uri)
        val webViewUri = sharedPreferences.getString("webViewUri", "")
        _webViewUriField?.setText(webViewUri)

        _webView?.settings?.javaScriptEnabled = true

        // Setup Listeners
        setupTrackerListener()
        setupTabListener()
        setupWebViewListener()
    }

    override fun onDestroy() {
        DemoUtils.resetExecutor()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        val tracker = defaultTracker ?: return
        val session = tracker.session ?: return
        session.resume()
    }

    /**
     * Sets up listener for tabs.
     */
    private fun setupTabListener() {
        _tabButton?.setOnClickListener(View.OnClickListener {
            val trackerController = defaultTracker ?: return@OnClickListener
            val sessionController = trackerController.session
            sessionController?.pause()
            val url = "https://snowplow.io/"
            val customTabsIntent = CustomTabsIntent.Builder().build()
            customTabsIntent.launchUrl(this@Demo, Uri.parse(url))
        })
    }

    /**
     * Builds and sets up the Tracker listener for the demo.
     */
    private fun setupTrackerListener() {
        makePollingUpdater(applicationContext)

        _collection!!.setOnCheckedChangeListener { _, i ->
            val tracker = defaultTracker
            if (i == R.id.radio_data_on) {
                tracker!!.resume()
            } else if (i == R.id.radio_data_off) {
                tracker!!.pause()
            }
        }

        _startButton!!.setOnClickListener {
            requestPermissions { isGranted: Boolean ->
                if (isGranted) {
                    setupTracker { trackEvents() }
                    trackEvents()
                }
            }
        }
    }

    private fun setupWebViewListener() {
        _loadWebViewButton!!.setOnClickListener {
            val uri = _webViewUriField!!.text.toString()
            if (uri.isEmpty()) {
                updateLogger("Web view URI is empty!")
            } else {
                val editor =
                    PreferenceManager.getDefaultSharedPreferences(applicationContext).edit()
                editor.putString("webViewUri", uri).apply()
                _webView!!.loadUrl(uri)
            }
        }
    }

    // Configuration
    private fun setupTracker(callbackTrackerReady: Consumer<Boolean>) {
        val isRemoteConfig = _remoteConfig!!.checkedRadioButtonId == _radioRemoteConfig!!.id
        if (isRemoteConfig) {
            setupWithRemoteConfig(callbackTrackerReady)
        } else {
            setupWithLocalConfig()
        }
    }

    private fun setupWithRemoteConfig(callbackTrackerReady: Consumer<Boolean>): Boolean {
        val uri = _uriField!!.text.toString()
        if (uri.isEmpty()) {
            updateLogger("URI field empty!")
            return false
        }
        val editor = PreferenceManager.getDefaultSharedPreferences(applicationContext).edit()
        editor.putString("uri", uri).apply()
        val method = if (_type!!.checkedRadioButtonId ==
            _radioGet!!.id
        ) HttpMethod.GET else HttpMethod.POST

        val remoteConfig = RemoteConfiguration(uri, method)
        setup(
            applicationContext,
            remoteConfig,
            null,
            Consumer<Pair<List<String>, ConfigurationState?>?> { configurationPair: Pair<List<String>, ConfigurationState?>? ->
                val namespaces = configurationPair!!.first
                updateLogger("Created namespaces: $namespaces")
                when (configurationPair.second) {
                    ConfigurationState.CACHED -> {
                        updateLogger("Configuration retrieved from cache")
                        updateLogger("Configuration fetched from remote endpoint")
                    }
                    ConfigurationState.FETCHED -> updateLogger("Configuration fetched from remote endpoint")
                    else -> updateLogger("Configuration was not found")
                }
                defaultTracker!!.emitter.requestCallback = requestCallback
                callbackTrackerReady.accept(true)
            })
        return true
    }

    private fun setupWithLocalConfig(): Boolean {
        val uri = _uriField!!.text.toString()
        val editor = PreferenceManager.getDefaultSharedPreferences(applicationContext).edit()
        editor.putString("uri", uri).apply()
        if (uri.isEmpty()) {
            updateLogger("URI field empty!")
            return false
        }
        val method = if (_type!!.checkedRadioButtonId ==
            _radioGet!!.id
        ) HttpMethod.GET else HttpMethod.POST

        val networkConfiguration = NetworkConfiguration(uri, method)
        val emitterConfiguration = EmitterConfiguration()
            .requestCallback(requestCallback)
            .bufferOption(BufferOption.DefaultGroup)
            .threadPoolSize(20)
            .emitRange(500)
            .byteLimitPost(52000)
        val trackerConfiguration = TrackerConfiguration(appId)
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
            .diagnosticAutotracking(true)
        val sessionConfiguration = SessionConfiguration(
            TimeMeasure(6, TimeUnit.SECONDS),
            TimeMeasure(30, TimeUnit.SECONDS)
        )
            .onSessionUpdate { state: SessionState ->
                updateLogger(
                    """
                Session: ${state.sessionId}
                previous: ${state.previousSessionId}
                eventId: ${state.firstEventId}
                index: ${state.sessionIndex}
                userId: ${state.userId}
                """.trimIndent()
                )
            }
        val gdprConfiguration = GdprConfiguration(
            Basis.CONSENT,
            "someId",
            "0.1.0",
            "this is a demo document description"
        )
        val gcConfiguration = GlobalContextsConfiguration(null)
        val pairs: Map<String, Any> = HashMap()
        Util.addToMap("id", "snowplow", pairs)
        Util.addToMap("email", "info@snowplow.io", pairs)
        gcConfiguration.add(
            "ruleSetExampleTag",
            GlobalContext(listOf(SelfDescribingJson(SCHEMA_IDENTIFY, pairs)))
        )
        createTracker(
            applicationContext,
            namespace,
            networkConfiguration,
            trackerConfiguration,
            emitterConfiguration,
            sessionConfiguration,
            gdprConfiguration,
            gcConfiguration
        )
        subscribeToWebViewEvents(_webView!!)
        return true
    }

    private fun trackEvents() {
        val tracker = defaultTracker
        if (tracker == null) {
            updateLogger("TrackerController not ready!")
            return
        }
        TrackerEvents.trackAll(tracker)
        eventsCreated += 11
        val made = "Made: $eventsCreated"
        runOnUiThread { _eventsCreated!!.text = made }
    }

    private fun requestPermissions(callbackIsGranted: Consumer<Boolean>) {
        callbackIsPermissionGranted = callbackIsGranted
        val permissionState =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permissionState == PackageManager.PERMISSION_GRANTED) {
            callbackIsGranted.accept(true)
            return
        }
        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        ActivityCompat.requestPermissions(this, permissions, APP_PERMISSION_REQUEST_LOCATION)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == APP_PERMISSION_REQUEST_LOCATION
            && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            callbackIsPermissionGranted!!.accept(true)
            return
        }
        callbackIsPermissionGranted!!.accept(false)
    }

    /**
     * Updates the logger with a message.
     *
     * @param message the message to add to the log.
     */
    private fun updateLogger(message: String) {
        runOnUiThread {
            _logOutput!!.append(message + "\n")
        }
    }

    /**
     * Updates the events sent counter.
     *
     * @param count the amount of successful events
     */
    private fun updateEventsSent(count: Int) {
        runOnUiThread {
            eventsSent += count
            val sent = "Sent: $eventsSent"
            _eventsSent!!.text = sent
        }
    }

    /**
     * Updates the various UI elements based on information
     * about the Tracker and Emitter.
     *
     * @param isOnline is the device online
     * @param isRunning is the emitter running
     * @param dbSize the database event size
     */
    private fun updateEmitterStats(
        isOnline: Boolean,
        isRunning: Boolean,
        dbSize: Long,
        sessionIndex: Int
    ) {
        runOnUiThread {
            val online = if (isOnline) "Online: yes" else "Online: no"
            _emitterOnline!!.text = online
            val status = if (isRunning) "Running: yes" else "Running: no"
            _emitterStatus!!.text = status
            val dbSizeStr = "DB Size: $dbSize"
            _databaseSize!!.text = dbSizeStr
            val sessionIndexStr = "Session #: $sessionIndex"
            _sessionIndex!!.text = sessionIndexStr

            if (isRunning) {
                when (_startButton!!.text.toString()) {
                    "Start!", "Running  ." -> _startButton!!.setText(R.string.running_1)
                    "Running.  " -> _startButton!!.setText(R.string.running_2)
                    else -> _startButton!!.setText(R.string.running_3)
                }
            } else {
                _startButton!!.setText(R.string.start)
            }
        }
    }

    /**
     * Starts a polling updater which will fetch
     * and update the UI.
     *
     * @param context the activity context
     */
    private fun makePollingUpdater(context: Context) {
        DemoUtils.scheduleRepeating(Runnable {
            val isOnline = Util.isOnline(context)
            val tracker = defaultTracker ?: return@Runnable

            val e = tracker.emitter
            val isRunning = e.isSending
            val dbSize = e.dbCount
            val session = tracker.session
            val sessionIndex = session?.sessionIndex ?: -1
            updateEmitterStats(isOnline, isRunning, dbSize, sessionIndex)
        }, 1, 1, TimeUnit.SECONDS)
    }

    /**
     * Returns the Emitter Request Callback.
     */
    private val requestCallback: RequestCallback
        private get() = object : RequestCallback {
            override fun onSuccess(successCount: Int) {
                updateLogger("Emitter Send Success:\n - Events sent: $successCount\n")
                updateEventsSent(successCount)
            }

            override fun onFailure(successCount: Int, failureCount: Int) {
                updateLogger("Emitter Send Failure:\n - Events sent: $successCount" +
                        " - Events failed: $failureCount")
                updateEventsSent(successCount)
            }
        }

    /// - Implements LoggerDelegate
    override fun error(tag: String, msg: String) {
        Log.e("[$tag]", msg)
    }

    override fun debug(tag: String, msg: String) {
        Log.d("[$tag]", msg)
    }

    override fun verbose(tag: String, msg: String) {
        Log.v("[$tag]", msg)
    }

    companion object {
        private const val APP_PERMISSION_REQUEST_LOCATION = 1
        private const val namespace = "SnowplowAndroidTrackerDemo"
        private const val appId = "DemoID"
    }
}
