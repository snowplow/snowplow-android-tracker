package com.snowplowanalytics.snowplow.configuration

import android.net.Uri
import com.snowplowanalytics.core.emitter.NetworkConfigurationInterface
import com.snowplowanalytics.core.tracker.Logger
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration
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
 */
class NetworkConfiguration : NetworkConfigurationInterface, Configuration {
    private var endpoint: String? = null
    private var method: HttpMethod? = null
    private var protocol: Protocol? = null

    /**
     * @see .NetworkConfiguration
     */
    @JvmField
    var networkConnection: NetworkConnection? = null

    /**
     * @see .customPostPath
     */
    @JvmField
    var customPostPath: String? = null

    /**
     * @see .timeout
     */
    @JvmField
    var timeout: Int? = null

    /**
     * @see .okHttpClient
     */
    @JvmField
    var okHttpClient: OkHttpClient? = null

    /**
     * @see .okHttpCookieJar
     */
    @JvmField
    var okHttpCookieJar: CookieJar? = null
    
    /**
     * @param endpoint URL of the collector that is going to receive the events tracked by the tracker.
     * The URL can include the schema/protocol (e.g.: `http://collector-url.com`).
     * In case the URL doesn't include the schema/protocol, the HTTPS protocol is
     * automatically selected.
     * @param method The method used to send the requests (GET or POST).
     */
    
    // Constructors
    
    /**
     * @param endpoint URL of the collector that is going to receive the events tracked by the tracker.
     * The URL can include the schema/protocol (e.g.: `http://collector-url.com`).
     * In case the URL doesn't include the schema/protocol, the HTTPS protocol is
     * automatically selected.
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
     * @param networkConnection The NetworkConnection component which will control the
     * communication between the tracker and the collector.
     */
    constructor(networkConnection: NetworkConnection) {
        this.networkConnection = networkConnection
    }
    
    // Getters
    
    /**
     * @return URL (without schema/protocol) used to send events to the collector.
     */
    override fun getEndpoint(): String? {
        return endpoint
    }

    /**
     * @return Method used to send events to the collector.
     */
    override fun getMethod(): HttpMethod? {
        return method
    }

    /**
     * @return Protocol used to send events to the collector.
     */
    override fun getProtocol(): Protocol? {
        return protocol
    }

    override fun getCustomPostPath(): String? {
        return customPostPath
    }

    override fun getTimeout(): Int? {
        return timeout
    }

    override fun getNetworkConnection(): NetworkConnection? {
        return networkConnection
    }

    override fun getOkHttpClient(): OkHttpClient? {
        return okHttpClient
    }

    override fun getOkHttpCookieJar(): CookieJar? {
        return okHttpCookieJar
    }
    
    // Builder methods
    
    /**
     * A custom path which will be added to the endpoint URL to specify the
     * complete URL of the collector when paired with the POST method.
     */
    fun customPostPath(customPostPath: String): NetworkConfiguration {
        this.customPostPath = customPostPath
        return this
    }

    /**
     * The timeout set for the requests to the collector.
     */
    fun timeout(timeout: Int): NetworkConfiguration {
        this.timeout = timeout
        return this
    }

    /**
     * An OkHttp client that will be used in the emitter, you can provide your
     * own if you want to share your Singleton client's interceptors, connection pool etc..
     * Otherwise a new one is created.
     */
    fun okHttpClient(okHttpClient: OkHttpClient): NetworkConfiguration {
        this.okHttpClient = okHttpClient
        return this
    }

    /**
     * An OkHttp cookie jar to override the default cookie jar that stores cookies in SharedPreferences.
     * The cookie jar will be ignored in case custom `okHttpClient` is configured.
     */
    fun okHttpCookieJar(okHttpCookieJar: CookieJar): NetworkConfiguration {
        this.okHttpCookieJar = okHttpCookieJar
        return this
    }

    // Copyable
    override fun copy(): Configuration {
        val copy: NetworkConfiguration = if (networkConnection != null) {
            NetworkConfiguration(networkConnection!!)
        } else {
            val scheme =
                if (protocol == Protocol.HTTPS) "https://" else "http://"
            NetworkConfiguration(scheme + endpoint, method!!)
        }
        copy.customPostPath = customPostPath
        copy.timeout = timeout
        return copy
    }

    // JSON Formatter
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
