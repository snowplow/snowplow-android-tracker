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

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Point;
import android.location.Location;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.snowplowanalytics.snowplow.tracker.core.DevicePlatform;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class Subject extends com.snowplowanalytics.snowplow.tracker.core.Subject {

    private HashMap<String, String> standardPairs = new HashMap<String, String>();
    private HashMap<String, String> geoLocationPairs = new HashMap<String, String>();

    public Subject() {
        super();

        // Default Platform
        super.setPlatform(DevicePlatform.Mobile);

        // Default Timezone
        setDefaultTimezone();

        // Default Language
        setDefaultLanguage();
    }

    public Subject(Context context) {
        super();

        // Default Platform
        super.setPlatform(DevicePlatform.Mobile);

        // Default Timezone
        setDefaultTimezone();

        // Default Language
        setDefaultLanguage();

        // Default Screen Resolution
        setDefaultScreenResolution(context);

        // Advertising ID from Play Services
        setAdvertisingID(context);

        // Closest Location available
        setLocation(context);

        // Carrier Name
        setCarrier(context);
    }

    @TargetApi(19)
    @SuppressWarnings("deprecation")
    private void setDefaultScreenResolution(Context context) {
        WindowManager windowManager =
                (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
            try {
                Class<?> partypes[] = new Class[1];
                partypes[0] = Point.class;
                Display.class.getMethod("getSize", partypes);
                display.getSize(size);
                this.setScreenResolution(size.x, size.y);
            } catch (NoSuchMethodException e) {
                Log.e(Subject.class.toString(), "Display.getSize isn't available on older devices.");
                this.setScreenResolution(display.getWidth(), display.getHeight());
            }
    }

    private void setDefaultTimezone() {
        TimeZone tz = Calendar.getInstance().getTimeZone();
        this.setTimezone(tz.getID());
    }

    private void setDefaultLanguage() {
        this.setLanguage(Locale.getDefault().getDisplayLanguage());
    }

    private void setAdvertisingID(Context context) {
        this.standardPairs.put(Parameter.ANDROID_IDFA, Util.getAdvertisingID(context));
    }

    private void setLocation(Context context) {
        Location location = Util.getLocation(context);
        this.geoLocationPairs.put(Parameter.LATITUDE,
                Double.toString(location.getLatitude()));
        this.geoLocationPairs.put(Parameter.LONGITUDE,
                Double.toString(location.getLongitude()));
        this.geoLocationPairs.put(Parameter.ALTITUDE,
                Double.toString(location.getAltitude()));
        this.geoLocationPairs.put(Parameter.LATLONG_ACCURACY,
                Float.toString(location.getAccuracy()));
        this.geoLocationPairs.put(Parameter.SPEED,
                Float.toString(location.getSpeed()));
        this.geoLocationPairs.put(Parameter.BEARING,
                Double.toString(location.getBearing()));
    }

    private void setCarrier(Context context) {
        this.standardPairs.put(Parameter.CARRIER, Util.getCarrier(context));
    }

    public Map<String, String> getSubjectLocation() {
        return this.geoLocationPairs;
    }

    public Map<String, String> getSubject() {
        HashMap<String, String> allPairs = new HashMap<String, String>();
        allPairs.putAll(super.getSubject());
        allPairs.putAll(this.standardPairs);
        return allPairs;
    }

    public void setContext(Context context) {
        setAdvertisingID(context);
        setDefaultScreenResolution(context);
        setLocation(context);
        setCarrier(context);
    }
}
