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
package com.snowplowanalytics.snowplow.configuration

/**
 * Optional properties tracked in the platform context entity
 */
enum class PlatformContextProperty {
    /// The carrier of the SIM inserted in the device
    CARRIER,
    /// Type of network the device is connected to
    NETWORK_TYPE,
    /// Radio access technology that the device is using
    NETWORK_TECHNOLOGY,
    /// Advertising identifier on Android
    ANDROID_IDFA,
    /// Total physical system memory in bytes
    PHYSICAL_MEMORY,
    /// Available memory on the system in bytes (Android only)
    SYSTEM_AVAILABLE_MEMORY,
    /// Remaining battery level as an integer percentage of total battery capacity
    BATTERY_LEVEL,
    /// Battery state for the device
    BATTERY_STATE,
    /// Bytes of storage remaining
    AVAILABLE_STORAGE,
    /// Total size of storage in bytes
    TOTAL_STORAGE,
    /// A Boolean indicating whether the device orientation is portrait (either upright or upside down)
    IS_PORTRAIT,
    /// Screen resolution in pixels. Arrives in the form of WIDTHxHEIGHT (e.g., 1200x900). Doesn't change when device orientation changes
    RESOLUTION,
    /// Scale factor used to convert logical coordinates to device coordinates of the screen (uses DisplayMetrics.density on Android)
    SCALE,
    /// System language currently used on the device (ISO 639)
    LANGUAGE,
    /// Android vendor ID scoped to the set of apps published under the same Google Play developer account (see https://developer.android.com/training/articles/app-set-id)
    APP_SET_ID,
    /// Scope of the `appSetId`. Can be scoped to the app or to a developer account on an app store (all apps from the same developer on the same device will have the same ID)
    APP_SET_ID_SCOPE
}
