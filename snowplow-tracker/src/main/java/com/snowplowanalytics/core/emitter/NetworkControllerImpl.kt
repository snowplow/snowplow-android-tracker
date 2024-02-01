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

import androidx.annotation.RestrictTo
import com.snowplowanalytics.core.Controller
import com.snowplowanalytics.core.tracker.ServiceProviderInterface
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration
import com.snowplowanalytics.snowplow.controller.NetworkController
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.network.OkHttpNetworkConnection

@RestrictTo(RestrictTo.Scope.LIBRARY)
class NetworkControllerImpl(serviceProvider: ServiceProviderInterface) : 
    Controller(serviceProvider), NetworkController {
    
    // Getters and Setters
    val customNetworkConnection: Boolean
        get() {
            val networkConnection = emitter.networkConnection
            return networkConnection != null && networkConnection !is OkHttpNetworkConnection
        }

    override var endpoint: String
        get() = emitter.emitterUri
        set(endpoint) {
            emitter.emitterUri = endpoint
        }
    
    override var method: HttpMethod
        get() = emitter.httpMethod
        set(method) {
            emitter.httpMethod = method
        }
    
    override var customPostPath: String?
        get() = emitter.customPostPath
        set(customPostPath) {
            dirtyConfig.customPostPath = customPostPath
            emitter.customPostPath = customPostPath
        }
    
    override var timeout: Int?
        get() = emitter.emitTimeout
        set(timeout) {
            emitter.emitTimeout = timeout
        }

    // Private methods
    private val emitter: Emitter
        get() = serviceProvider.getOrMakeEmitter()
    
    private val dirtyConfig: NetworkConfiguration
        get() = serviceProvider.networkConfiguration
}
