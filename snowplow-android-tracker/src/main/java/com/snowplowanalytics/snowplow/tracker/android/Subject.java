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
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.snowplowanalytics.snowplow.tracker.core.DevicePlatform;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class Subject extends com.snowplowanalytics.snowplow.tracker.core.Subject {

    private String TAG = Subject.class.getName();
    private HashMap<String, String> standardPairs = new HashMap<String, String>();
    private HashMap<String, Object> geoLocationPairs = new HashMap<String, Object>();
    private HashMap<String, String> mobilePairs = new HashMap<String, String>();

    public Subject() {
        super();

        // Default Platform
        super.setPlatform(DevicePlatform.Mobile);

        // Default Timezone
        setDefaultTimezone();

        // Default Language
        setDefaultLanguage();

        // Other mobile context data
        setOsType();
        setOsVersion();
        setDeviceModel();
        setDeviceVendor();
    }

    public Subject(Context context) {
        // Default constructor for Subject data we can get
        this();

        // Default Screen Resolution
        setDefaultScreenResolution(context);

        // Advertising ID from Play Services
        setAdvertisingID(context);

        // Closest Location available
        setLocation(context);

        // Carrier Name
        setCarrier(context);
    }

    private void putToMobile(String key, String value) {
        // Avoid putting null or empty values in the map
        if (key != null && value != null && !key.isEmpty() && !value.isEmpty())
            this.mobilePairs.put(key, value);
    }

    private void putToGeoLocation(String key, Object value) {
        // Avoid putting null or empty values in the map
        // or if they are strings, avoid empty strings
        if (key != null && value != null && !key.isEmpty()
                || (value instanceof String) && !((String) value).isEmpty())
            this.geoLocationPairs.put(key, value);
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

    private void setAdvertisingID(final Context context) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                AdvertisingIdClient.Info info = null;
                try {
                    info = AdvertisingIdClient.getAdvertisingIdInfo(context);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                } catch (GooglePlayServicesRepairableException e) {
                    e.printStackTrace();
                }
                putToMobile(Parameter.ANDROID_IDFA, info.getId());
            }
        });
        thread.start();
    }

    private void setLocation(Context context) {
        Location location = Util.getLocation(context);
        if (location == null) // No location available
            return;
        putToGeoLocation(Parameter.LATITUDE, location.getLatitude());
        putToGeoLocation(Parameter.LONGITUDE, location.getLongitude());
        putToGeoLocation(Parameter.ALTITUDE, location.getAltitude());
        putToGeoLocation(Parameter.LATLONG_ACCURACY, location.getAccuracy());
        putToGeoLocation(Parameter.SPEED, location.getSpeed());
        putToGeoLocation(Parameter.BEARING, location.getBearing());
    }

    private void setCarrier(Context context) {
        putToMobile(Parameter.CARRIER, Util.getCarrier(context));
    }

    private void setDeviceModel() {
        putToMobile(Parameter.DEVICE_MODEL, android.os.Build.MODEL);
    }

    private void setDeviceVendor() {
        putToMobile(Parameter.DEVICE_MANUFACTURER, Build.MANUFACTURER);
    }

    private void setOsVersion() {
        putToMobile(Parameter.OS_VERSION, android.os.Build.VERSION.RELEASE);
    }

    private void setOsType() {
        putToMobile(Parameter.OS_TYPE, "android");
    }

    public Map<String, Object> getSubjectLocation() {
        return this.geoLocationPairs;
    }

    public Map<String, String> getSubjectMobile() { return this.mobilePairs; }

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
