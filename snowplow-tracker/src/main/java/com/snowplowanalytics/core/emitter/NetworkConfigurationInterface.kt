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
package com.snowplowanalytics.core.emitter

import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.network.NetworkConnection
import com.snowplowanalytics.snowplow.network.Protocol

import okhttp3.CookieJar
import okhttp3.OkHttpClient

interface NetworkConfigurationInterface {
    /** URL (without schema/protocol) used to send events to the collector. */
    val endpoint: String?
    /** Method used to send events to the collector. */
    val method: HttpMethod?
    /** Protocol used to send events to the collector. */
    val protocol: Protocol?
    /** Custom `NetworkConnection` instance to use for sending events. */
    val networkConnection: NetworkConnection?
    /** A custom path which will be added to the endpoint URL to specify the complete URL of the collector when paired with the POST method. */
    val customPostPath: String?
    /**
     * The timeout set for the requests to the collector.
     * The maximum timeout for emitting events. If emit time exceeds this value
     * TimeOutException will be thrown.
     */
    val timeout: Int?
    /**
     * An OkHttp client that will be used in the emitter. You can provide your
     * own if you want to share your Singleton client's interceptors, connection pool etc.
     * By default a new [OkHttpClient] is created when the tracker is instantiated.
     */
    val okHttpClient: OkHttpClient?
    /**
     * An OkHttp cookie jar to override the default
     * [CollectorCookieJar](com.snowplowanalytics.snowplow.network.CollectorCookieJar)
     * that stores cookies in SharedPreferences.
     * A cookie jar provided here will be ignored if a custom `okHttpClient` is configured.
     */
    val okHttpCookieJar: CookieJar?
    /** Custom headers to add to HTTP requests to the collector. */
    val requestHeaders: Map<String, String>?
}
