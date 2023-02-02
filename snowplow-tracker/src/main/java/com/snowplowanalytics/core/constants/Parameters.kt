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
package com.snowplowanalytics.core.constants

/**
 * All of the keys for each type of event
 * that can be sent by the Tracker.
 */
object Parameters {
    // General
    const val SCHEMA = "schema"
    const val DATA = "data"
    const val EVENT = "e"
    const val EID = "eid"
    const val DEVICE_TIMESTAMP = "dtm"
    const val TRUE_TIMESTAMP = "ttm"
    const val SENT_TIMESTAMP = "stm"
    const val TRACKER_VERSION = "tv"
    const val APPID = "aid"
    const val NAMESPACE = "tna"
    const val UID = "uid"
    const val CONTEXT = "co"
    const val CONTEXT_ENCODED = "cx"
    const val UNSTRUCTURED = "ue_pr"
    const val UNSTRUCTURED_ENCODED = "ue_px"

    // Subject class
    const val PLATFORM = "p"
    const val RESOLUTION = "res"
    const val VIEWPORT = "vp"
    const val COLOR_DEPTH = "cd"
    const val TIMEZONE = "tz"
    const val LANGUAGE = "lang"
    const val IP_ADDRESS = "ip"
    const val USERAGENT = "ua"
    const val NETWORK_UID = "tnuid"
    const val DOMAIN_UID = "duid"

    // Page View
    const val PAGE_URL = "url"
    const val PAGE_TITLE = "page"
    const val PAGE_REFR = "refr"

    // Structured Event
    const val SE_CATEGORY = "se_ca"
    const val SE_ACTION = "se_ac"
    const val SE_LABEL = "se_la"
    const val SE_PROPERTY = "se_pr"
    const val SE_VALUE = "se_va"

    // E-commerce Transaction
    const val TR_ID = "tr_id"
    const val TR_TOTAL = "tr_tt"
    const val TR_AFFILIATION = "tr_af"
    const val TR_TAX = "tr_tx"
    const val TR_SHIPPING = "tr_sh"
    const val TR_CITY = "tr_ci"
    const val TR_STATE = "tr_st"
    const val TR_COUNTRY = "tr_co"
    const val TR_CURRENCY = "tr_cu"

    // Transaction Item
    const val TI_ITEM_ID = "ti_id"
    const val TI_ITEM_SKU = "ti_sk"
    const val TI_ITEM_NAME = "ti_nm"
    const val TI_ITEM_CATEGORY = "ti_ca"
    const val TI_ITEM_PRICE = "ti_pr"
    const val TI_ITEM_QUANTITY = "ti_qu"
    const val TI_ITEM_CURRENCY = "ti_cu"

    // Screen View
    const val SV_ID = "id"
    const val SV_NAME = "name"
    const val SV_TYPE = "type"
    const val SV_PREVIOUS_NAME = "previousName"
    const val SV_PREVIOUS_ID = "previousId"
    const val SV_PREVIOUS_TYPE = "previousType"
    const val SV_TRANSITION_TYPE = "transitionType"

    // User Timing
    const val UT_CATEGORY = "category"
    const val UT_VARIABLE = "variable"
    const val UT_TIMING = "timing"
    const val UT_LABEL = "label"

    // Consent Granted
    const val CG_EXPIRY = "expiry"

    // Consent Withdrawn
    const val CW_ALL = "all"

    // Consent Document
    const val CD_DESCRIPTION = "description"
    const val CD_VERSION = "version"
    const val CD_NAME = "name"
    const val CD_ID = "id"

    // Mobile context
    const val ANDROID_IDFA = "androidIdfa"
    const val CARRIER = "carrier"
    const val DEVICE_MODEL = "deviceModel"
    const val DEVICE_MANUFACTURER = "deviceManufacturer"
    const val OS_VERSION = "osVersion"
    const val OS_TYPE = "osType"
    const val NETWORK_TYPE = "networkType"
    const val NETWORK_TECHNOLOGY = "networkTechnology"
    const val PHYSICAL_MEMORY = "physicalMemory"
    const val SYSTEM_AVAILABLE_MEMORY = "systemAvailableMemory"
    const val BATTERY_LEVEL = "batteryLevel"
    const val BATTERY_STATE = "batteryState"
    const val AVAILABLE_STORAGE = "availableStorage"
    const val TOTAL_STORAGE = "totalStorage"

    // Geolocation context
    const val LATITUDE = "latitude"
    const val LONGITUDE = "longitude"
    const val ALTITUDE = "altitude"
    const val LATLONG_ACCURACY = "latitudeLongitudeAccuracy"
    const val SPEED = "speed"
    const val BEARING = "bearing"
    const val GEO_TIMESTAMP = "timestamp"

    // Session Context
    const val SESSION_USER_ID = "userId"
    const val SESSION_ID = "sessionId"
    const val SESSION_PREVIOUS_ID = "previousSessionId"
    const val SESSION_INDEX = "sessionIndex"
    const val SESSION_EVENT_INDEX = "eventIndex"
    const val SESSION_STORAGE = "storageMechanism"
    const val SESSION_FIRST_ID = "firstEventId"
    const val SESSION_FIRST_TIMESTAMP = "firstEventTimestamp"

    // Screen Context
    const val SCREEN_NAME = "name"
    const val SCREEN_ID = "id"
    const val SCREEN_TYPE = "type"
    const val SCREEN_FRAGMENT = "fragment"
    const val SCREEN_ACTIVITY = "activity"

    // Application Context
    const val APP_VERSION = "version"
    const val APP_BUILD = "build"

    // Application Crash
    const val APP_ERROR_MESSAGE = "message"
    const val APP_ERROR_STACK = "stackTrace"
    const val APP_ERROR_THREAD_NAME = "threadName"
    const val APP_ERROR_THREAD_ID = "threadId"
    const val APP_ERROR_LANG = "programmingLanguage"
    const val APP_ERROR_LINE = "lineNumber"
    const val APP_ERROR_CLASS_NAME = "className"
    const val APP_ERROR_EXCEPTION_NAME = "exceptionName"
    const val APP_ERROR_FATAL = "isFatal"

    // Application Focus
    const val APP_FOREGROUND_INDEX = "foregroundIndex"
    const val APP_BACKGROUND_INDEX = "backgroundIndex"

    // Tracker Diagnostic
    const val DIAGNOSTIC_ERROR_MESSAGE = "message"
    const val DIAGNOSTIC_ERROR_STACK = "stackTrace"
    const val DIAGNOSTIC_ERROR_CLASS_NAME = "className"
    const val DIAGNOSTIC_ERROR_EXCEPTION_NAME = "exceptionName"
}
