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

package com.snowplowanalytics.snowplow.signals

/**
 * Names of the attribute keys built into Signals that the tracker can fill in from the `client_session` entity.
 */
object SignalsAttributeKeys {
    /** The `userId` in the `client_session` entity, i.e. `tracker.session.userId`. */
    const val CLIENT_SESSION_USER_ID = "client_session_user_id"

    /** The `sessionId` in the `client_session` entity, i.e. `tracker.session.sessionId`. */
    const val CLIENT_SESSION_SESSION_ID = "client_session_session_id"
}
