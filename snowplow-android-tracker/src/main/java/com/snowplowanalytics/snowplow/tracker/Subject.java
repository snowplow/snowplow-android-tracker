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

package com.snowplowanalytics.snowplow.tracker;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Point;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.view.Display;
import android.view.WindowManager;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.lang.reflect.Method;

import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.utils.Logger;

/**
 * Provides Subject information for
 * each event sent from the Snowplow
 * Tracker.
 */
public class Subject {

    private static String TAG = Subject.class.getSimpleName();

    private HashMap<String, String> standardPairs = new HashMap<>();
    private HashMap<String, Object> geoLocationPairs = new HashMap<>();
    private HashMap<String, String> mobilePairs = new HashMap<>();

    /**
     * Creates a Subject object
     * @param builder The builder that constructs a subject
     */
    private Subject(SubjectBuilder builder) {
        setDefaultTimezone();
        setDefaultLanguage();
        setOsType();
        setOsVersion();
        setDeviceModel();
        setDeviceVendor();

        if (builder.context != null) {
            setContextualParams(builder.context);
        }
    }

    public static class SubjectBuilder {
        private Context context = null; // Optional

        /**
         * @param context The android context to pass to the subject
         */
        public SubjectBuilder context(Context context) {
            this.context = context;
            return this;
        }

        /**
         * Creates a new Subject
         */
        public Subject build() {
            return new Subject(this);
        }
    }

    /**
     * Sets the context based parameters
     *
     * @param context the android context
     */
    public void setContextualParams(Context context) {
        setAdvertisingID(context);
        setDefaultScreenResolution(context);
        setLocation(context);
        setCarrier(context);
    }

    /**
     * Inserts a value into the mobilePairs
     * subject storage.
     *
     * NOTE: Avoid putting null or empty
     * values in the map
     *
     * @param key a key value
     * @param value the value associated with
     *              the key
     */
    private void putToMobile(String key, String value) {
        if (key != null && value != null && !key.isEmpty() && !value.isEmpty()) {
            this.mobilePairs.put(key, value);
        }
    }

    /**
     * Inserts a value into the geoLocation
     * subject storage.
     *
     * NOTE: Avoid putting null or empty values
     * in the map. If they are strings, avoid
     * empty strings
     *
     * @param key a key value
     * @param value the value associated with
     *              the key
     */
    private void putToGeoLocation(String key, Object value) {
        if (key != null && value != null && !key.isEmpty() ||
                (value instanceof String) && !((String) value).isEmpty()) {
            this.geoLocationPairs.put(key, value);
        }
    }

    // Default information setters

    /**
     * Sets the default timezone of the
     * device.
     */
    private void setDefaultTimezone() {
        TimeZone tz = Calendar.getInstance().getTimeZone();
        this.setTimezone(tz.getID());
    }

    /**
     * Sets the default language of the
     * device.
     */
    private void setDefaultLanguage() {
        this.setLanguage(Locale.getDefault().getDisplayLanguage());
    }

    /**
     * Set operating system type.
     * Defaults too 'android' currently.
     */
    private void setOsType() {
        putToMobile(Parameters.OS_TYPE, "android");
    }

    /**
     * Sets the operating system version.
     */
    private void setOsVersion() {
        putToMobile(Parameters.OS_VERSION, android.os.Build.VERSION.RELEASE);
    }

    /**
     * Sets the device model.
     */
    private void setDeviceModel() {
        putToMobile(Parameters.DEVICE_MODEL, android.os.Build.MODEL);
    }

    /**
     * Sets the device vendor/manufacturer.
     */
    private void setDeviceVendor() {
        putToMobile(Parameters.DEVICE_MANUFACTURER, Build.MANUFACTURER);
    }

    // Context information setters

