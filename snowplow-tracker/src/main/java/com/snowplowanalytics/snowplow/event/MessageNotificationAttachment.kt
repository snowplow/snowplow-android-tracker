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
package com.snowplowanalytics.snowplow.event

/** Attachment object that identify an attachment in the MessageNotification.  */
class MessageNotificationAttachment(identifier: String, type: String, url: String) :
    HashMap<String, Any>() {
    
    /** Attachments added to the notification (they can be part of the data object).  */
    init {
        put(PARAM_IDENTIFIER, identifier)
        put(PARAM_TYPE, type)
        put(PARAM_URL, url)
    }

    companion object {
        const val PARAM_IDENTIFIER = "identifier"
        const val PARAM_TYPE = "type"
        const val PARAM_URL = "url"
    }
}
