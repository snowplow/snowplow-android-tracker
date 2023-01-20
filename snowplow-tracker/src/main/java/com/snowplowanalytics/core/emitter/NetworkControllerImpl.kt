package com.snowplowanalytics.core.emitter

import androidx.annotation.RestrictTo
import com.snowplowanalytics.core.Controller
import com.snowplowanalytics.core.tracker.ServiceProviderInterface
import com.snowplowanalytics.snowplow.controller.NetworkController
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.network.NetworkConnection
import com.snowplowanalytics.snowplow.network.OkHttpNetworkConnection
import java.util.concurrent.atomic.AtomicReference

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
            dirtyConfig.customPostPathUpdated = true
            emitter.customPostPath = customPostPath
        }
    
    override var timeout: Int
        get() = emitter.emitTimeout
        set(timeout) {
            emitter.emitTimeout = timeout
        }

    // Private methods
    private val emitter: Emitter
        get() = serviceProvider.orMakeEmitter()
    
    private val dirtyConfig: NetworkConfigurationUpdate
        get() = serviceProvider.networkConfigurationUpdate
}
