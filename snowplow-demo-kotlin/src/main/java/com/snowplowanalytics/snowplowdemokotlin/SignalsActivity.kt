/*
 * Copyright (c) 2015-present Snowplow Analytics Ltd. All rights reserved.
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

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.snowplowanalytics.snowplow.Snowplow
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration
import com.snowplowanalytics.snowplow.configuration.SignalsConfiguration
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration
import com.snowplowanalytics.snowplow.controller.TrackerController
import com.snowplowanalytics.snowplow.event.ScreenView
import com.snowplowanalytics.snowplow.event.Structured
import com.snowplowanalytics.snowplow.signals.ServiceAttributesResult
import com.snowplowanalytics.snowplow.signals.SignalsAttributeKeys
import com.snowplowanalytics.snowplow.tracker.LogLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Demonstrates reading attributes from, and receiving interventions from, Snowplow Signals.
 *
 * The defaults point at a collector and Signals running on the host machine, as seen from the
 * Android emulator. They can also be passed as intent extras: `collector`, `signals`, `service`.
 */
class SignalsActivity : Activity() {
    private lateinit var collectorField: EditText
    private lateinit var signalsField: EditText
    private lateinit var serviceField: EditText
    private lateinit var log: TextView
    private var tracker: TrackerController? = null
    private var isHandlerAdded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Signals Demo"

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        collectorField = field(layout, "Collector endpoint", intent.getStringExtra("collector") ?: "http://10.0.2.2:9091")
        signalsField = field(layout, "Signals endpoint", intent.getStringExtra("signals") ?: "http://10.0.2.2:8081")
        serviceField = field(layout, "Service", intent.getStringExtra("service") ?: "mobile_profile")

        button(layout, "Create tracker") { createTracker() }
        button(layout, "Track screen view") { track { ScreenView("signals_demo") } }
        button(layout, "Track structured event") { track { Structured("signals_demo", "tap") } }
        button(layout, "Read attributes (user)") { read(SignalsAttributeKeys.CLIENT_SESSION_USER_ID) }
        button(layout, "Read attributes (session)") { read(SignalsAttributeKeys.CLIENT_SESSION_SESSION_ID) }
        val handlerButton = button(layout, "Add intervention handler") {}
        handlerButton.setOnClickListener {
            toggleHandler()
            handlerButton.text = if (isHandlerAdded) "Remove intervention handler" else "Add intervention handler"
        }

        log = TextView(this).apply { setTextIsSelectable(true) }
        layout.addView(log)
        setContentView(ScrollView(this).apply { addView(layout) })

        createTracker()
    }

    private fun createTracker() {
        Snowplow.getTracker("signals-demo")?.let { Snowplow.removeTracker(it) }
        isHandlerAdded = false
        tracker = Snowplow.createTracker(
            this,
            "signals-demo",
            NetworkConfiguration(collectorField.text.toString()),
            TrackerConfiguration("signals-demo-app").logLevel(LogLevel.DEBUG),
            SignalsConfiguration(signalsField.text.toString())
        )
        append("Tracker created. userId=${tracker?.session?.userId}")
    }

    private fun track(event: () -> com.snowplowanalytics.snowplow.event.Event) {
        val tracker = tracker ?: return
        tracker.track(event())
        append("Tracked event. sessionId=${tracker.session?.sessionId}")
    }

    private fun read(attributeKey: String) {
        val tracker = tracker ?: return
        val service = serviceField.text.toString()
        tracker.signals.getServiceAttributes(service, attributeKey) { result: ServiceAttributesResult ->
            runOnUiThread {
                if (result.isSuccess) {
                    append("Attributes of $service by $attributeKey: ${result.attributes}")
                } else {
                    append("Read failed: status=${result.error?.status} message=${result.error?.message}")
                }
            }
        }
    }

    private fun toggleHandler() {
        val tracker = tracker ?: return
        if (isHandlerAdded) {
            tracker.signals.removeInterventionHandler("demo")
            append("Intervention handler removed")
        } else {
            tracker.signals.addInterventionHandler("demo") { intervention ->
                append("Intervention received: ${intervention.name} v${intervention.version} " +
                    "for ${intervention.targetAttributeKey.name} attributes=${intervention.attributes}")
            }
            append("Intervention handler added")
        }
        isHandlerAdded = !isHandlerAdded
    }

    private fun append(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        log.append("$time $message\n\n")
        android.util.Log.i("SignalsDemo", message)
    }

    private fun field(layout: LinearLayout, hint: String, value: String): EditText {
        return EditText(this).apply {
            this.hint = hint
            setText(value)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            layout.addView(this)
        }
    }

    private fun button(layout: LinearLayout, text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            setOnClickListener { onClick() }
            layout.addView(this)
        }
    }
}
