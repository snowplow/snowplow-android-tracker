package com.snowplowanalytics.core.emitter

import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.network.NetworkConnection
import com.snowplowanalytics.snowplow.network.Protocol
import okhttp3.CookieJar
import okhttp3.OkHttpClient

class NetworkConfigurationUpdate : NetworkConfigurationInterface {
    var sourceConfig: NetworkConfiguration? = null
    var customPostPathUpdated = false
    
    override var customPostPath: String? = null
        get() = if (sourceConfig == null || customPostPathUpdated) field else sourceConfig!!.customPostPath
    
    override val endpoint: String?
        get() = if (sourceConfig == null) null else sourceConfig!!.endpoint
    
    override val method: HttpMethod?
        get() = if (sourceConfig == null) null else sourceConfig!!.method
    
    override val protocol: Protocol?
        get() = if (sourceConfig == null) null else sourceConfig!!.protocol
    
    override val networkConnection: NetworkConnection?
        get() = if (sourceConfig == null) null else sourceConfig!!.networkConnection
    
    override val timeout: Int?
        get() = if (sourceConfig == null) null else sourceConfig!!.timeout
    
    override val okHttpClient: OkHttpClient?
        get() = if (sourceConfig == null) null else sourceConfig!!.okHttpClient
    
    override val okHttpCookieJar: CookieJar?
        get() = if (sourceConfig == null) null else sourceConfig!!.okHttpCookieJar
}
