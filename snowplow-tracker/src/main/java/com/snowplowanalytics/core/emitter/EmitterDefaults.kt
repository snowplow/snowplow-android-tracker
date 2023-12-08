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

import com.snowplowanalytics.snowplow.emitter.BufferOption
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.network.Protocol

import java.util.*
import java.util.concurrent.TimeUnit

object EmitterDefaults {
    var httpMethod = HttpMethod.POST
    var bufferOption = BufferOption.Single
    var httpProtocol = Protocol.HTTPS
    var tlsVersions: EnumSet<TLSVersion> = EnumSet.of(TLSVersion.TLSv1_2)
    var emitRange: Int = 150
    var emitterTick = 5
    var sendLimit = 250
    var emptyLimit = 5
    var byteLimitGet: Long = 40000
    var byteLimitPost: Long = 40000
    var emitTimeout = 5
    var threadPoolSize = 15
    var serverAnonymisation = false
    var retryFailedRequests = true
    var timeUnit = TimeUnit.SECONDS
}
