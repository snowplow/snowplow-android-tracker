/*
 * Copyright (c) 2014 Snowplow Analytics Ltd. All rights reserved.
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

package com.snowplowanalytics.snowplow.tracker.android;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.view.Display;
import android.view.WindowManager;

import com.snowplowanalytics.snowplow.tracker.DevicePlatform;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

//public class Subject {
public class Subject extends com.snowplowanalytics.snowplow.tracker.Subject {

    public Subject() {
        // Default Platform
        this.setPlatform(DevicePlatform.Mobile);

        // Default Timezone
        setDefaultTimezone();
    }

    public Subject(Context context) {
        // Default Platform
        this.setPlatform(DevicePlatform.Mobile);

        // Default Timezone
        setDefaultTimezone();

        // Default Screen Resolution
        setDefaultScreenResolution(context);

        // Default Language
        setDefaultLanguage();
    }

    private void setDefaultScreenResolution(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
//        Point size = new Point();
//        if(Build.VERSION.SDK_INT >= 13) {
//            display.getSize(size);
//            this.setScreenResolution(size.x, size.y);
//        } else {
            this.setScreenResolution(display.getWidth(), display.getHeight());
//        }
    }

    private void setDefaultTimezone() {
        TimeZone tz = Calendar.getInstance().getTimeZone();
        this.setTimezone(tz.getID());
    }

    private void setDefaultLanguage() {
        this.setLanguage(Locale.getDefault().getDisplayLanguage());
    }
}
