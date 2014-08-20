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

public class Parameter extends com.snowplowanalytics.snowplow.tracker.core.Parameter {
    // Mobile context
    public static final String ANDROID_IDFA = "androidIdfa";
    public static final String CARRIER = "carrier";
    public static final String DEVICE_MODEL = "deviceModel";
    public static final String DEVICE_VENDOR = "deviceVendor";
    public static final String OS_VERSION = "osVersion";
    public static final String OS_TYPE = "osType";

    // Geolocation context
    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "longitude";
    public static final String ALTITUDE = "altitude";
    public static final String LATLONG_ACCURACY = "latitudeLongitudeAccuracy";
    public static final String SPEED = "speed";
    public static final String BEARING = "bearing";
}
