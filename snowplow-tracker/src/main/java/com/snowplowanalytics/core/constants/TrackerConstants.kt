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
 * Constants which apply to schemas, event types
 * and sending protocols.
 */
object TrackerConstants {
    const val PROTOCOL_VENDOR = "com.snowplowanalytics.snowplow"
    const val PROTOCOL_VERSION = "tp2"
    const val SCHEMA_PAYLOAD_DATA =
        "iglu:com.snowplowanalytics.snowplow/payload_data/jsonschema/1-0-4"
    const val SCHEMA_CONTEXTS = "iglu:com.snowplowanalytics.snowplow/contexts/jsonschema/1-0-1"
    const val SCHEMA_UNSTRUCT_EVENT =
        "iglu:com.snowplowanalytics.snowplow/unstruct_event/jsonschema/1-0-0"
    const val SCHEMA_SCREEN_VIEW = "iglu:com.snowplowanalytics.mobile/screen_view/jsonschema/1-0-0"
    const val SCHEMA_USER_TIMINGS = "iglu:com.snowplowanalytics.snowplow/timing/jsonschema/1-0-0"
    const val SCHEMA_CONSENT_GRANTED =
        "iglu:com.snowplowanalytics.snowplow/consent_granted/jsonschema/1-0-0"
    const val SCHEMA_CONSENT_WITHDRAWN =
        "iglu:com.snowplowanalytics.snowplow/consent_withdrawn/jsonschema/1-0-0"
    const val SCHEMA_CONSENT_DOCUMENT =
        "iglu:com.snowplowanalytics.snowplow/consent_document/jsonschema/1-0-0"
    const val GEOLOCATION_SCHEMA =
        "iglu:com.snowplowanalytics.snowplow/geolocation_context/jsonschema/1-1-0"
    const val MOBILE_SCHEMA = "iglu:com.snowplowanalytics.snowplow/mobile_context/jsonschema/1-0-3"
    const val SESSION_SCHEMA = "iglu:com.snowplowanalytics.snowplow/client_session/jsonschema/1-0-2"
    const val APPLICATION_ERROR_SCHEMA =
        "iglu:com.snowplowanalytics.snowplow/application_error/jsonschema/1-0-0"
    const val SCHEMA_SCREEN = "iglu:com.snowplowanalytics.mobile/screen/jsonschema/1-0-0"
    const val SCHEMA_APPLICATION_INSTALL =
        "iglu:com.snowplowanalytics.mobile/application_install/jsonschema/1-0-0"
    const val SCHEMA_APPLICATION = "iglu:com.snowplowanalytics.mobile/application/jsonschema/1-0-0"
    const val SCHEMA_GDPR = "iglu:com.snowplowanalytics.snowplow/gdpr/jsonschema/1-0-0"
    const val SCHEMA_DIAGNOSTIC_ERROR =
        "iglu:com.snowplowanalytics.snowplow/diagnostic_error/jsonschema/1-0-0"
    const val SCHEMA_ECOMMERCE_ACTION = 
        "iglu:com.snowplowanalytics.snowplow.ecommerce/snowplow_ecommerce_action/jsonschema/1-0-1"
    const val SCHEMA_ECOMMERCE_PRODUCT =
        "iglu:com.snowplowanalytics.snowplow.ecommerce/product/jsonschema/1-0-0"
    const val SCHEMA_ECOMMERCE_CART =
        "iglu:com.snowplowanalytics.snowplow.ecommerce/cart/jsonschema/1-0-0"
    const val SCHEMA_ECOMMERCE_TRANSACTION =
        "iglu:com.snowplowanalytics.snowplow.ecommerce/transaction/jsonschema/1-0-0"
    const val SCHEMA_ECOMMERCE_CHECKOUT_STEP =
        "iglu:com.snowplowanalytics.snowplow.ecommerce/checkout_step/jsonschema/1-0-0"
    const val SCHEMA_ECOMMERCE_PROMOTION =
        "iglu:com.snowplowanalytics.snowplow.ecommerce/promotion/jsonschema/1-0-0"
    const val SCHEMA_ECOMMERCE_REFUND =
        "iglu:com.snowplowanalytics.snowplow.ecommerce/refund/jsonschema/1-0-0"
    const val SCHEMA_ECOMMERCE_USER =
        "iglu:com.snowplowanalytics.snowplow.ecommerce/user/jsonschema/1-0-0"
    const val SCHEMA_ECOMMERCE_PAGE =
        "iglu:com.snowplowanalytics.snowplow.ecommerce/page/jsonschema/1-0-0"
    const val POST_CONTENT_TYPE = "application/json; charset=utf-8"
    const val EVENT_PAGE_VIEW = "pv"
    const val EVENT_STRUCTURED = "se"
    const val EVENT_UNSTRUCTURED = "ue"
    const val EVENT_ECOMM = "tr"
    const val EVENT_ECOMM_ITEM = "ti"
    const val SESSION_STATE = "session_state"
    const val SNOWPLOW_SESSION_VARS = "snowplow_session_vars"
    const val SNOWPLOW_GENERAL_VARS = "snowplow_general_vars"
    const val INSTALLATION_USER_ID = "SPInstallationUserId"
    const val INSTALLED_BEFORE = "installed_before"
    const val INSTALL_TIMESTAMP = "install_timestamp"
    const val COOKIE_PERSISTANCE = "cookie_persistance"
}
