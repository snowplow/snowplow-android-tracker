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
package com.snowplowanalytics.core.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.telephony.TelephonyManager
import android.util.Pair
import com.snowplowanalytics.core.tracker.Logger
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration
import java.util.*

open class DeviceInfoMonitor {
    open val osType: String
        get() = "android"
    open val osVersion: String
        get() = Build.VERSION.RELEASE
    open val deviceModel: String
        get() = Build.MODEL
    open val deviceVendor: String
        get() = Build.MANUFACTURER

    open fun getCarrier(context: Context): String? {
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val carrierName = telephonyManager.networkOperatorName
        if (carrierName != "") {
            return carrierName
        }
        return null
    }

    /**
     * The function that actually fetches the Advertising ID.
     * - If called from the UI Thread will throw an Exception
     *
     * @param context the android context
     * @return an empty string if limited tracking is on otherwise the advertising id or null
     */
    open fun getAndroidIdfa(context: Context): String? {
        return try {
            val advertisingInfoObject = invokeStaticMethod(
                "com.google.android.gms.ads.identifier.AdvertisingIdClient",
                "getAdvertisingIdInfo", arrayOf(Context::class.java), context
            )
            val limitedTracking = advertisingInfoObject?.let {
                invokeInstanceMethod(
                    it,
                    "isLimitAdTrackingEnabled", null
                )
            } as Boolean
            if (limitedTracking) {
                ""
            } else invokeInstanceMethod(advertisingInfoObject, "getId", null) as String
        } catch (e: Exception) {
            Logger.e(
                TrackerConfiguration.TAG,
                "Exception getting the Advertising ID: %s",
                e.toString()
            )
            null
        }
    }

    open fun getNetworkType(networkInfo: NetworkInfo?): String {
        var networkType = "offline"
        if (networkInfo != null) {
            val maybeNetworkType = networkInfo.typeName.lowercase(Locale.getDefault())
            when (maybeNetworkType) {
                "mobile", "wifi" -> networkType = maybeNetworkType
                else -> {}
            }
        }
        return networkType
    }

    open fun getNetworkTechnology(networkInfo: NetworkInfo?): String? {
        var networkTech: String? = null
        if (networkInfo != null) {
            val networkType = networkInfo.typeName
            if (networkType.equals("MOBILE", ignoreCase = true)) {
                networkTech = networkInfo.subtypeName
            }
        }
        return networkTech
    }

    fun getNetworkInfo(context: Context): NetworkInfo? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var ni: NetworkInfo? = null
        try {
            val maybeNi = cm.activeNetworkInfo
            if (maybeNi != null && maybeNi.isConnected) {
                ni = maybeNi
            }
        } catch (e: SecurityException) {
            Logger.e(
                TrackerConfiguration.TAG,
                "Security exception getting NetworkInfo: %s",
                e.toString()
            )
        }
        return ni
    }

    /**
     * @param context The Android context
     * @return Total physical memory on the device in bytes
     */
    open fun getPhysicalMemory(context: Context): Long {
        val mi = getMemoryInfo(context)
        return mi.totalMem
    }

    /**
     * @param context The Android context
     * @return Currently available system memory in bytes
     */
    open fun getSystemAvailableMemory(context: Context): Long {
        val mi = getMemoryInfo(context)
        return mi.availMem
    }

    /**
     * @param context The Android context
     * @return A pair containing the current battery state (either "full", "charging", "unplugged" or NULL if unknown) and the battery level (0 to 100)
     */
    open fun getBatteryStateAndLevel(context: Context): Pair<String?, Int>? {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryIntent = context.registerReceiver(null, ifilter) ?: return null
        val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (status == -1 || level == -1 || scale == -1) {
            return null
        }
        val batteryState: String?
        batteryState = when (status) {
            BatteryManager.BATTERY_HEALTH_UNKNOWN -> null
            BatteryManager.BATTERY_STATUS_FULL -> "full"
            BatteryManager.BATTERY_STATUS_CHARGING -> "charging"
            else -> "unplugged"
        }
        val batteryLevel = (level * 100 / scale.toFloat()).toInt()
        return Pair(batteryState, batteryLevel)
    }

    /**
     * Checked in the user data directory.
     * @return Currently available storage on disk in bytes
     */
    open val availableStorage: Long
        get() {
            val statFs = StatFs(Environment.getDataDirectory().path)
            return statFs.freeBytes
        }

    /**
     * Checked in the user data directory.
     * @return Total storage on disk in bytes
     */
    open val totalStorage: Long
        get() {
            val statFs = StatFs(Environment.getDataDirectory().path)
            return statFs.totalBytes
        }

    // --- PRIVATE
    private fun getMemoryInfo(context: Context): ActivityManager.MemoryInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(mi)
        return mi
    }

    companion object {
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
        @Throws(Exception::class)
        private fun invokeInstanceMethod(
            instance: Any, methodName: String,
            cArgs: Array<Class<*>>?, vararg args: Any
        ): Any? {
            val classObject: Class<*> = instance.javaClass
            return invokeMethod(classObject, methodName, instance, cArgs, *args)
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
        @Throws(Exception::class)
        private fun invokeStaticMethod(
            className: String, methodName: String,
            cArgs: Array<Class<*>>, vararg args: Any
        ): Any? {
            val classObject = Class.forName(className)
            return invokeMethod(classObject, methodName, null, cArgs, *args)
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
        @Throws(Exception::class)
        private fun invokeMethod(
            classObject: Class<*>, methodName: String, instance: Any?,
            cArgs: Array<Class<*>>?, vararg args: Any
        ): Any? {
            val methodObject = if (cArgs == null) {
                classObject.getMethod(methodName)
            } else {
                classObject.getMethod(methodName, *cArgs)
            }
            return methodObject.invoke(instance, *args)
        }
    }
}
