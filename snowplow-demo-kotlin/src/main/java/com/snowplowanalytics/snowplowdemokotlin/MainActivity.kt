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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.snowplowanalytics.core.tracker.Logger
import com.snowplowanalytics.snowplow.tracker.LogLevel

/**
 * Main Activity
 */
class MainActivity : Activity() {
    private var _liteBtn: Button? = null
    private val repoUrl = "https://github.com/snowplow/snowplow-android-tracker"
    private val snowplowDocsUrl = "https://docs.snowplow.io/docs/collecting-data/collecting-from-own-applications/mobile-trackers"
    private val techDocsUrl = "https://snowplow.github.io/snowplow-android-tracker"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        _liteBtn = findViewById<View>(R.id.btn_lite) as Button
        _liteBtn?.setOnClickListener {
            Logger.updateLogLevel(LogLevel.VERBOSE)
            val intent = Intent(this@MainActivity, Demo::class.java)
            startActivity(intent)
        }

        // Setup Hyperlinks

        val link1 = findViewById<View>(R.id.link_tech_docs) as TextView
        val linkText1 = "- <a href='$techDocsUrl'>API Documentation</a>"
        link1.text = Html.fromHtml(linkText1, 0)
        link1.movementMethod = LinkMovementMethod.getInstance()

        val link2 = findViewById<View>(R.id.link_docs) as TextView
        val linkText2 = "- <a href='$snowplowDocsUrl'>Documentation</a>"
        link2.text = Html.fromHtml(linkText2, 0)
        link2.movementMethod = LinkMovementMethod.getInstance()

        val link3 = findViewById<View>(R.id.link_repo) as TextView
        val linkText3 = "- <a href='$repoUrl'>Github Repository</a>"
        link3.text = Html.fromHtml(linkText3, 0)
        link3.movementMethod = LinkMovementMethod.getInstance()

    }
}
