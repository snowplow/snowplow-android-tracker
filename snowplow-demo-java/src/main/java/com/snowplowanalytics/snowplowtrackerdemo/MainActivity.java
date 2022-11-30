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

import android.os.Bundle;
import android.app.Activity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.Button;
import android.view.View;
import android.content.Intent;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.tracker.LogLevel;
import com.snowplowanalytics.core.tracker.Logger;

/**
 * Main Activity
 */
@SuppressWarnings("FieldCanBeLocal")
public class MainActivity extends Activity {

    private Button _liteBtn;

    private final String repo_url = "https://github.com/snowplow/snowplow-android-tracker";
    private final String tech_docs_url = "https://snowplow.github.io/snowplow-android-tracker";
    private final String snowplow_docs_url = "https://docs.snowplow.io/docs/collecting-data/collecting-from-own-applications/mobile-trackers";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _liteBtn = ( Button ) findViewById(R.id.btn_lite);

        _liteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.updateLogLevel(LogLevel.VERBOSE);
                Intent intent = new Intent(MainActivity.this, Demo.class);
                startActivity(intent);
            }
        });

        // Setup Hyperlinks

        TextView link1 = (TextView) findViewById(R.id.link_tech_docs);
        String linkText1 = "- <a href='" + tech_docs_url + "'>API Documentation</a>";
        link1.setText(Html.fromHtml(linkText1));
        link1.setMovementMethod(LinkMovementMethod.getInstance());

        TextView link3 = (TextView) findViewById(R.id.link_repo);
        String linkText3 = "- <a href='" + repo_url + "'>Github Repository</a>";
        link3.setText(Html.fromHtml(linkText3));
        link3.setMovementMethod(LinkMovementMethod.getInstance());

        TextView link4 = (TextView) findViewById(R.id.link_docs);
        String linkText4 = "- <a href='" + snowplow_docs_url + "'>Documentation</a>";
        link4.setText(Html.fromHtml(linkText4));
        link4.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
