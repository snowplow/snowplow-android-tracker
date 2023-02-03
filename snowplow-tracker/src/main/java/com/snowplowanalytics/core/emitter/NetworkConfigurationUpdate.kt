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
