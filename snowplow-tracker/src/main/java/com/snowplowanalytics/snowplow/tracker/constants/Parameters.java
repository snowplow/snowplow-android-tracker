/*
 * Copyright (c) 2015 Snowplow Analytics Ltd. All rights reserved.
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

package com.snowplowanalytics.snowplow.tracker.constants;

/**
 * All of the keys for each type of event
 * that can be sent by the Tracker.
 */
public class Parameters {

    // General
    public static final String SCHEMA = "schema";
    public static final String DATA = "data";
    public static final String EVENT = "e";
    public static final String EID = "eid";
    public static final String TIMESTAMP = "dtm";
    public static final String SENT_TIMESTAMP = "stm";
    public static final String TRACKER_VERSION = "tv";
    public static final String APPID = "aid";
    public static final String NAMESPACE = "tna";

    public static final String UID = "uid";
    public static final String CONTEXT = "co";
    public static final String CONTEXT_ENCODED = "cx";
    public static final String UNSTRUCTURED = "ue_pr";
    public static final String UNSTRUCTURED_ENCODED = "ue_px";

    // Subject class
    public static final String PLATFORM = "p";
    public static final String RESOLUTION = "res";
    public static final String VIEWPORT = "vp";
    public static final String COLOR_DEPTH = "cd";
    public static final String TIMEZONE = "tz";
    public static final String LANGUAGE = "lang";
    public static final String IP_ADDRESS = "ip";
    public static final String USERAGENT = "ua";
    public static final String NETWORK_UID = "tnuid";
    public static final String DOMAIN_UID = "duid";

    // Page View
    public static final String PAGE_URL = "url";
    public static final String PAGE_TITLE = "page";
    public static final String PAGE_REFR = "refr";

    // Structured Event
    public static final String SE_CATEGORY = "se_ca";
    public static final String SE_ACTION = "se_ac";
    public static final String SE_LABEL = "se_la";
    public static final String SE_PROPERTY = "se_pr";
    public static final String SE_VALUE = "se_va";

    // E-commerce Transaction
    public static final String TR_ID = "tr_id";
    public static final String TR_TOTAL = "tr_tt";
    public static final String TR_AFFILIATION = "tr_af";
    public static final String TR_TAX = "tr_tx";
    public static final String TR_SHIPPING = "tr_sh";
    public static final String TR_CITY = "tr_ci";
    public static final String TR_STATE = "tr_st";
    public static final String TR_COUNTRY = "tr_co";
    public static final String TR_CURRENCY = "tr_cu";

    // Transaction Item
    public static final String TI_ITEM_ID = "ti_id";
    public static final String TI_ITEM_SKU = "ti_sk";
    public static final String TI_ITEM_NAME = "ti_nm";
    public static final String TI_ITEM_CATEGORY = "ti_ca";
    public static final String TI_ITEM_PRICE = "ti_pr";
    public static final String TI_ITEM_QUANTITY = "ti_qu";
    public static final String TI_ITEM_CURRENCY = "ti_cu";

    // Screen View
    public static final String SV_ID = "id";
    public static final String SV_NAME = "name";

    // User Timing
    public static final String UT_CATEGORY = "category";
    public static final String UT_VARIABLE = "variable";
    public static final String UT_TIMING = "timing";
    public static final String UT_LABEL = "label";

    // Mobile context
    public static final String ANDROID_IDFA = "androidIdfa";
    public static final String CARRIER = "carrier";
    public static final String DEVICE_MODEL = "deviceModel";
    public static final String DEVICE_MANUFACTURER = "deviceManufacturer";
    public static final String OS_VERSION = "osVersion";
    public static final String OS_TYPE = "osType";
    public static final String NETWORK_TYPE = "networkType";
    public static final String NETWORK_TECHNOLOGY = "networkTechnology";

    // Geolocation context
    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "longitude";
    public static final String ALTITUDE = "altitude";
    public static final String LATLONG_ACCURACY = "latitudeLongitudeAccuracy";
    public static final String SPEED = "speed";
    public static final String BEARING = "bearing";
    public static final String GEO_TIMESTAMP = "timestamp";

    // Session Context
    public static final String SESSION_USER_ID = "userId";
    public static final String SESSION_ID = "sessionId";
    public static final String SESSION_PREVIOUS_ID = "previousSessionId";
    public static final String SESSION_INDEX = "sessionIndex";
    public static final String SESSION_STORAGE = "storageMechanism";
}