    /**
     * Sets the advertising id of the
     * device.
     *
     * @param context the android context
     */
    private void setAdvertisingID(Context context) {
        try {
            Object AdvertisingInfoObject = getAdvertisingInfoObject(context);
            String playAdId = (String) invokeInstanceMethod(AdvertisingInfoObject, "getId", null);
            putToMobile(Parameters.ANDROID_IDFA, playAdId);
        }
        catch (Exception e) {
            Logger.d(TAG, "Unable to obtain Google AdvertisingIdClient.Info via reflection.", null,
                    e.toString());
            Logger.d(TAG, "Exception cause: %s", null, e.getCause().toString());
        }
    }

    /**
     * Returns the AdvertisingIdInfo object
     *
     * @param context the android context
     * @return the advertising id information object
     * @throws Exception
     */
    private Object getAdvertisingInfoObject(Context context) throws Exception {
        return invokeStaticMethod(
                "com.google.android.gms.ads.identifier.AdvertisingIdClient",
                "getAdvertisingIdInfo",
                new Class[] {Context.class},
                context
        );
    }

    /**
     * Invokes a static method within a class
     * if it can be found on the classpath.
     *
     * @param className The full defined classname
     * @param methodName The name of the method to invoke
     * @param cArgs The args that the method can take
     * @param args The args to pass to the method on invocation
     * @return the result of the method invoke
     * @throws Exception
     */
    private Object invokeStaticMethod(String className, String methodName,
                                             Class[] cArgs, Object... args) throws Exception {
        Class classObject = Class.forName(className);
        return invokeMethod(classObject, methodName, null, cArgs, args);
    }

    /**
     * Invokes a method on a static instance
     * within a class by reflection.
     *
     * @param instance The instance to invoke a method on
     * @param methodName The name of the method to invoke
     * @param cArgs The args that the method can take
     * @param args The args to pass to the method on invocation
     * @return the result of the method invoke
     * @throws Exception
     */
    private Object invokeInstanceMethod(Object instance, String methodName,
                                               Class[] cArgs, Object... args) throws Exception {
        Class classObject = instance.getClass();
        return invokeMethod(classObject, methodName, instance, cArgs, args);
    }

    /**
     * Invokes methods of a class via reflection
     *
     * @param classObject The class to attempt invocation on
     * @param methodName The name of the method to invoke
     * @param instance The object instance to invoke on
     * @param cArgs The args that the method can take
     * @param args The args to pass to the method on invocation
     * @return the result of the method invoke
     * @throws Exception
     */
    private Object invokeMethod(Class classObject, String methodName, Object instance,
                                       Class[] cArgs, Object... args) throws Exception {
        Method methodObject = classObject.getMethod(methodName, cArgs);
        return methodObject.invoke(instance, args);
    }

