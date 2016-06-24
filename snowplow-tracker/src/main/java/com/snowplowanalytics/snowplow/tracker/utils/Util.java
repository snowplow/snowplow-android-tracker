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

package com.snowplowanalytics.snowplow.tracker.utils;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Base64;

import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

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
    public static String getTimestamp() {
        return Long.toString(System.currentTimeMillis());
    }

    /**
     * Encodes a string into Base64.
     *
     * @param string the string too encode
     * @return a Base64 encoded string
     */
    public static String base64Encode(String string) {
        return Base64.encodeToString(string.getBytes(), Base64.NO_WRAP);
    }

    /**
     * Generates a random UUID for
     * each event.
     *
     * @return a UUID string
     */
    public static String getEventId() {
        return UUID.randomUUID().toString();
    }

    /**
     *  Converts a Map to a JSONObject
     *
     *  @param map The map to convert
     *  @return The JSONObject
     */
    @SuppressWarnings("unchecked")
    public static JSONObject mapToJSONObject(Map map) {
        Logger.v(TAG, "Converting a map to a JSONObject: %s", map);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return new JSONObject(map);
        } else {
            JSONObject retObject = new JSONObject();
            Set<Map.Entry> entries = map.entrySet();
            for (Map.Entry entry : entries) {
                String key = (String) entry.getKey();
                Object value = getJsonSafeObject(entry.getValue());
                try {
                    retObject.put(key, value);
                } catch (JSONException e) {
                    Logger.e(TAG, "Could not put key '%s' and value '%s' into new JSONObject: %s", key, value, e);
                    e.printStackTrace();
                }
            }
            return retObject;
        }
    }
    
    /**
     * Returns a Json Safe object for situations
     * where the Build Version is too old.
     *
     * @param o The object to check and convert
     * @return the json safe object
     */
    private static Object getJsonSafeObject(Object o) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return o;
        } else if (o == null) {
            return JSONObject.NULL;
        } else if (o instanceof JSONObject || o instanceof JSONArray) {
            return o;
        } else if (o instanceof Collection) {
            JSONArray retArray = new JSONArray();
            for (Object entry : (Collection) o) {
                retArray.put(getJsonSafeObject(entry));
            }
            return retArray;
        } else if (o.getClass().isArray()) {
            JSONArray retArray = new JSONArray();
            int length = Array.getLength(o);
            for (int i = 0; i < length; i++) {
                retArray.put(getJsonSafeObject(Array.get(o, i)));
            }
            return retArray;
        } else if (o instanceof Map) {
            return mapToJSONObject((Map)o);
        } else  if (o instanceof Boolean || 
                o instanceof Byte ||
                o instanceof Character ||
                o instanceof Double ||
                o instanceof Float ||
                o instanceof Integer ||
                o instanceof Long ||
                o instanceof Short ||
                o instanceof String) {
            return o;
        } else if (o.getClass().getPackage().getName().startsWith("java.")) {
            return o.toString();
        }
        return null;
    }

    /**
     * Count the number of bytes a string will occupy when UTF-8 encoded
     *
     * @param s the String to process
     * @return number Length of s in bytes when UTF-8 encoded
     */
    public static long getUTF8Length(String s) {
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
    public static boolean isOnline(Context context) {

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
    public static String joinLongList(List<Long> list) {
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

    private static SelfDescribingJson geoLocationContext = null;
    private static AtomicBoolean geoLocationContextAttempted = new AtomicBoolean(false);

    /**
     * Returns the Geo-Location Context
     *
     * @param context the Android context
     * @return the geo-location context
     */
    public synchronized static SelfDescribingJson getGeoLocationContext(Context context) {
        if (!geoLocationContextAttempted.getAndSet(true)) {
            Location location = getLocation(context);

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
                    geoLocationContext = new SelfDescribingJson(
                            TrackerConstants.GEOLOCATION_SCHEMA, pairs
                    );
                }
            }
        }

        return geoLocationContext;
    }

    /**
     * Returns the location of the android
     * device.
     *
     * @param context the android context
     * @return the phones Location
     */
    public static Location getLocation(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!isNetworkEnabled && !isGPSEnabled) {
            Logger.e(TAG, "Cannot get location, Network and GPS are disabled");
            return null;
        } else {
            Location location = null;
            try {
                if (isNetworkEnabled) {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    Logger.d(TAG, "Network location found: %s", location);
                } else if (isGPSEnabled) {
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    Logger.d(TAG, "GPS location found: %s", location);
                }
            } catch (SecurityException ex) {
                Logger.e(TAG, "Exception occurred when retrieving location: %s", ex.toString());
            }
            return location;
        }
    }

    // --- Mobile Context

    private static SelfDescribingJson mobileContext = null;
    private static AtomicBoolean mobileContextAttempted = new AtomicBoolean(false);

    /**
     * Returns the Mobile Context
     *
     * @param context the Android context
     * @return the mobile context
     */
    public synchronized static SelfDescribingJson getMobileContext(Context context) {
        if (!mobileContextAttempted.getAndSet(true)) {
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
                mobileContext = new SelfDescribingJson(TrackerConstants.MOBILE_SCHEMA, pairs);
            }
        }

        return mobileContext;
    }

    /**
     * @return the OS Type
     */
    public static String getOsType() {
        return "android";
    }

    /**
     * @return the OS Version
     */
    public static String getOsVersion() {
        return android.os.Build.VERSION.RELEASE;
    }

    /**
     * @return the device model
     */
    public static String getDeviceModel() {
        return android.os.Build.MODEL;
    }

    /**
     * @return the device vendor
     */
    public static String getDeviceVendor() {
        return android.os.Build.MANUFACTURER;
    }

    /**
     * @param context the android context
     * @return a carrier name or null
     */
    public static String getCarrier(Context context) {
        TelephonyManager telephonyManager =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        if (telephonyManager != null) {
            return telephonyManager.getNetworkOperatorName();
        }
        return null;
    }

    /**
     * The function that actually fetches the Advertising ID.
     * - If called from the UI Thread will throw an Exception
     *
     * @param context the android context
     * @return the advertising id or null
     */
    public static String getAndroidIdfa(Context context) {
        try {
            Object AdvertisingInfoObject = invokeStaticMethod(
                    "com.google.android.gms.ads.identifier.AdvertisingIdClient",
                    "getAdvertisingIdInfo", new Class[]{Context.class}, context);
            return (String) invokeInstanceMethod(AdvertisingInfoObject, "getId", null);
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
    public static String getNetworkType(NetworkInfo networkInfo) {
        String networkType = null;
        if (networkInfo != null) {
            networkType = networkInfo.getTypeName().toLowerCase();
        }
        return networkType;
    }

    /**
     * Returns the network technology
     *
     * @param networkInfo The NetworkInformation object
     * @return the technology of the network
     */
    public static String getNetworkTechnology(NetworkInfo networkInfo) {
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
    public static NetworkInfo getNetworkInfo(Context context) {
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
    public static boolean mapHasKeys(Map<String, Object> map, String... keys) {
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
    public static void addToMap(String key, Object value, Map<String, Object> map) {
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
}
