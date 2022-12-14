package com.snowplowanalytics.core.emitter

import com.snowplowanalytics.snowplow.emitter.BufferOption
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.network.Protocol

import java.util.*
import java.util.concurrent.TimeUnit

object EmitterDefaults {
    var httpMethod = HttpMethod.POST
    var bufferOption = BufferOption.DefaultGroup
    var requestSecurity = Protocol.HTTP
    var tlsVersions: EnumSet<TLSVersion> = EnumSet.of(TLSVersion.TLSv1_2)
    var emitterTick = 5
    var sendLimit = 250
    var emptyLimit = 5
    var byteLimitGet: Long = 40000
    var byteLimitPost: Long = 40000
    var emitTimeout = 5
    var threadPoolSize = 2
    var serverAnonymisation = false
    var timeUnit = TimeUnit.SECONDS
}
