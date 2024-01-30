/*
 * Copyright (c) 2015-present Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.core.tracker

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.tracker.Logger.v
import com.snowplowanalytics.snowplow.util.Size
import java.util.*

/**
 * Provides Subject information for each
 * event sent from the Snowplow Tracker.
 */
class Subject(context: Context, config: SubjectConfigurationInterface?) {
    private val standardPairs = HashMap<String, String?>()
    
    var userId: String? = null
        /**
         * Sets the subjects userId
         * @param userId a user id string
         */
        set(userId) {
            field = userId
            standardPairs[Parameters.UID] = userId
        }
    
    var networkUserId: String? = null
        /**
         * User inputted Network User ID for the subject.
         * This overrides the network user ID set by the Collector in response Cookies.
         * @param networkUserId a network user id
         */
        set(networkUserId) {
            if (networkUserId == null) {
                return
            }

            field = networkUserId
            standardPairs[Parameters.NETWORK_UID] = networkUserId
        }
    
    var domainUserId: String? = null
        /**
         * User inputted Domain User Id for the
         * subject.
         * @param domainUserId a domain user id
         */
        set(domainUserId) {
            if (domainUserId == null) {
                return
            }

            field = domainUserId
            standardPairs[Parameters.DOMAIN_UID] = domainUserId
        }
    
    var useragent: String? = null
        /**
         * User inputted useragent for the
         * subject.
         * @param useragent a useragent
         */
        set(useragent) {
            if (useragent == null) {
                return
            }

            field = useragent
            standardPairs[Parameters.USERAGENT] = useragent
        }
    
    var ipAddress: String? = null
        /**
         * User inputted ip address for the
         * subject.
         * @param ipAddress an ip address
         */
        set(ipAddress) {
            if (ipAddress == null) {
                return
            }

            field = ipAddress
            standardPairs[Parameters.IP_ADDRESS] = ipAddress
        }
    
    var timezone: String? = null
        /**
        * User inputted timezone
        * @param timezone a valid timezone
        */
        set(timezone) {
            if (timezone == null) {
                return
            }
            
            field = timezone
            standardPairs[Parameters.TIMEZONE] = timezone
        }
    
    var language: String? = null
        /**
         * User inputted language for the
         * subject.
         * @param language language setting
         */
        set(language) {
            if (language == null) {
                return
            }

            field = language
            standardPairs[Parameters.LANGUAGE] = language
        }
    
    
    var screenResolution: Size? = null
        /**
         * Sets a custom screen resolution based
         * on user inputted width and height.
         *
         * Measured in pixels: 1920x1080
         * @param size a Size object
         */
        set(size) {
            if (size == null) {
                return
            }

            field = size
            val res = size.width.toString() + "x" + size.height.toString()
            standardPairs[Parameters.RESOLUTION] = res
        }
    
    var screenViewPort: Size? = null
        /**
         * Sets the view port resolution
         *
         * Measured in pixels: 1280x1024
         * @param size a Size object
         */
        set(size) {
            if (size == null) {
                return
            }

            field = size
            val res = size.width.toString() + "x" + size.height.toString()
            standardPairs[Parameters.VIEWPORT] = res
        }
    
    var colorDepth: Int? = null
        /**
         * user defined color depth.
         *
         * Measure as an integer
         * @param depth the color depth
         */
        set(depth) {
            if (depth == null) {
                return
            }

            field = depth
            standardPairs[Parameters.COLOR_DEPTH] = depth.toString()
        }
    

    init {
        setDefaultTimezone()
        setDefaultLanguage()
        setDefaultScreenResolution(context)
        
        if (config != null) {
            config.userId?.let { userId = it }
            config.networkUserId?.let { networkUserId = it }
            config.domainUserId?.let { domainUserId = it }
            config.useragent?.let { useragent = it }
            config.ipAddress?.let { ipAddress = it }
            config.timezone?.let { timezone = it }
            config.language?.let { language = it }
            config.screenResolution?.let { screenResolution = it }
            config.screenViewPort?.let { screenViewPort = it }
            config.colorDepth?.let { colorDepth = it }
        }
        v(TAG, "Subject created successfully.")
    }
    
    // Default information setters
    /**
     * Sets the default timezone of the
     * device.
     */
    private fun setDefaultTimezone() {
        val tz = Calendar.getInstance().timeZone
        timezone = tz.id
    }

    /**
     * Sets the default language of the
     * device.
     */
    private fun setDefaultLanguage() {
        language = (Locale.getDefault().displayLanguage)
    }

    /**
     * Sets the default screen resolution
     * of the device the Tracker is running
     * on.
     *
     * @param context the android context
     */
    private fun setDefaultScreenResolution(context: Context) {
        try {
            screenResolution = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val metrics =
                    context.getSystemService(WindowManager::class.java).currentWindowMetrics
                Size(metrics.bounds.width(), metrics.bounds.height())
            } else {
                val windowManager =
                    context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                val display = windowManager?.defaultDisplay
                val metrics = if (display != null) {
                    DisplayMetrics().also { display.getRealMetrics(it) }
                } else {
                    Resources.getSystem().displayMetrics
                }
                Size(metrics.widthPixels, metrics.heightPixels)
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to set default screen resolution.")
        }
    }

    /**
     * @param userAnonymisation Whether to anonymize user identifiers
     * @return the standard subject pairs
     */
    fun getSubject(userAnonymisation: Boolean): Map<String, String?> {
        if (userAnonymisation) {
            val pairsCopy: MutableMap<String, String?> = HashMap(standardPairs)
            pairsCopy.remove(Parameters.UID)
            pairsCopy.remove(Parameters.DOMAIN_UID)
            pairsCopy.remove(Parameters.NETWORK_UID)
            pairsCopy.remove(Parameters.IP_ADDRESS)
            return pairsCopy
        }
        return standardPairs
    }

    companion object {
        private val TAG = Subject::class.java.simpleName
    }
}
