package com.snowplowanalytics.core.emitter

import com.snowplowanalytics.snowplow.configuration.EmitterConfiguration
import com.snowplowanalytics.snowplow.emitter.BufferOption
import com.snowplowanalytics.snowplow.emitter.EventStore
import com.snowplowanalytics.snowplow.network.RequestCallback

class EmitterConfigurationUpdate : EmitterConfiguration() {
    var sourceConfig: EmitterConfiguration? = null
    var isPaused = false

    fun eventStore(): EventStore? {
        return if (sourceConfig == null) null else sourceConfig!!.eventStore
    }

    fun requestCallback(): RequestCallback? {
        return if (sourceConfig == null) null else sourceConfig!!.requestCallback
    }

    // bufferOption flag
    var bufferOptionUpdated = false
    fun bufferOption(): BufferOption {
        return if (sourceConfig == null || bufferOptionUpdated) super.bufferOption else sourceConfig!!.bufferOption
    }

    // emitRange flag
    var emitRangeUpdated = false
    fun emitRange(): Int {
        return if (sourceConfig == null || emitRangeUpdated) super.emitRange else sourceConfig!!.emitRange
    }

    // threadPoolSize flag
    var threadPoolSizeUpdated = false
    fun threadPoolSize(): Int {
        return if (sourceConfig == null || threadPoolSizeUpdated) super.threadPoolSize else sourceConfig!!.threadPoolSize
    }

    // byteLimitGet flag
    var byteLimitGetUpdated = false
    fun byteLimitGet(): Long {
        return if (sourceConfig == null || byteLimitGetUpdated) super.byteLimitGet else sourceConfig!!.byteLimitGet
    }

    // byteLimitPost flag
    var byteLimitPostUpdated = false
    fun byteLimitPost(): Long {
        return if (sourceConfig == null || byteLimitPostUpdated) super.byteLimitPost else sourceConfig!!.byteLimitPost
    }

    // customRetryForStatusCodes flag
    var customRetryForStatusCodesUpdated = false
    fun customRetryForStatusCodes(): Map<Int, Boolean>? {
        return if (sourceConfig == null || customRetryForStatusCodesUpdated) super.customRetryForStatusCodes else sourceConfig!!.customRetryForStatusCodes
    }

    // serverAnonymisation flag
    var serverAnonymisationUpdated = false
    fun serverAnonymisation(): Boolean {
        return if (sourceConfig == null || serverAnonymisationUpdated) super.serverAnonymisation else sourceConfig!!.serverAnonymisation
    }


}
