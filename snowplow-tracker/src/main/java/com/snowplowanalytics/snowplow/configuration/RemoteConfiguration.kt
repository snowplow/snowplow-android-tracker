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
import com.snowplowanalytics.snowplow.network.HttpMethod
import java.util.*

/**
 * Represents the configuration for fetching configurations from a remote source.
 */
class RemoteConfiguration(endpoint: String, method: HttpMethod) : Configuration {
    /**
     * URL of the remote configuration.
     */
    val endpoint: String

    /**
     * The method used to send the requests (GET or POST).
     */
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
