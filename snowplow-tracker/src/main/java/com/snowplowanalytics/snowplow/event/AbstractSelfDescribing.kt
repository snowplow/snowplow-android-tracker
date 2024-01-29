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
package com.snowplowanalytics.snowplow.event

/**
 * Abstract class which contains common elements to all self-describing events, i.e. events that are
 * based on a schema. Examples include [SelfDescribing], [ScreenView], and [Foreground] events.
 */
abstract class AbstractSelfDescribing protected constructor() : AbstractEvent() {
    /**
     * @return The schema of the event.
     */
    abstract val schema: String
}
