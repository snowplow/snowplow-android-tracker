package com.snowplowanalytics.core.emitter

import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.network.NetworkConnection
import com.snowplowanalytics.snowplow.network.Protocol
import okhttp3.CookieJar
import okhttp3.OkHttpClient

interface NetworkConfigurationInterface {
    val endpoint: String?
    val method: HttpMethod?
    val protocol: Protocol?
    val networkConnection: NetworkConnection?
    val customPostPath: String?
    val timeout: Int?
    val okHttpClient: OkHttpClient?
    val okHttpCookieJar: CookieJar?
}