    /**
     * Sets the default screen resolution
     * of the device the Tracker is running
     * on.
     *
     * @param context the android context
     */
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
            Logger.e(TAG, "Display.getSize isn't available on older devices.", null);
            this.setScreenResolution(display.getWidth(), display.getHeight());
        }
    }

    /**
     * Sets the location of the android
     * device.
     *
     * @param context the android context
     */
    private void setLocation(Context context) {
        Location location = getLocation(context);
        if (location == null) // No location available
            return;
        putToGeoLocation(Parameters.LATITUDE, location.getLatitude());
        putToGeoLocation(Parameters.LONGITUDE, location.getLongitude());
        putToGeoLocation(Parameters.ALTITUDE, location.getAltitude());
        putToGeoLocation(Parameters.LATLONG_ACCURACY, location.getAccuracy());
        putToGeoLocation(Parameters.SPEED, location.getSpeed());
        putToGeoLocation(Parameters.BEARING, location.getBearing());
    }

    /**
     * Sets the carrier of the android
     * device.
     *
     * @param context the android context
     */
    private void setCarrier(Context context) {
        String carrier = getCarrier(context);
        if (carrier == null)
            return;
        putToMobile(Parameters.CARRIER, getCarrier(context));
    }

    // Public Subject information setters

    /**
     * Sets the subjects userId
     *
     * @param userId a user id string
     */
    public void setUserId(String userId) {
        this.standardPairs.put(Parameters.UID, userId);
    }

    /**
     * Sets a custom screen resolution based
     * on user inputted width and height.
     *
     * Measured in pixels: 1920x1080
     *
     * @param width the width of the screen
     * @param height the height of the screen
     */
    public void setScreenResolution(int width, int height) {
        String res = Integer.toString(width) + "x" + Integer.toString(height);
        this.standardPairs.put(Parameters.RESOLUTION, res);
    }

    /**
     * Sets the view port resolution
     *
     * Measured in pixels: 1280x1024
     *
     * @param width the width of the viewport
     * @param height the height of the viewport
     */
    public void setViewPort(int width, int height) {
        String res = Integer.toString(width) + "x" + Integer.toString(height);
        this.standardPairs.put(Parameters.VIEWPORT, res);
    }

    /**
     * user defined color depth.
     *
     * Measure as an integer
     *
     * @param depth the color depth
     */
    public void setColorDepth(int depth) {
        this.standardPairs.put(Parameters.COLOR_DEPTH, Integer.toString(depth));
    }

    /**
     * User inputted timezone
     *
     * @param timezone a valid timezone
     */
    public void setTimezone(String timezone) {
        this.standardPairs.put(Parameters.TIMEZONE, timezone);
    }

    /**
     * User inputted language for the
     * subject.
     *
     * @param language language setting
     */
    public void setLanguage(String language) {
        this.standardPairs.put(Parameters.LANGUAGE, language);
    }

    /**
     * User inputted ip address for the
     * subject.
     *
     * @param ipAddress an ip address
     */
    public void setIpAddress(String ipAddress) {
        this.standardPairs.put(Parameters.IP_ADDRESS, ipAddress);
    }

    /**
     * User inputted useragent for the
     * subject.
     *
     * @param useragent a useragent
     */
    public void setUseragent(String useragent) {
        this.standardPairs.put(Parameters.USERAGENT, useragent);
    }

    /**
     * User inputted Network User Id for the
     * subject.
     *
     * @param networkUserId a network user id
     */
    public void setNetworkUserId(String networkUserId) {
        this.standardPairs.put(Parameters.NETWORK_UID, networkUserId);
    }

    /**
     * User inputted Domain User Id for the
     * subject.
     *
     * @param domainUserId a domain user id
     */
    public void setDomainUserId(String domainUserId) {
        this.standardPairs.put(Parameters.DOMAIN_UID, domainUserId);
    }

    // Get Functions

    /**
     * Returns the carrier name based
     * on the android context supplied.
     *
     * @param context the android context
     * @return a carrier name
     */
    private static String getCarrier(Context context) {
        TelephonyManager telephonyManager =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        if (telephonyManager != null) {
            return telephonyManager.getNetworkOperatorName();
        }
        return null;
    }

    /**
     * Returns the location of the android
     * device.
     *
     * @param context the android context
     * @return the phones Location
     */
    private static Location getLocation(Context context) {
        LocationManager locationManager =
                (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);

        String provider = locationManager.getBestProvider(criteria, true);
        if (provider != null) {
            try {
                return locationManager.getLastKnownLocation(provider);
            } catch (SecurityException ex) {
                Logger.e(TAG, "Failed to retrieve retrieve location: %s", null, ex.toString());
                return null;
            }
        }
        return null;
    }

    // Functions too return individual maps of information

    /**
     * @return the geolocation subject pairs
     */
    public Map<String, Object> getSubjectLocation() {
        return this.geoLocationPairs;
    }

    /**
     * @return the mobile subject pairs
     */
    public Map<String, String> getSubjectMobile() {
        return this.mobilePairs;
    }

    /**
     * @return the standard subject pairs
     */
    public Map<String, String> getSubject() {
        return this.standardPairs;
    }
}
