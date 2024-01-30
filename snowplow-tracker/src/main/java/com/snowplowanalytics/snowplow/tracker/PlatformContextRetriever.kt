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
package com.snowplowanalytics.snowplow.tracker

/**
 * Overrides for the values for properties of the platform context.
 */
data class PlatformContextRetriever(
    /**
     * Operating system type (e.g., ios, tvos, watchos, osx, android).
     */
    var osType: (() -> String)? = null,

    /*
     * The current version of the operating system.
     */
    var osVersion: (() -> String)? = null,

    /**
     * The manufacturer of the product/hardware.
     */
    var deviceVendor: (() -> String)? = null,

    /**
     * The end-user-visible name for the end product.
     */
    var deviceModel: (() -> String)? = null,

    /**
     * The carrier of the SIM inserted in the device.
     */
    var carrier: (() -> String?)? = null,

    /**
     * Type of network the device is connected to.
     */
    var networkType: (() -> String?)? = null,

    /**
     * Radio access technology that the device is using.
     */
    var networkTechnology: (() -> String?)? = null,

    /**
     * Advertising identifier on Android.
     */
    var androidIdfa: (() -> String?)? = null,

    /**
     * Bytes of storage remaining.
     */
    var availableStorage: (() -> Long?)? = null,

    /**
     * Total size of storage in bytes.
     */
    var totalStorage: (() -> Long?)? = null,

    /**
     * Total physical system memory in bytes.
     */
    var physicalMemory: (() -> Long?)? = null,

    /**
     * Available memory on the system in bytes (Android only).
     */
    var systemAvailableMemory: (() -> Long?)? = null,

    /**
     * Remaining battery level as an integer percentage of total battery capacity.
     */
    var batteryLevel: (() -> Int?)? = null,

    /**
     * Battery state for the device
     */
    var batteryState: (() -> String?)? = null,

    /**
     * A Boolean indicating whether the device orientation is portrait (either upright or upside down).
     */
    var isPortrait: (() -> Boolean?)? = null,

    /**
     * Screen resolution in pixels. Arrives in the form of WIDTHxHEIGHT (e.g., 1200x900). Doesn't change when device orientation changes.
     */
    var resolution: (() -> String?)? = null,

    /**
     * Scale factor used to convert logical coordinates to device coordinates of the screen (uses UIScreen.scale on iOS).
     */
    var scale: (() -> Float?)? = null,

    /**
     * System language currently used on the device (ISO 639).
     */
    var language: (() -> String)? = null,

    /**
     * Android vendor ID scoped to the set of apps published under the same Google Play developer account (see https://developer.android.com/training/articles/app-set-id).
     */
    var appSetId: (() -> String)? = null,

    /**
     * Scope of the `appSetId`. Can be scoped to the app or to a developer account on an app store (all apps from the same developer on the same device will have the same ID).
     */
    var appSetIdScope: (() -> String)? = null
)
