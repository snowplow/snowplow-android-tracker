package com.snowplowanalytics.snowplow.configuration

import android.net.Uri
import com.snowplowanalytics.snowplow.network.HttpMethod
import java.util.*

/**
 * Represents the configuration for fetching configurations from a remote source.
 */
class RemoteConfiguration(endpoint: String, method: HttpMethod) : Configuration {
    /**
     * URL of the remote configuration.
     */
    @JvmField
    val endpoint: String

    /**
     * The method used to send the requests (GET or POST).
     */
    @JvmField
    val method: HttpMethod

    /**
     * @param endpoint URL of the remote configuration.
     * The URL can include the schema/protocol (e.g.: `http://remote-config-url.xyz`).
     * In case the URL doesn't include the schema/protocol, the HTTPS protocol is
     * automatically selected.
     * @param method   The method used to send the requests (GET or POST).
     */
    init {
        this.method = method
        val uri = Uri.parse(endpoint)
        val scheme = uri.scheme

        if (scheme != null && listOf("https", "http").contains(scheme)) {
            this.endpoint = endpoint
        } else {
            this.endpoint = "https://$endpoint"
        }
    }

    // Copyable
    override fun copy(): Configuration {
        return RemoteConfiguration(endpoint, method)
    }
}
