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
    
    // Snowplow Ecommerce
    const val ECOMM_TYPE = "type"
    const val ECOMM_NAME = "name"
    
    // Snowplow Ecommerce Product
    const val ECOMM_PRODUCT_ID = "id"
    const val ECOMM_PRODUCT_NAME = "name"
    const val ECOMM_PRODUCT_CATEGORY = "category"
    const val ECOMM_PRODUCT_PRICE = "price"
    const val ECOMM_PRODUCT_LIST_PRICE = "list_price"
    const val ECOMM_PRODUCT_QUANTITY = "quantity"
    const val ECOMM_PRODUCT_SIZE = "size"
    const val ECOMM_PRODUCT_VARIANT = "variant"
    const val ECOMM_PRODUCT_BRAND = "brand"
    const val ECOMM_PRODUCT_INVENTORY_STATUS = "inventory_status"
    const val ECOMM_PRODUCT_POSITION = "position"
    const val ECOMM_PRODUCT_CURRENCY = "currency"
    const val ECOMM_PRODUCT_CREATIVE_ID = "creative_id"
    
    // Snowplow Ecommerce Cart
    const val ECOMM_CART_ID = "cart_id"
    const val ECOMM_CART_VALUE = "total_value"
    const val ECOMM_CART_CURRENCY = "currency"
    
    // Snowplow Ecommerce Transaction
    const val ECOMM_TRANSACTION_ID = "transaction_id"
    const val ECOMM_TRANSACTION_REVENUE = "revenue"
    const val ECOMM_TRANSACTION_CURRENCY = "currency"
    const val ECOMM_TRANSACTION_PAYMENT_METHOD = "payment_method"
    const val ECOMM_TRANSACTION_QUANTITY = "total_quantity"
    const val ECOMM_TRANSACTION_TAX = "tax"
    const val ECOMM_TRANSACTION_SHIPPING = "shipping"
    const val ECOMM_TRANSACTION_DISCOUNT_CODE = "discount_code"
    const val ECOMM_TRANSACTION_DISCOUNT_AMOUNT = "discount_amount"
    const val ECOMM_TRANSACTION_CREDIT_ORDER = "credit_order"

    // Snowplow Ecommerce Transaction Error
    const val ECOMM_TRANSACTION_ERROR_CODE = "error_code"
    const val ECOMM_TRANSACTION_ERROR_SHORTCODE = "error_shortcode"
    const val ECOMM_TRANSACTION_ERROR_DESCRIPTION = "error_description"
    const val ECOMM_TRANSACTION_ERROR_TYPE = "error_type"
    const val ECOMM_TRANSACTION_ERROR_RESOLUTION = "resolution"

    // Snowplow Ecommerce Checkout Step
    const val ECOMM_CHECKOUT_STEP = "step"
    const val ECOMM_CHECKOUT_SHIPPING_POSTCODE = "shipping_postcode"
    const val ECOMM_CHECKOUT_BILLING_POSTCODE = "billing_postcode"
    const val ECOMM_CHECKOUT_SHIPPING_ADDRESS = "shipping_full_address"
    const val ECOMM_CHECKOUT_BILLING_ADDRESS = "billing_full_address"
    const val ECOMM_CHECKOUT_DELIVERY_PROVIDER = "delivery_provider"
    const val ECOMM_CHECKOUT_DELIVERY_METHOD = "delivery_method"
    const val ECOMM_CHECKOUT_COUPON_CODE = "coupon_code"
    const val ECOMM_CHECKOUT_ACCOUNT_TYPE = "account_type"
    const val ECOMM_CHECKOUT_PAYMENT_METHOD = "payment_method"
    const val ECOMM_CHECKOUT_PROOF_OF_PAYMENT = "proof_of_payment"
    const val ECOMM_CHECKOUT_MARKETING_OPT_IN = "marketing_opt_in"

    // Snowplow Ecommerce Promo
    const val ECOMM_PROMO_ID = "id"
    const val ECOMM_PROMO_NAME = "name"
    const val ECOMM_PROMO_PRODUCT_IDS = "product_ids"
    const val ECOMM_PROMO_POSITION = "position"
    const val ECOMM_PROMO_CREATIVE_ID = "creative_id"
    const val ECOMM_PROMO_TYPE = "type"
    const val ECOMM_PROMO_SLOT = "slot"

    // Snowplow Ecommerce Refund
    const val ECOMM_REFUND_ID = "transaction_id"
    const val ECOMM_REFUND_CURRENCY = "currency"
    const val ECOMM_REFUND_AMOUNT = "refund_amount"
    const val ECOMM_REFUND_REASON = "refund_reason"

    // Snowplow Ecommerce Screen/Page
    const val ECOMM_SCREEN_TYPE = "type"
    const val ECOMM_SCREEN_LANGUAGE = "language"
    const val ECOMM_SCREEN_LOCALE = "locale"

    // Snowplow Ecommerce User
    const val ECOMM_USER_ID = "id"
    const val ECOMM_USER_GUEST = "is_guest"
    const val ECOMM_USER_EMAIL = "email"

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
    const val IS_PORTRAIT = "isPortrait"
    const val MOBILE_RESOLUTION = "resolution"
    const val MOBILE_LANGUAGE = "language"
    const val MOBILE_SCALE = "scale"
    const val APP_SET_ID = "appSetId"
    const val APP_SET_ID_SCOPE = "appSetIdScope"

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
    
    // Page Pings (for WebView tracking)
    const val PING_XOFFSET_MIN = "pp_mix"
    const val PING_XOFFSET_MAX = "pp_max"
    const val PING_YOFFSET_MIN = "pp_miy"
    const val PING_YOFFSET_MAX = "pp_may"
}
