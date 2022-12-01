package com.snowplowanalytics.core.emitter

import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.network.NetworkConnection
import com.snowplowanalytics.snowplow.network.Protocol
import okhttp3.CookieJar
import okhttp3.OkHttpClient

class NetworkConfigurationUpdate : NetworkConfigurationInterface {
    @JvmField
    var sourceConfig: NetworkConfiguration? = null
    override var customPostPath: String? = null

    // Getters
    override val endpoint: String?
        get() = if (sourceConfig == null) null else sourceConfig!!.endpoint
    override val method: HttpMethod?
        get() = if (sourceConfig == null) null else sourceConfig!!.method
    override val protocol: Protocol?
        get() = if (sourceConfig == null) null else sourceConfig!!.protocol
    override val networkConnection: NetworkConnection?
        get() = if (sourceConfig == null) null else sourceConfig!!.getNetworkConnection()
    override val timeout: Int?
        get() = if (sourceConfig == null) null else sourceConfig!!.getTimeout()
    override val okHttpClient: OkHttpClient?
        get() = if (sourceConfig == null) null else sourceConfig!!.getOkHttpClient()
    override val okHttpCookieJar: CookieJar?
        get() = if (sourceConfig == null) null else sourceConfig!!.getOkHttpCookieJar()

    // customPostPath flag
    var customPostPathUpdated = false
    override fun getCustomPostPath(): String? {
        return if (sourceConfig == null || customPostPathUpdated) customPostPath else sourceConfig!!.customPostPath
    }
}
