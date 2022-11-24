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
package com.snowplowanalytics.snowplow.globalcontexts

import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow.tracker.InspectableEvent
import java.util.*

class GlobalContext {
    private var generator: FunctionalGenerator
    private var filter: FunctionalFilter?

    constructor(contextGenerator: ContextGenerator) {
        generator = object : FunctionalGenerator() {
            override fun apply(event: InspectableEvent): List<SelfDescribingJson?> {
                return contextGenerator.generateContexts(event)
            }
        }
        filter = object : FunctionalFilter() {
            override fun apply(event: InspectableEvent): Boolean {
                return contextGenerator.filterEvent(event)
            }
        }
    }

    constructor(staticContexts: List<SelfDescribingJson?>) {
        generator = object : FunctionalGenerator() {
            override fun apply(event: InspectableEvent): List<SelfDescribingJson?> {
                return staticContexts
            }
        }
        filter = null
    }

    constructor(staticContexts: List<SelfDescribingJson?>, ruleset: SchemaRuleSet?) {
        generator = object : FunctionalGenerator() {
            override fun apply(event: InspectableEvent): List<SelfDescribingJson?> {
                return staticContexts
            }
        }
        filter = ruleset?.filter
    }

    constructor(staticContexts: List<SelfDescribingJson?>, filter: FunctionalFilter?) {
        generator = object : FunctionalGenerator() {
            override fun apply(event: InspectableEvent): List<SelfDescribingJson?> {
                return staticContexts
            }
        }
        this.filter = filter
    }

    constructor(generator: FunctionalGenerator, ruleset: SchemaRuleSet?) : this(
        generator,
        ruleset?.filter
    )

    @JvmOverloads
    constructor(
        generator: FunctionalGenerator,
        filter: FunctionalFilter? = null
    ) {
        this.generator = generator
        this.filter = filter
    }

    fun generateContexts(event: InspectableEvent): List<SelfDescribingJson?> {
        return if (filter != null && !filter!!.apply(event)) {
            ArrayList()
        } else generator.apply(event)!!
    }
}
