package com.snowplowanalytics.core.emitter

import androidx.annotation.RestrictTo
import com.snowplowanalytics.core.Controller
import com.snowplowanalytics.core.emitter.EmitterControllerImpl
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
        private get() = serviceProvider.orMakeTracker.emitter

    // Getters and Setters
    override fun getEventStore(): EventStore? {
        return emitter.eventStore
    }

    override fun getBufferOption(): BufferOption {
        return emitter.bufferOption
    }

    override fun setBufferOption(bufferOption: BufferOption) {
        dirtyConfig.bufferOption = bufferOption
        dirtyConfig.bufferOptionUpdated = true
        emitter.bufferOption = bufferOption
    }

    override fun getEmitRange(): Int {
        return emitter.sendLimit
    }

    override fun setEmitRange(emitRange: Int) {
        dirtyConfig.emitRange = emitRange
        dirtyConfig.emitRangeUpdated = true
        emitter.sendLimit = emitRange
    }

    override fun getThreadPoolSize(): Int {
        return Executor.getThreadCount()
    }

    override fun getByteLimitGet(): Long {
        return emitter.byteLimitGet
    }

    override fun setByteLimitGet(byteLimitGet: Long) {
        dirtyConfig.byteLimitGet = byteLimitGet
        dirtyConfig.byteLimitGetUpdated = true
        emitter.byteLimitGet = byteLimitGet
    }

    override fun getByteLimitPost(): Long {
        return emitter.byteLimitPost
    }

    override fun setByteLimitPost(byteLimitPost: Long) {
        dirtyConfig.byteLimitPost = byteLimitPost
        dirtyConfig.byteLimitPostUpdated = true
        emitter.byteLimitPost = byteLimitPost
    }

    override fun getRequestCallback(): RequestCallback? {
        return emitter.requestCallback
    }

    override fun setRequestCallback(requestCallback: RequestCallback?) {
        emitter.requestCallback = requestCallback
    }

    override fun getCustomRetryForStatusCodes(): Map<Int?, Boolean?>? {
        return emitter.customRetryForStatusCodes
    }

    override fun setCustomRetryForStatusCodes(customRetryForStatusCodes: Map<Int, Boolean>?) {
        emitter.setCustomRetryForStatusCodes(customRetryForStatusCodes)
    }

    override fun isServerAnonymisation(): Boolean {
        return emitter.serverAnonymisation
    }

    override fun setServerAnonymisation(serverAnonymisation: Boolean) {
        dirtyConfig.serverAnonymisation = serverAnonymisation
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
    val dirtyConfig: EmitterConfigurationUpdate
        get() = serviceProvider.emitterConfigurationUpdate

    companion object {
        private val TAG = EmitterControllerImpl::class.java.simpleName
    }
}
