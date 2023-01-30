/*
 * Copyright (c) 2015-2022 Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.core.globalcontexts

import com.snowplowanalytics.snowplow.configuration.PluginAfterTrackConfiguration
import com.snowplowanalytics.snowplow.configuration.PluginConfigurationInterface
import com.snowplowanalytics.snowplow.configuration.PluginEntitiesConfiguration
import com.snowplowanalytics.snowplow.globalcontexts.GlobalContext

class GlobalContextPluginConfiguration(
    override val identifier: String,
    val globalContext: GlobalContext
) : PluginConfigurationInterface {

    override val afterTrackConfiguration: PluginAfterTrackConfiguration? = null

    override val entitiesConfiguration: PluginEntitiesConfiguration
        get() = PluginEntitiesConfiguration(closure = globalContext::generateContexts)
}
