/*
 * Copyright (c) 2015-2020 Snowplow Analytics Ltd. All rights reserved.
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

package com.snowplowanalytics.snowplow.internal.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.internal.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.internal.tracker.Logger;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Provides basic Utilities for the Snowplow Tracker.
 */
public class Util {

    private static final String TAG = Util.class.getSimpleName();

    /**
     * Returns the current System time
     * as a String.
     *
     * @return the system time as a string
     */
    @NonNull
    public static String getTimestamp() {
        return Long.toString(System.currentTimeMillis());
    }

    /**
     * Encodes a string into Base64.
     *
     * @param string the string too encode
     * @return a Base64 encoded string
     */
    @NonNull
    public static String base64Encode(@NonNull String string) {
        return Base64.encodeToString(string.getBytes(), Base64.NO_WRAP);
    }

    /**
     * Generates a random UUID for
     * each event.
     *
     * @return a UUID string
     */
    @NonNull
    public static String getUUIDString() {
        return UUID.randomUUID().toString();
    }

    /**
     * Check the passed string is a UUID code.
     *
     * @param uuid a UUID code string.
     * @return true if it's a UUID code.
     */
    public static boolean isUUIDString(@NonNull String uuid) {
        try {
            return UUID.fromString(uuid) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Count the number of bytes a string will occupy when UTF-8 encoded
     *
     * @param s the String to process
     * @return number Length of s in bytes when UTF-8 encoded
     */
    public static long getUTF8Length(@NonNull String s) {
        long len = 0;
        for (int i = 0; i < s.length(); i++) {
            char code = s.charAt(i);
            if (code <= 0x7f) {
                len += 1;
            } else if (code <= 0x7ff) {
                len += 2;
            } else if (code >= 0xd800 && code <= 0xdfff) {
                // Surrogate pair: These take 4 bytes in UTF-8 and 2 chars in UCS-2
                // (Assume next char is the other [valid] half and just skip it)
                len += 4; i++;
            } else if (code < 0xffff) {
                len += 3;
            } else {
                len += 4;
            }
        }
        return len;
    }

    /**
     * Checks whether or not the device
     * is online and able to communicate
     * with the outside world.
     *
     * @param context the android context
     * @return whether the tracker is online
     */
    public static boolean isOnline(@NonNull Context context) {

        Logger.v(TAG, "Checking tracker internet connectivity.");

        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        try {
            NetworkInfo ni = cm.getActiveNetworkInfo();
            boolean connected = ni != null && ni.isConnected();
            Logger.d(TAG, "Tracker connection online: %s", connected);
            return connected;
        } catch (SecurityException e) {
            Logger.e(TAG, "Security exception checking connection: %s", e.toString());
            return true;
        }
    }

    /**
     * The startTime must be greater than the endTime minus the
     * interval to be within an acceptable range.
     *
     * Example:
     * - Start Time = 1425060000000 // Fri, 27 Feb 2015 18:00:00 GMT
     * - Check Time = 1425060300000 // Fri, 27 Feb 2015 18:05:00 GMT
     * - Range = 600000 // 10 minutes
     *
     * If the start time is greater than 17:55:00 then it is in range.
     *
     * @param startTime the startTime of the check
     * @param checkTime the time of the check
     * @param range the allowed range the startTime must be in
     * @return whether the time is in range or not
     */
    public static boolean isTimeInRange(long startTime, long checkTime, long range) {
        return startTime > (checkTime - range);
    }

    /**
     * Joins a list of Longs into a single string
     *
     * @param list the list to join
     * @return the joined list
     */
    @NonNull
    public static String joinLongList(@NonNull List<Long> list) {
        String s = "";

        for (int i = 0; i < list.size(); i++) {
            Long longVal = list.get(i);
            if (longVal != null) {
                s += Long.toString(list.get(i));
                if (i < list.size() - 1) {
                    s += ",";
                }
            }
        }

        if (s.substring(s.length() - 1).equals(",")) {
            s = s.substring(0, s.length() - 1);
        }

        return s;
    }

    // --- Geo-Location Context

    /**
     * Returns the Geo-Location Context
     *
     * @param context the Android context
     * @return the geo-location context
     */
    @Nullable
    public static SelfDescribingJson getGeoLocationContext(@NonNull Context context) {
        Location location = getLastKnownLocation(context);

        if (location != null) {
            Map<String, Object> pairs = new HashMap<>();
            addToMap(Parameters.LATITUDE, location.getLatitude(), pairs);
            addToMap(Parameters.LONGITUDE, location.getLongitude(), pairs);
            addToMap(Parameters.ALTITUDE, location.getAltitude(), pairs);
            addToMap(Parameters.LATLONG_ACCURACY, location.getAccuracy(), pairs);
            addToMap(Parameters.SPEED, location.getSpeed(), pairs);
            addToMap(Parameters.BEARING, location.getBearing(), pairs);
            addToMap(Parameters.GEO_TIMESTAMP, System.currentTimeMillis(), pairs);

            if (mapHasKeys(pairs, Parameters.LATITUDE, Parameters.LONGITUDE)) {
                return new SelfDescribingJson(
                        TrackerConstants.GEOLOCATION_SCHEMA, pairs
                );
            }
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
    @Nullable
    public static Location getLastKnownLocation(@NonNull Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Location location = null;

        try {
            String locationProvider = null;
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationProvider = LocationManager.GPS_PROVIDER;
            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationProvider = LocationManager.NETWORK_PROVIDER;
            } else {
                List<String> locationProviders = locationManager.getProviders(true);
                if (locationProviders.size() > 0) {
                    locationProvider = locationProviders.get(0);
                }
            }

            if (locationProvider != null && !locationProvider.equals("")) {
                location = locationManager.getLastKnownLocation(locationProvider);
            }
        } catch (SecurityException ex) {
            Logger.e(TAG, "Exception occurred when retrieving location: %s", ex.toString());
        }

        return location;
    }

    // --- Application Context

    /**
     * Returns the Application Context
     */
    @Nullable
    static public SelfDescribingJson getApplicationContext(@NonNull Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            String versionName = pInfo.versionName;
            String versionCode = "";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                versionCode = String.valueOf(pInfo.getLongVersionCode());
            } else {
                versionCode = String.valueOf(pInfo.versionCode);
            }
            if (versionName != null) {
                Map<String, Object> pairs = new HashMap<>();
                addToMap(Parameters.APP_VERSION, versionName, pairs);
                addToMap(Parameters.APP_BUILD, versionCode, pairs);
                return new SelfDescribingJson(
                        TrackerConstants.SCHEMA_APPLICATION, pairs
                );
            }
        } catch (PackageManager.NameNotFoundException e) {
            Logger.e(TAG, "Failed to find application context: %s", e.getMessage());
        }
        return null;
    }

    // --- Mobile Context

    /**
     * Returns the Mobile Context
     *
     * @param context the Android context
     * @return the mobile context
     */
    @Nullable
    public static SelfDescribingJson getMobileContext(@NonNull Context context) {
        Map<String, Object> pairs = new HashMap<>();
        addToMap(Parameters.OS_TYPE, getOsType(), pairs);
        addToMap(Parameters.OS_VERSION, getOsVersion(), pairs);
        addToMap(Parameters.DEVICE_MODEL, getDeviceModel(), pairs);
        addToMap(Parameters.DEVICE_MANUFACTURER, getDeviceVendor(), pairs);
        addToMap(Parameters.CARRIER, getCarrier(context), pairs);
        addToMap(Parameters.ANDROID_IDFA, getAndroidIdfa(context), pairs);

        NetworkInfo networkInfo = getNetworkInfo(context);
        addToMap(Parameters.NETWORK_TYPE, getNetworkType(networkInfo), pairs);
        addToMap(Parameters.NETWORK_TECHNOLOGY, getNetworkTechnology(networkInfo), pairs);

        if (mapHasKeys(pairs,
                Parameters.OS_TYPE,
                Parameters.OS_VERSION,
                Parameters.DEVICE_MANUFACTURER,
                Parameters.DEVICE_MODEL)) {
            return new SelfDescribingJson(TrackerConstants.MOBILE_SCHEMA, pairs);
        } else {
            return null;
        }
    }

    /**
     * @return the OS Type
     */
    @NonNull
    public static String getOsType() {
        return "android";
    }

    /**
     * @return the OS Version
     */
    @NonNull
    public static String getOsVersion() {
        return android.os.Build.VERSION.RELEASE;
    }

    /**
     * @return the device model
     */
    @NonNull
    public static String getDeviceModel() {
        return android.os.Build.MODEL;
    }

    /**
     * @return the device vendor
     */
    @NonNull
    public static String getDeviceVendor() {
        return android.os.Build.MANUFACTURER;
    }

    /**
     * @param context the android context
     * @return a carrier name or null
     */
    @Nullable
    public static String getCarrier(@NonNull Context context) {
        TelephonyManager telephonyManager =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        if (telephonyManager != null) {
            String carrierName = telephonyManager.getNetworkOperatorName();
            if (!carrierName.equals("")) {
                return carrierName;
            }
        }
        return null;
    }

    /**
     * The function that actually fetches the Advertising ID.
     * - If called from the UI Thread will throw an Exception
     *
     * @param context the android context
     * @return an empty string if limited tracking is on otherwise the advertising id or null
     */
    @Nullable
    public static String getAndroidIdfa(@NonNull Context context) {
        try {
            Object advertisingInfoObject = invokeStaticMethod(
                    "com.google.android.gms.ads.identifier.AdvertisingIdClient",
                    "getAdvertisingIdInfo", new Class[]{Context.class}, context);
            Boolean limitedTracking = (Boolean) invokeInstanceMethod(advertisingInfoObject,
                    "isLimitAdTrackingEnabled", null);
            if (limitedTracking) {
                return "";
            }
            return (String) invokeInstanceMethod(advertisingInfoObject, "getId", null);
        }
        catch (Exception e) {
            Logger.e(TAG, "Exception getting the Advertising ID: %s", e.toString());
            return null;
        }
    }

    /**
     * Returns the network type that the device is connected to
     *
     * @param networkInfo The NetworkInformation object
     * @return the type of the network
     */
    @NonNull
    public static String getNetworkType(@Nullable NetworkInfo networkInfo) {
        String networkType = "offline";
        if (networkInfo != null) {
            String maybeNetworkType = networkInfo.getTypeName().toLowerCase();
            switch (maybeNetworkType) {
                case "mobile":
                case "wifi":
                    networkType = maybeNetworkType;
                    break;
                default: break;
            }
        }
        return networkType;
    }

    /**
     * Returns the network technology
     *
     * @param networkInfo The NetworkInformation object
     * @return the technology of the network
     */
    @Nullable
    public static String getNetworkTechnology(@Nullable NetworkInfo networkInfo) {
        String networkTech = null;
        if (networkInfo != null) {
            String networkType = networkInfo.getTypeName();
            if (networkType.equalsIgnoreCase("MOBILE")) {
                networkTech = networkInfo.getSubtypeName();
            }
        }
        return networkTech;
    }

    /**
     * Returns an instance that represents the current network connection
     *
     * @param context the android context
     * @return the representation of the current network connection or null
     */
    @Nullable
    public static NetworkInfo getNetworkInfo(@NonNull Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo ni = null;
        try {
            NetworkInfo maybeNi = cm.getActiveNetworkInfo();
            if (maybeNi != null && maybeNi.isConnected()) {
                ni = maybeNi;
            }
        } catch (SecurityException e) {
            Logger.e(TAG, "Security exception getting NetworkInfo: %s", e.toString());
        }
        return ni;
    }

    // --- Context Helpers

    /**
     * Checks if a map contains a range of keys
     *
     * @param map the map to check
     * @param keys the keys to check
     * @return whether the map contains the keys
     */
    public static boolean mapHasKeys(@NonNull Map<String, Object> map, @NonNull String... keys) {
        for (String key : keys) {
            if (!map.containsKey(key)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Inserts a value into a map safely.
     *
     * NOTE: Avoid putting null or empty values
     * in the map. If they are strings, avoid
     * empty strings
     *
     * @param key a key value
     * @param value the value associated with
     *              the key
     * @param map the map to insert the pair into
     */
    public static void addToMap(@NonNull String key, @NonNull Object value, @NonNull Map<String, Object> map) {
        if (key != null && value != null && !key.isEmpty()) {
            map.put(key, value);
        }
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
    private static Object invokeStaticMethod(String className, String methodName,
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
    private static Object invokeInstanceMethod(Object instance, String methodName,
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
    @SuppressWarnings("unchecked")
    private static Object invokeMethod(Class classObject, String methodName, Object instance,
                                       Class[] cArgs, Object... args) throws Exception {
        Method methodObject = classObject.getMethod(methodName, cArgs);
        return methodObject.invoke(instance, args);
    }

    /**
     * Converts an event map to a byte
     * array for storage.
     *
     * @param map the map containing all
     *            the event parameters
     * @return the byte array or null
     */
    @Nullable
    public static byte[] serialize(@NonNull Map<String, String> map) {
        byte[] newByteArray = null;
        try {
            ByteArrayOutputStream mem_out = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(mem_out);
            out.writeObject(map);
            out.close();
            mem_out.close();
            newByteArray = mem_out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return newByteArray;
    }

    /**
     * Converts a byte array back into an
     * event map for sending.
     *
     * @param bytes the bytes to be converted
     * @return the Map or null
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static Map<String, String> deserializer(@NonNull byte[] bytes) {
        Map<String, String> newMap = null;
        try {
            ByteArrayInputStream mem_in = new ByteArrayInputStream(bytes);
            ObjectInputStream in = new ObjectInputStream(mem_in);
            Map<String, String> map = (HashMap<String, String>) in.readObject();
            in.close();
            mem_in.close();
            newMap = map;
        } catch (NullPointerException | ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
        return newMap;
    }

    /**
     * Converts a StackTrace to a String
     *
     * @param e the Throwable to convert
     * @return the StackTrace as a string
     */
    @NonNull
    public static String stackTraceToString(@NonNull Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
