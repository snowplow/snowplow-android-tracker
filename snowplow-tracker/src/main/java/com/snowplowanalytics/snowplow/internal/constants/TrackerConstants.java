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

package com.snowplowanalytics.snowplow.internal.constants;

/**
 * Constants which apply to schemas, event types
 * and sending protocols.
 */
public class TrackerConstants {
    public static final String PROTOCOL_VENDOR = "com.snowplowanalytics.snowplow";
    public static final String PROTOCOL_VERSION = "tp2";

    public static final String SCHEMA_PAYLOAD_DATA = "iglu:com.snowplowanalytics.snowplow/payload_data/jsonschema/1-0-4";
    public static final String SCHEMA_CONTEXTS = "iglu:com.snowplowanalytics.snowplow/contexts/jsonschema/1-0-1";
    public static final String SCHEMA_UNSTRUCT_EVENT = "iglu:com.snowplowanalytics.snowplow/unstruct_event/jsonschema/1-0-0";
    public static final String SCHEMA_SCREEN_VIEW = "iglu:com.snowplowanalytics.mobile/screen_view/jsonschema/1-0-0";
    public static final String SCHEMA_USER_TIMINGS = "iglu:com.snowplowanalytics.snowplow/timing/jsonschema/1-0-0";
    public static final String SCHEMA_CONSENT_GRANTED = "iglu:com.snowplowanalytics.snowplow/consent_granted/jsonschema/1-0-0";
    public static final String SCHEMA_CONSENT_WITHDRAWN = "iglu:com.snowplowanalytics.snowplow/consent_withdrawn/jsonschema/1-0-0";
    public static final String SCHEMA_CONSENT_DOCUMENT = "iglu:com.snowplowanalytics.snowplow/consent_document/jsonschema/1-0-0";
    public static final String GEOLOCATION_SCHEMA = "iglu:com.snowplowanalytics.snowplow/geolocation_context/jsonschema/1-1-0";
    public static final String MOBILE_SCHEMA = "iglu:com.snowplowanalytics.snowplow/mobile_context/jsonschema/1-0-2";
    public static final String SESSION_SCHEMA = "iglu:com.snowplowanalytics.snowplow/client_session/jsonschema/1-0-2";
    public static final String APPLICATION_ERROR_SCHEMA = "iglu:com.snowplowanalytics.snowplow/application_error/jsonschema/1-0-0";
    public static final String SCHEMA_SCREEN = "iglu:com.snowplowanalytics.mobile/screen/jsonschema/1-0-0";
    public static final String SCHEMA_APPLICATION_INSTALL = "iglu:com.snowplowanalytics.mobile/application_install/jsonschema/1-0-0";
    public static final String SCHEMA_APPLICATION = "iglu:com.snowplowanalytics.mobile/application/jsonschema/1-0-0";
    public static final String SCHEMA_GDPR = "iglu:com.snowplowanalytics.snowplow/gdpr/jsonschema/1-0-0";
    public static final String SCHEMA_DIAGNOSTIC_ERROR = "iglu:com.snowplowanalytics.snowplow/diagnostic_error/jsonschema/1-0-0";

    public static final String POST_CONTENT_TYPE = "application/json; charset=utf-8";

    public static final String EVENT_PAGE_VIEW = "pv";
    public static final String EVENT_STRUCTURED = "se";
    public static final String EVENT_UNSTRUCTURED = "ue";
    public static final String EVENT_ECOMM = "tr";
    public static final String EVENT_ECOMM_ITEM = "ti";

    public static final String SESSION_STATE = "session_state";
    public static final String SNOWPLOW_SESSION_VARS = "snowplow_session_vars";
    public static final String SNOWPLOW_GENERAL_VARS = "snowplow_general_vars";
    public static final String INSTALLATION_USER_ID = "SPInstallationUserId";

    public static final String INSTALLED_BEFORE = "installed_before";
    public static final String INSTALL_TIMESTAMP = "install_timestamp";

    public static final String COOKIE_PERSISTANCE = "cookie_persistance";
}
