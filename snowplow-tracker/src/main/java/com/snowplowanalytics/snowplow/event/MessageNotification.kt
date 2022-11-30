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

/** Event that represents the reception of a push notification (or a locally generated one).
 * @note The custom data of the push notification have to be tracked separately in custom entities that can be attached to this event.
 * @param title Title of message notification.
 * @param body Body content of the message notification.
 * @param trigger The trigger that raised this notification: remote notification (push), position related (location), date-time related (calendar, timeInterval) or app generated (other).
 */
class MessageNotification(
    val title: String,
    val body: String,
    val trigger: MessageNotificationTrigger
) : AbstractSelfDescribing() {
    
    /** The action associated with the notification.  */
    @JvmField
    var action: String? = null

    /** Attachments added to the notification (they can be part of the data object).  */
    @JvmField
    var attachments: List<MessageNotificationAttachment>? = null

    /** Variable string values to be used in place of the format specifiers in bodyLocArgs to use to localize the body text to the user's current localization.  */
    @JvmField
    var bodyLocArgs: List<String>? = null

    /** The key to the body string in the app's string resources to use to localize the body text to the user's current localization.  */
    @JvmField
    var bodyLocKey: String? = null

    /** The category associated to the notification.  */
    @JvmField
    var category: String? = null

    /** The application is notified of the delivery of the notification if it's in the foreground or background, the app will be woken up (iOS only).  */
    @JvmField
    var contentAvailable: Boolean? = null

    /** The group which this notification is part of.  */
    @JvmField
    var group: String? = null

    /** The icon associated to the notification (Android only).  */
    @JvmField
    var icon: String? = null

    /** The number of items this notification represents.  */
    @JvmField
    var notificationCount: Int? = null

    /** The time when the event of the notification occurred.  */
    @JvmField
    var notificationTimestamp: String? = null

    /** The sound played when the device receives the notification.  */
    @JvmField
    var sound: String? = null

    /** The notification's subtitle. (iOS only)  */
    @JvmField
    var subtitle: String? = null

    /** An identifier similar to 'group' but usable for different purposes (Android only).  */
    @JvmField
    var tag: String? = null

    /** An identifier similar to 'group' but usable for different purposes (iOS only).  */
    @JvmField
    var threadIdentifier: String? = null

    /** Variable string values to be used in place of the format specifiers in titleLocArgs to use to localize the title text to the user's current localization.  */
    @JvmField
    var titleLocArgs: List<String>? = null

    /** The key to the title string in the app's string resources to use to localize the title text to the user's current localization.  */
    @JvmField
    var titleLocKey: String? = null

    override val schema: String
        get() = Companion.schema
    
    // Builder methods
    
    /** The action associated with the notification.  */
    fun action(action: String?): MessageNotification {
        this.action = action
        return this
    }

    /** Attachments added to the notification (they can be part of the data object).  */
    fun attachments(attachments: List<MessageNotificationAttachment>?): MessageNotification {
        this.attachments = attachments
        return this
    }

    /** Variable string values to be used in place of the format specifiers in bodyLocArgs to use to localize the body text to the user's current localization.  */
    fun bodyLocArgs(bodyLocArgs: List<String>?): MessageNotification {
        this.bodyLocArgs = bodyLocArgs
        return this
    }

    /** The key to the body string in the app's string resources to use to localize the body text to the user's current localization.  */
    fun bodyLocKey(bodyLocKey: String?): MessageNotification {
        this.bodyLocKey = bodyLocKey
        return this
    }

    /** The category associated to the notification.  */
    fun category(category: String?): MessageNotification {
        this.category = category
        return this
    }

    /** The application is notified of the delivery of the notification if it's in the foreground or background, the app will be woken up (iOS only).  */
    fun contentAvailable(contentAvailable: Boolean?): MessageNotification {
        this.contentAvailable = contentAvailable
        return this
    }

    /** The group which this notification is part of.  */
    fun group(group: String?): MessageNotification {
        this.group = group
        return this
    }

    /** The icon associated to the notification (Android only).  */
    fun icon(icon: String?): MessageNotification {
        this.icon = icon
        return this
    }

    /** The number of items this notification represents.  */
    fun notificationCount(notificationCount: Int?): MessageNotification {
        this.notificationCount = notificationCount
        return this
    }

    /** The time when the event of the notification occurred.  */
    fun notificationTimestamp(notificationTimestamp: String?): MessageNotification {
        this.notificationTimestamp = notificationTimestamp
        return this
    }

    /** The sound played when the device receives the notification.  */
    fun sound(sound: String?): MessageNotification {
        this.sound = sound
        return this
    }

    /** The notification's subtitle. (iOS only)  */
    fun subtitle(subtitle: String?): MessageNotification {
        this.subtitle = subtitle
        return this
    }

    /** An identifier similar to 'group' but usable for different purposes (Android only).  */
    fun tag(tag: String?): MessageNotification {
        this.tag = tag
        return this
    }

    /** An identifier similar to 'group' but usable for different purposes (iOS only).  */
    fun threadIdentifier(threadIdentifier: String?): MessageNotification {
        this.threadIdentifier = threadIdentifier
        return this
    }

    /** Variable string values to be used in place of the format specifiers in titleLocArgs to use to localize the title text to the user's current localization.  */
    fun titleLocArgs(titleLocArgs: List<String>?): MessageNotification {
        this.titleLocArgs = titleLocArgs
        return this
    }

    /** The key to the title string in the app's string resources to use to localize the title text to the user's current localization.  */
    fun titleLocKey(titleLocKey: String?): MessageNotification {
        this.titleLocKey = titleLocKey
        return this
    }

    // Tracker methods
    override val dataPayload: Map<String, Any?>
        get() {
            val payload = HashMap<String, Any?>()
            payload[PARAM_TITLE] = title
            payload[PARAM_BODY] = body
            payload[PARAM_TRIGGER] = trigger.name
            action?.let { payload[PARAM_ACTION] = it }
            if (attachments != null && attachments!!.isNotEmpty()) {
                payload[PARAM_MESSAGENOTIFICATIONATTACHMENTS] = attachments
            }
            bodyLocArgs?.let { payload[PARAM_BODYLOCARGS] = it }
            bodyLocKey?.let { payload[PARAM_BODYLOCKEY] = it }
            category?.let { payload[PARAM_CATEGORY] = it }
            contentAvailable?.let { payload[PARAM_CONTENTAVAILABLE] = it }
            group?.let { payload[PARAM_GROUP] = it }
            icon?.let { payload[PARAM_ICON] = it }
            notificationCount?.let { payload[PARAM_NOTIFICATIONCOUNT] = it }
            notificationTimestamp?.let { payload[PARAM_NOTIFICATIONTIMESTAMP] = it }
            sound?.let { payload[PARAM_SOUND] = it }
            subtitle?.let { payload[PARAM_SUBTITLE] = it }
            tag?.let { payload[PARAM_TAG] = it }
            threadIdentifier?.let { payload[PARAM_THREADIDENTIFIER] = it }
            titleLocArgs?.let { payload[PARAM_TITLELOCARGS] = it }
            titleLocKey?.let { payload[PARAM_TITLELOCKEY] = it }

            return payload
        }

    companion object {
        const val schema = "iglu:com.snowplowanalytics.mobile/message_notification/jsonschema/1-0-0"
        const val PARAM_ACTION = "action"
        const val PARAM_MESSAGENOTIFICATIONATTACHMENTS = "attachments"
        const val PARAM_BODY = "body"
        const val PARAM_BODYLOCARGS = "bodyLocArgs"
        const val PARAM_BODYLOCKEY = "bodyLocKey"
        const val PARAM_CATEGORY = "category"
        const val PARAM_CONTENTAVAILABLE = "contentAvailable"
        const val PARAM_GROUP = "group"
        const val PARAM_ICON = "icon"
        const val PARAM_NOTIFICATIONCOUNT = "notificationCount"
        const val PARAM_NOTIFICATIONTIMESTAMP = "notificationTimestamp"
        const val PARAM_SOUND = "sound"
        const val PARAM_SUBTITLE = "subtitle"
        const val PARAM_TAG = "tag"
        const val PARAM_THREADIDENTIFIER = "threadIdentifier"
        const val PARAM_TITLE = "title"
        const val PARAM_TITLELOCARGS = "titleLocArgs"
        const val PARAM_TITLELOCKEY = "titleLocKey"
        const val PARAM_TRIGGER = "trigger"
    }
}
