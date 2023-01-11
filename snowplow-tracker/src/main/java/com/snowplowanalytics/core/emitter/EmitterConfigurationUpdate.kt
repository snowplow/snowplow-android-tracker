package com.snowplowanalytics.core.emitter

import com.snowplowanalytics.snowplow.configuration.EmitterConfiguration
import com.snowplowanalytics.snowplow.emitter.BufferOption
import com.snowplowanalytics.snowplow.emitter.EventStore
import com.snowplowanalytics.snowplow.network.RequestCallback

class EmitterConfigurationUpdate : EmitterConfiguration() {
    var sourceConfig: EmitterConfiguration? = null
    var isPaused = false
    var bufferOptionUpdated = false
    var emitRangeUpdated = false
    var threadPoolSizeUpdated = false
    var byteLimitGetUpdated = false
    var byteLimitPostUpdated = false
    var customRetryForStatusCodesUpdated = false
    var serverAnonymisationUpdated = false

    override var eventStore: EventStore?
        get() = if (sourceConfig == null) null else sourceConfig!!.eventStore
        set(value) {
            // can't set a new eventStore
        }

    override var requestCallback: RequestCallback?
        get() = if (sourceConfig == null) null else sourceConfig!!.requestCallback
        set(value) {
            // can't set a new requestCallback
        }

    override var bufferOption: BufferOption
        get() = if (sourceConfig == null || bufferOptionUpdated) super.bufferOption else sourceConfig!!.bufferOption
        set(value) {
            super.bufferOption = value
            bufferOptionUpdated = true
        }
    
    override var threadPoolSize: Int
        get() = if (sourceConfig == null || threadPoolSizeUpdated) super.threadPoolSize else sourceConfig!!.threadPoolSize
        set(value) {
            super.threadPoolSize = value
            threadPoolSizeUpdated = true
        }
    
    override var byteLimitGet: Long
        get() = if (sourceConfig == null || byteLimitGetUpdated) super.byteLimitGet else sourceConfig!!.byteLimitGet
        set(value) {
            super.byteLimitGet = value
            byteLimitGetUpdated = true
        }
    
    override var byteLimitPost: Long
        get() = if (sourceConfig == null || byteLimitPostUpdated) super.byteLimitPost else sourceConfig!!.byteLimitPost
        set(value) {
            super.byteLimitPost = value
            byteLimitPostUpdated = true
        }
    
    override var customRetryForStatusCodes: Map<Int, Boolean>?
        get() = if (sourceConfig == null || customRetryForStatusCodesUpdated) super.customRetryForStatusCodes else sourceConfig!!.customRetryForStatusCodes
        set(value) {
            super.customRetryForStatusCodes = value
            customRetryForStatusCodesUpdated = true
        }
    
    override var serverAnonymisation: Boolean
        get() = if (sourceConfig == null || serverAnonymisationUpdated) super.serverAnonymisation else sourceConfig!!.serverAnonymisation
        set(value) {
            super.serverAnonymisation = value
            serverAnonymisationUpdated = true
        }

//    fun requestCallback(): RequestCallback? {
//        return if (sourceConfig == null) null else sourceConfig!!.requestCallback
//    }
//    
//    fun bufferOption(): BufferOption {
//        return if (sourceConfig == null || bufferOptionUpdated) super.bufferOption else sourceConfig!!.bufferOption
//    }

    override var emitRange: Int
        get() = if (sourceConfig == null || emitRangeUpdated) super.emitRange else sourceConfig!!.emitRange
        set(value) {
            super.emitRange = value
            emitRangeUpdated = true
        }


//    fun emitRange(): Int {
//        return if (sourceConfig == null || emitRangeUpdated) super.emitRange else sourceConfig!!.emitRange
//    }

//    fun threadPoolSize(): Int {
//        return if (sourceConfig == null || threadPoolSizeUpdated) super.threadPoolSize else sourceConfig!!.threadPoolSize
//    }
//
//    fun byteLimitGet(): Long {
//        return if (sourceConfig == null || byteLimitGetUpdated) super.byteLimitGet else sourceConfig!!.byteLimitGet
//    }
//
//
//    fun byteLimitPost(): Long {
//        return if (sourceConfig == null || byteLimitPostUpdated) super.byteLimitPost else sourceConfig!!.byteLimitPost
//    }

//
//    fun customRetryForStatusCodes(): Map<Int, Boolean>? {
//        return if (sourceConfig == null || customRetryForStatusCodesUpdated) super.customRetryForStatusCodes else sourceConfig!!.customRetryForStatusCodes
//    }
//
//
//    fun serverAnonymisation(): Boolean {
//        return if (sourceConfig == null || serverAnonymisationUpdated) super.serverAnonymisation else sourceConfig!!.serverAnonymisation
//    }


}
