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

package com.snowplowanalytics.snowplowtrackerdemo;

import android.os.Bundle;
import android.app.Activity;
import android.widget.Button;
import android.view.View;
import android.content.Intent;

import com.snowplowanalytics.snowplow.tracker.LogLevel;
import com.snowplowanalytics.snowplow.tracker.utils.Logger;

public class MainActivity extends Activity {

    Button _rxDemoBtn;
    Button _rxLiteBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _rxDemoBtn = ( Button ) findViewById(R.id.btn_rx);
        _rxLiteBtn = ( Button ) findViewById(R.id.btn_lite);

        _rxDemoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RxDemo.class);
                startActivity(intent);
                Logger.updateLogLevel(LogLevel.VERBOSE);
            }
        });

        _rxLiteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ClassicDemo.class);
                startActivity(intent);
                Logger.updateLogLevel(LogLevel.VERBOSE);
            }
        });
    }
}
