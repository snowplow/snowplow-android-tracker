/*
 * Copyright (c) 2015-2023 Snowplow Analytics Ltd. All rights reserved.
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

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.Build
import android.util.Base64
import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.core.tracker.Logger
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Provides basic Utilities for the Snowplow Tracker.
 */
object Util {
    private val TAG = Util::class.java.simpleName

    /**
     * Returns the current System time as a String.
     *
     * @return the system time as a string
     */
    @JvmStatic
    fun timestamp(): String {
        return System.currentTimeMillis().toString()
    }

    @JvmStatic
    fun getDateTimeFromTimestamp(timestamp: Long): String {
        val date = Date(timestamp)
        return getDateTimeFromDate(date)
    }

    @JvmStatic
    fun getDateTimeFromDate(date: Date): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale("en"))
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return dateFormat.format(date)
    }

    /**
     * Encodes a string into Base64.
     *
     * @param string the string too encode
     * @return a Base64 encoded string
     */
    @JvmStatic
    fun base64Encode(string: String): String {
        return Base64.encodeToString(string.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Generates a random UUID for each event.
     *
     * @return a UUID string
     */
    @JvmStatic
    fun uUIDString(): String {
        return UUID.randomUUID().toString()
    }

    /**
     * Count the number of bytes a string will occupy when UTF-8 encoded
     *
     * @param s the String to process
     * @return number Length of s in bytes when UTF-8 encoded
     */
    @JvmStatic
    fun getUTF8Length(s: String): Long {
        var len: Long = 0
        var i = 0
        while (i < s.length) {
            val code = s[i]
            if (code.code <= 0x7f) {
                len += 1
            } else if (code.code <= 0x7ff) {
                len += 2
            } else if (code.code in 0xd800..0xdfff) {
                // Surrogate pair: These take 4 bytes in UTF-8 and 2 chars in UCS-2
                // (Assume next char is the other [valid] half and just skip it)
                len += 4
                i++
            } else if (code.code < 0xffff) {
                len += 3
            } else {
                len += 4
            }
            i++
        }
        return len
    }

    /**
     * Checks whether or not the device
     * is online and able to communicate
     * with the outside world.
     *
     * @param context the android context
     * @return whether the tracker is online
     */
    @JvmStatic
    fun isOnline(context: Context): Boolean {
        Logger.v(TAG, "Checking tracker internet connectivity.")
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        return try {
            val ni = cm?.activeNetworkInfo
            val connected = ni != null && ni.isConnected
            Logger.d(TAG, "Tracker connection online: %s", connected)
            connected
        } catch (e: SecurityException) {
            Logger.e(TAG, "Security exception checking connection: %s", e.toString())
            true
        }
    }

    /**
     * Joins a list of Longs into a single string
     *
     * @param list the list to join
     * @return the joined list
     */
    @JvmStatic
    fun joinLongList(list: List<Long?>): String {
        var s = StringBuilder()
        for (i in list.indices) {
            val longVal = list[i]
            if (longVal != null) {
                s.append(list[i])
                if (i < list.size - 1) {
                    s.append(",")
                }
            }
        }
        if (s.toString().endsWith(",")) {
            s = StringBuilder(s.substring(0, s.length - 1))
        }
        return s.toString()
    }
    
    // --- Geo-Location Context
    
    /**
     * Returns the Geo-Location Context
     *
     * @param context the Android context
     * @return the geo-location context
     */
    @JvmStatic
    fun getGeoLocationContext(context: Context): SelfDescribingJson? {
        val location = getLastKnownLocation(context)
        
        if (location != null) {
            val pairs: MutableMap<String, Any> = HashMap()
            addToMap(Parameters.LATITUDE, location.latitude, pairs)
            addToMap(Parameters.LONGITUDE, location.longitude, pairs)
            addToMap(Parameters.ALTITUDE, location.altitude, pairs)
            addToMap(Parameters.LATLONG_ACCURACY, location.accuracy, pairs)
            addToMap(Parameters.SPEED, location.speed, pairs)
            addToMap(Parameters.BEARING, location.bearing, pairs)
            addToMap(Parameters.GEO_TIMESTAMP, System.currentTimeMillis(), pairs)
            
            if (mapHasKeys(pairs, Parameters.LATITUDE, Parameters.LONGITUDE)) {
                return SelfDescribingJson(
                    TrackerConstants.GEOLOCATION_SCHEMA, pairs
                )
            }
        }
        return null
    }

    /**
     * Returns the location of the android
     * device.
     *
     * @param context the android context
     * @return the phones Location
     */
    @SuppressLint("MissingPermission") // Suppressed as it's caught by SecurityException catch block.
    fun getLastKnownLocation(context: Context): Location? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        var location: Location? = null
        
        try {
            var locationProvider: String? = null
            if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                locationProvider = LocationManager.GPS_PROVIDER
            } else if (locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) {
                locationProvider = LocationManager.NETWORK_PROVIDER
            } else {
                val locationProviders = locationManager?.getProviders(true)
                locationProviders?.let {
                    if (it.size > 0) { locationProvider = it[0] }
                }
            }
            locationProvider?.let { 
                if (it.isNotEmpty()) {
                    location = locationManager?.getLastKnownLocation(it)
                }
            }
        } catch (ex: SecurityException) {
            Logger.e(TAG, "Exception occurred when retrieving location: %s", ex.toString())
        }
        return location
    }
    
    // --- Application Context
    
    /**
     * Returns the Application Context
     */
    @JvmStatic
    fun getApplicationContext(context: Context): SelfDescribingJson? {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionName = pInfo.versionName
            val versionCode: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toString()
            } else {
                pInfo.versionCode.toString()
            }
            if (versionName != null) {
                val pairs: MutableMap<String, Any> = HashMap()
                addToMap(Parameters.APP_VERSION, versionName, pairs)
                addToMap(Parameters.APP_BUILD, versionCode, pairs)
                return SelfDescribingJson(
                    TrackerConstants.SCHEMA_APPLICATION, pairs
                )
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Logger.e(TAG, "Failed to find application context: %s", e.message)
        }
        return null
    }
    
    // --- Context Helpers
    
    /**
     * Checks if a map contains a range of keys
     *
     * @param map the map to check
     * @param keys the keys to check
     * @return whether the map contains the keys
     */
    @JvmStatic
    fun mapHasKeys(map: Map<String, Any>, vararg keys: String): Boolean {
        for (key in keys) {
            if (!map.containsKey(key)) {
                return false
            }
        }
        return true
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
     * the key
     * @param map the map to insert the pair into
     */
    @JvmStatic
    fun addToMap(key: String?, value: Any?, map: MutableMap<String, Any>) {
        if (key != null && value != null && !key.isEmpty()) {
            map[key] = value
        }
    }

    /**
     * Converts an event map to a byte
     * array for storage.
     *
     * @param map the map containing all
     * the event parameters
     * @return the byte array or null
     */
    @JvmStatic
    fun serialize(map: Map<String, String>): ByteArray? {
        var newByteArray: ByteArray? = null
        try {
            val memOut = ByteArrayOutputStream()
            val out = ObjectOutputStream(memOut)
            out.writeObject(map)
            out.close()
            memOut.close()
            newByteArray = memOut.toByteArray()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return newByteArray
    }

    /**
     * Converts a byte array back into an
     * event map for sending.
     *
     * @param bytes the bytes to be converted
     * @return the Map or null
     */
    @JvmStatic
    fun deserializer(bytes: ByteArray): Map<String, String>? {
        var newMap: Map<String, String>? = null
        try {
            val memIn = ByteArrayInputStream(bytes)
            val `in` = ObjectInputStream(memIn)
            val map: Map<String, String>? = `in`.readObject() as? HashMap<String, String>
            `in`.close()
            memIn.close()
            newMap = map
        } catch (e: NullPointerException) {
            e.printStackTrace()
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return newMap
    }

    /**
     * Converts a map with Object values into String values.
     *
     * @param map the map to be converted
     * @return the new Map
     */
    @JvmStatic
    fun objectMapToString(map: Map<String, Any>): Map<String, String> {
        val stringsMap: MutableMap<String, String> = HashMap()
        for ((key, value) in map) {
            stringsMap[key] = value.toString()
        }
        return stringsMap
    }

    /**
     * Converts a StackTrace to a String
     *
     * @param e the Throwable to convert
     * @return the StackTrace as a string
     */
    @JvmStatic
    fun stackTraceToString(e: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        e.printStackTrace(pw)
        return sw.toString()
    }
}
