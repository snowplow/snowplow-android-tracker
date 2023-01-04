package com.snowplowanalytics.core.emitter

import androidx.annotation.RestrictTo
import com.snowplowanalytics.core.Controller
import com.snowplowanalytics.core.tracker.Logger
import com.snowplowanalytics.core.tracker.ServiceProviderInterface
import com.snowplowanalytics.snowplow.controller.EmitterController
import com.snowplowanalytics.snowplow.emitter.BufferOption
import com.snowplowanalytics.snowplow.emitter.EventStore
import com.snowplowanalytics.snowplow.network.RequestCallback

@RestrictTo(RestrictTo.Scope.LIBRARY)
class EmitterControllerImpl(serviceProvider: ServiceProviderInterface) :
    Controller(serviceProvider), EmitterController {
    
    private val emitter: Emitter
        get() = serviceProvider.orMakeTracker().emitter
    
    override val eventStore: EventStore?
        get() = emitter.eventStore

    override var bufferOption: BufferOption
        get() = emitter.bufferOption
        set(bufferOption) {
            dirtyConfig.bufferOption = bufferOption
            dirtyConfig.bufferOptionUpdated = true
            emitter.bufferOption = bufferOption
        }

    override var emitRange: Int
        get() = emitter.sendLimit
        set(emitRange) {
            dirtyConfig.emitRange = emitRange
            dirtyConfig.emitRangeUpdated = true
            emitter.sendLimit = emitRange
        }

    override val threadPoolSize: Int
        get() = Executor.threadCount
    
    override var byteLimitGet: Long
        get() = emitter.byteLimitGet
        set(byteLimitGet) {
            dirtyConfig.byteLimitGet = byteLimitGet
            dirtyConfig.byteLimitGetUpdated = true
            emitter.byteLimitGet = byteLimitGet
        }
    
    override var byteLimitPost: Long
        get() = emitter.byteLimitPost
        set(byteLimitPost) {
            dirtyConfig.byteLimitPost = byteLimitPost
            dirtyConfig.byteLimitPostUpdated = true
            emitter.byteLimitPost = byteLimitPost
        }

    override var requestCallback: RequestCallback?
        get() = emitter.requestCallback
        set(requestCallback) { emitter.requestCallback = requestCallback }
    
    override var customRetryForStatusCodes: Map<Int, Boolean>?
        get() = emitter.customRetryForStatusCodes
        set(customRetryForStatusCodes) { emitter.customRetryForStatusCodes = customRetryForStatusCodes }

    override var isServerAnonymisation: Boolean
        get() = emitter.serverAnonymisation
        set(serverAnonymisation) {
            dirtyConfig.isServerAnonymisation = serverAnonymisation
            dirtyConfig.serverAnonymisationUpdated = true
            emitter.serverAnonymisation = serverAnonymisation
        }

    override val dbCount: Long
        get() {
            val eventStore = emitter.eventStore
            if (eventStore == null) {
                Logger.e(TAG, "EventStore not available in the Emitter.")
                return -1
            }
            return eventStore.size
        }
    
    override val isSending: Boolean
        get() = emitter.emitterStatus

    override fun pause() {
        dirtyConfig.isPaused = true
        emitter.pauseEmit()
    }

    override fun resume() {
        dirtyConfig.isPaused = false
        emitter.resumeEmit()
    }

    // Private methods
    private val dirtyConfig: EmitterConfigurationUpdate
        get() = serviceProvider.emitterConfigurationUpdate

    companion object {
        private val TAG = EmitterControllerImpl::class.java.simpleName
    }
}
