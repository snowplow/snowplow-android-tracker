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
package com.snowplowanalytics.snowplow.configuration

import android.net.Uri
import com.snowplowanalytics.core.emitter.NetworkConfigurationInterface
import com.snowplowanalytics.core.tracker.Logger
import com.snowplowanalytics.core.emitter.EmitterDefaults
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.network.NetworkConnection
import com.snowplowanalytics.snowplow.network.Protocol
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.util.*

/**
 * Represents the network communication configuration
 * allowing the tracker to be able to send events to the Snowplow collector.
 * 
 * Default values:
 * 
 * method: [HttpMethod.POST]
 * 
 * protocol: [Protocol.HTTPS]
 * 
 * timeout: 5 seconds
 */
class NetworkConfiguration : NetworkConfigurationInterface, Configuration {

    /**
     * Fallback configuration to read from in case requested values are not present in this configuration.
     */
    internal var sourceConfig: NetworkConfiguration? = null

    private var _endpoint: String? = null
    /**
     * @return URL (without schema/protocol) used to send events to the collector.
     */
    override var endpoint: String?
        get() = _endpoint ?: sourceConfig?.endpoint
        set(value) { _endpoint = value }

    private var _method: HttpMethod? = null
    /**
     * @return Method (GET or POST) used to send events to the collector.
     */
    override var method: HttpMethod
        get() = _method ?: sourceConfig?.method ?: EmitterDefaults.httpMethod
        set(value) { _method = value }

    private var _protocol: Protocol? = null
    /**
     * @return Protocol (HTTP or HTTPS) used to send events to the collector.
     */
    override var protocol: Protocol?
        get() = _protocol ?: sourceConfig?.protocol ?: EmitterDefaults.httpProtocol
        set(value) { _protocol = value }

    private var _networkConnection: NetworkConnection? = null
    override var networkConnection: NetworkConnection?
        get() = _networkConnection ?: sourceConfig?.networkConnection
        set(value) { _networkConnection = value }

    private var _customPostPath: String? = null
    override var customPostPath: String?
        get() = _customPostPath ?: sourceConfig?.customPostPath
        set(value) { _customPostPath = value }

    private var _timeout: Int? = null
    override var timeout: Int?
        get() = _timeout ?: sourceConfig?.timeout ?: EmitterDefaults.emitTimeout
        set(value) { _timeout = value }

    private var _okHttpClient: OkHttpClient? = null
    override var okHttpClient: OkHttpClient?
        get() = _okHttpClient ?: sourceConfig?.okHttpClient
        set(value) { _okHttpClient = value }

    private var _okHttpCookieJar: CookieJar? = null
    override var okHttpCookieJar: CookieJar?
        get() = _okHttpCookieJar ?: sourceConfig?.okHttpCookieJar
        set(value) { _okHttpCookieJar = value }

    private var _requestHeaders: Map<String, String>? = null
    override var requestHeaders: Map<String, String>?
        get() = _requestHeaders ?: sourceConfig?.requestHeaders
        set(value) { _requestHeaders = value }

    // Constructors
    
    /**
     * @param endpoint URL of the collector that is going to receive the events tracked by the tracker.
     * The URL can include the schema/protocol (e.g.: `http://collector-url.com`).
     * If the URL doesn't include the protocol, HTTPS is automatically selected.
     * @param method The method used to send the requests (GET or POST).
     */
    @JvmOverloads
    constructor(endpoint: String, method: HttpMethod = HttpMethod.POST) {
        this.method = method
        val uri = Uri.parse(endpoint)
        val scheme = uri.scheme
        
        if (scheme == null) {
            protocol = Protocol.HTTPS
            this.endpoint = "https://$endpoint"
            return
        }
        when (scheme) {
            "https" -> {
                protocol = Protocol.HTTPS
                this.endpoint = endpoint
            }
            "http" -> {
                protocol = Protocol.HTTP
                this.endpoint = endpoint
            }
            else -> {
                protocol = Protocol.HTTPS
                this.endpoint = "https://$endpoint"
            }
        }
    }

    /**
     * @param networkConnection A [NetworkConnection] component which will control the
     * communication between the tracker and the collector.
     */
    constructor(networkConnection: NetworkConnection) {
        this.networkConnection = networkConnection
    }

    internal constructor() {}

    // Builder methods
    
    /**
     * A custom path which will be added to the endpoint URL to specify the
     * complete URL of the event collector when paired with the POST method. The default path is 
     * "com.snowplowanalytics/snowplow/tp2". 
     * The collector must be configured to accept the custom path.
     */
    fun customPostPath(customPostPath: String): NetworkConfiguration {
        this.customPostPath = customPostPath
        return this
    }

    /**
     * The timeout set for the requests to the collector.
     * The maximum timeout for emitting events. If emit time exceeds this value
     * TimeOutException will be thrown.
     */
    fun timeout(timeout: Int): NetworkConfiguration {
        this.timeout = timeout
        return this
    }

    /**
     * An OkHttp client that will be used in the emitter. You can provide your
     * own if you want to share your Singleton client's interceptors, connection pool etc.
     * By default a new [OkHttpClient] is created when the tracker is instantiated.
     */
    fun okHttpClient(okHttpClient: OkHttpClient): NetworkConfiguration {
        this.okHttpClient = okHttpClient
        return this
    }

    /**
     * An OkHttp cookie jar to override the default 
     * [CollectorCookieJar](com.snowplowanalytics.snowplow.network.CollectorCookieJar) 
     * that stores cookies in SharedPreferences.
     * A cookie jar provided here will be ignored if a custom `okHttpClient` is configured.
     */
    fun okHttpCookieJar(okHttpCookieJar: CookieJar): NetworkConfiguration {
        this.okHttpCookieJar = okHttpCookieJar
        return this
    }

    /**
     * Custom headers to add to HTTP requests to the collector.
     */
    fun requestHeaders(requestHeaders: Map<String, String>): NetworkConfiguration {
        this.requestHeaders = requestHeaders
        return this
    }

    // Copyable
    override fun copy(): Configuration {
        val copy: NetworkConfiguration = if (networkConnection != null) {
            NetworkConfiguration(networkConnection!!)
        } else {
            val scheme = if (protocol == Protocol.HTTPS) "https://" else "http://"
            NetworkConfiguration(scheme + endpoint, method)
        }
        copy.customPostPath = customPostPath
        copy.timeout = timeout
        return copy
    }

    // JSON Formatter
    /**
     * This constructor is used during remote configuration.
     */
    constructor(jsonObject: JSONObject) : this("") {
        try {
            endpoint = jsonObject.getString("endpoint")
            val methodStr = jsonObject.getString("method")
            method = HttpMethod.valueOf(methodStr.uppercase(Locale.getDefault()))
        } catch (e: Exception) {
            Logger.e(TAG, "Unable to get remote configuration")
        }
    }

    companion object {
        private val TAG = NetworkConfiguration::class.java.simpleName
    }
}
