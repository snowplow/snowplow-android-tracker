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

package com.snowplowanalytics.snowplow.internal.utils;

import static android.content.Context.ACTIVITY_SERVICE;
import static com.snowplowanalytics.snowplow.configuration.TrackerConfiguration.TAG;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.StatFs;
import android.telephony.TelephonyManager;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.internal.tracker.Logger;

import java.lang.reflect.Method;

public class DeviceInfoMonitor {

    @NonNull
    public String getOsType() {
        return "android";
    }

    @NonNull
    public String getOsVersion() {
        return android.os.Build.VERSION.RELEASE;
    }

    @NonNull
    public String getDeviceModel() {
        return android.os.Build.MODEL;
    }

    @NonNull
    public String getDeviceVendor() {
        return android.os.Build.MANUFACTURER;
    }

    @Nullable
    public String getCarrier(@NonNull Context context) {
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
    public String getAndroidIdfa(@NonNull Context context) {
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

    @NonNull
    public String getNetworkType(@Nullable NetworkInfo networkInfo) {
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

    @Nullable
    public String getNetworkTechnology(@Nullable NetworkInfo networkInfo) {
        String networkTech = null;
        if (networkInfo != null) {
            String networkType = networkInfo.getTypeName();
            if (networkType.equalsIgnoreCase("MOBILE")) {
                networkTech = networkInfo.getSubtypeName();
            }
        }
        return networkTech;
    }

    @Nullable
    public NetworkInfo getNetworkInfo(@NonNull Context context) {
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

    /**
     * @param context The Android context
     * @return Total physical memory on the device in bytes
     */
    public long getPhysicalMemory(@NonNull Context context) {
        ActivityManager.MemoryInfo mi = getMemoryInfo(context);
        return mi.totalMem;
    }

    /**
     * @param context The Android context
     * @return Currently available system memory in bytes
     */
    public long getSystemAvailableMemory(@NonNull Context context) {
        ActivityManager.MemoryInfo mi = getMemoryInfo(context);
        return mi.availMem;
    }

    /**
     * @param context The Android context
     * @return A pair containing the current battery state (either "full", "charging", "unplugged" or NULL if unknown) and the battery level (0 to 100)
     */
    @Nullable
    public Pair<String, Integer> getBatteryStateAndLevel(@NonNull Context context) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryIntent = context.registerReceiver(null, ifilter);
        if (batteryIntent == null) {
            return null;
        }

        int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if (status == -1 || level == -1 || scale == -1) {
            return null;
        }

        String batteryState;
        switch (status) {
            case BatteryManager.BATTERY_HEALTH_UNKNOWN:
                batteryState = null;
                break;
            case BatteryManager.BATTERY_STATUS_FULL:
                batteryState = "full";
                break;
            case BatteryManager.BATTERY_STATUS_CHARGING:
                batteryState = "charging";
                break;
            default:
                batteryState = "unplugged";
                break;
        }

        int batteryLevel = (int) (level * 100 / (float)scale);

        return new Pair<>(batteryState, batteryLevel);
    }

    /**
     * Checked in the user data directory.
     * @return Currently available storage on disk in bytes
     */
    public long getAvailableStorage() {
        StatFs statFs = new StatFs(Environment.getDataDirectory().getPath());
        return statFs.getFreeBytes();
    }

    /**
     * Checked in the user data directory.
     * @return Total storage on disk in bytes
     */
    public long getTotalStorage() {
        StatFs statFs = new StatFs(Environment.getDataDirectory().getPath());
        return statFs.getTotalBytes();
    }

    // --- PRIVATE

    @NonNull
    private ActivityManager.MemoryInfo getMemoryInfo(@NonNull Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(mi);
        return mi;
    }

    // --- PRIVATE STATIC FUNCTIONS FOR INVOKING METHODS VIA REFLECTION

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
