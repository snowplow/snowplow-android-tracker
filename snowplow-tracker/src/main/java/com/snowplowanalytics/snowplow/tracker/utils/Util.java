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
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Collection;
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
     * The function that actually fetches the Advertising ID.
     * - If called from the UI Thread will throw an Exception
     *
     * @param context the android context
     * @return the advertising id or null
     */
    public static String getAdvertisingId(Context context) {
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
     * Returns the carrier name based
     * on the android context supplied.
     *
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
     * Returns the location of the android
     * device.
     *
     * @param context the android context
     * @return the phones Location
     */
    public static Location getLocation(Context context) {
        LocationManager locationManager =
                (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        Criteria criteria = new Criteria();
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);

        String provider = locationManager.getBestProvider(criteria, true);

        if (provider != null) {
            try {
                Location location = locationManager.getLastKnownLocation(provider);
                Logger.d(TAG, "Location found: %s", location);
                return location;
            } catch (SecurityException ex) {
                Logger.e(TAG, "Failed to retrieve location: %s", ex.toString());
                return null;
            }
        }

        Logger.e(TAG, "Location Manager provider is null.");
        return null;
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
}
