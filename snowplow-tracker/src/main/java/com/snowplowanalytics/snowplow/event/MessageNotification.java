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

package com.snowplowanalytics.snowplow.event;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Event that represents the reception of a push notification (or a locally generated one). */
public class MessageNotification extends AbstractSelfDescribing {

    public final static String SCHEMA = "iglu:com.snowplowanalytics.mobile/message_notification/jsonschema/1-0-0";

    public final static String PARAM_ACTION = "action";
    public final static String PARAM_MESSAGENOTIFICATIONATTACHMENTS = "attachments";
    public final static String PARAM_BODY = "body";
    public final static String PARAM_BODYLOCARGS = "bodyLocArgs";
    public final static String PARAM_BODYLOCKEY = "bodyLocKey";
    public final static String PARAM_CATEGORY = "category";
    public final static String PARAM_CONTENTAVAILABLE = "contentAvailable";
    public final static String PARAM_GROUP = "group";
    public final static String PARAM_ICON = "icon";
    public final static String PARAM_NOTIFICATIONCOUNT = "notificationCount";
    public final static String PARAM_NOTIFICATIONTIMESTAMP = "notificationTimestamp";
    public final static String PARAM_SOUND = "sound";
    public final static String PARAM_SUBTITLE = "subtitle";
    public final static String PARAM_TAG = "tag";
    public final static String PARAM_THREADIDENTIFIER = "threadIdentifier";
    public final static String PARAM_TITLE = "title";
    public final static String PARAM_TITLELOCARGS = "titleLocArgs";
    public final static String PARAM_TITLELOCKEY = "titleLocKey";
    public final static String PARAM_TRIGGER = "trigger";

    /** The action associated with the notification. */
    @Nullable
    public String action;
    /** Attachments added to the notification (they can be part of the data object). */
    @Nullable
    public List<MessageNotificationAttachment> attachments;
    /** The notification's body. */
    @NonNull
    public final String body;
    /** Variable string values to be used in place of the format specifiers in bodyLocArgs to use to localize the body text to the user's current localization. */
    @Nullable
    public List<String> bodyLocArgs;
    /** The key to the body string in the app's string resources to use to localize the body text to the user's current localization. */
    @Nullable
    public String bodyLocKey;
    /** The category associated to the notification. */
    @Nullable
    public String category;
    /** The application is notified of the delivery of the notification if it's in the foreground or background, the app will be woken up (iOS only). */
    @Nullable
    public Boolean contentAvailable;
    /** The group which this notification is part of. */
    @Nullable
    public String group;
    /** The icon associated to the notification (Android only). */
    @Nullable
    public String icon;
    /** The number of items this notification represents. */
    @Nullable
    public Integer notificationCount;
    /** The time when the event of the notification occurred. */
    @Nullable
    public String notificationTimestamp;
    /** The sound played when the device receives the notification. */
    @Nullable
    public String sound;
    /** The notification's subtitle. (iOS only) */
    @Nullable
    public String subtitle;
    /** An identifier similar to 'group' but usable for different purposes (Android only). */
    @Nullable
    public String tag;
    /** An identifier similar to 'group' but usable for different purposes (iOS only). */
    @Nullable
    public String threadIdentifier;
    /** The notification's title. */
    @NonNull
    public final String title;
    /** Variable string values to be used in place of the format specifiers in titleLocArgs to use to localize the title text to the user's current localization. */
    @Nullable
    public List<String> titleLocArgs;
    /** The key to the title string in the app's string resources to use to localize the title text to the user's current localization. */
    @Nullable
    public String titleLocKey;
    /** The trigger that raised the notification message. */
    @NonNull
    public final MessageNotificationTrigger trigger;

    /**
     Creates a Message Notification event that represents a push notification or a local notification.
     @note The custom data of the push notification have to be tracked separately in custom entities that can be attached to this event.
     @param title Title of message notification.
     @param body Body content of the message notification.
     @param trigger The trigger that raised this notification: remote notification (push), position related (location), date-time related (calendar, timeInterval) or app generated (other).
     */
    public MessageNotification(@NonNull String title, @NonNull String body, @NonNull MessageNotificationTrigger trigger) {
        this.title = title;
        this.body = body;
        this.trigger = trigger;
    }

    // Builder methods

    /** The action associated with the notification. */
    @NonNull
    public MessageNotification action(@Nullable String action) {
        this.action = action;
        return this;
    }

    /** Attachments added to the notification (they can be part of the data object). */
    @NonNull
    public MessageNotification attachments(@Nullable List<MessageNotificationAttachment> attachments) {
        this.attachments = attachments;
        return this;
    }

    /** Variable string values to be used in place of the format specifiers in bodyLocArgs to use to localize the body text to the user's current localization. */
    @NonNull
    public MessageNotification bodyLocArgs(@Nullable List<String> bodyLocArgs) {
        this.bodyLocArgs = bodyLocArgs;
        return this;
    }

    /** The key to the body string in the app's string resources to use to localize the body text to the user's current localization. */
    @NonNull
    public MessageNotification bodyLocKey(@Nullable String bodyLocKey) {
        this.bodyLocKey = bodyLocKey;
        return this;
    }

    /** The category associated to the notification. */
    @NonNull
    public MessageNotification category(@Nullable String category) {
        this.category = category;
        return this;
    }

    /** The application is notified of the delivery of the notification if it's in the foreground or background, the app will be woken up (iOS only). */
    @NonNull
    public MessageNotification contentAvailable(@Nullable Boolean contentAvailable) {
        this.contentAvailable = contentAvailable;
        return this;
    }

    /** The group which this notification is part of. */
    @NonNull
    public MessageNotification group(@Nullable String group) {
        this.group = group;
        return this;
    }

    /** The icon associated to the notification (Android only). */
    @NonNull
    public MessageNotification icon(@Nullable String icon) {
        this.icon = icon;
        return this;
    }

    /** The number of items this notification represents. */
    @NonNull
    public MessageNotification notificationCount(@Nullable Integer notificationCount) {
        this.notificationCount = notificationCount;
        return this;
    }

    /** The time when the event of the notification occurred. */
    @NonNull
    public MessageNotification notificationTimestamp(@Nullable String notificationTimestamp) {
        this.notificationTimestamp = notificationTimestamp;
        return this;
    }

    /** The sound played when the device receives the notification. */
    @NonNull
    public MessageNotification sound(@Nullable String sound) {
        this.sound = sound;
        return this;
    }

    /** The notification's subtitle. (iOS only) */
    @NonNull
    public MessageNotification subtitle(@Nullable String subtitle) {
        this.subtitle = subtitle;
        return this;
    }

    /** An identifier similar to 'group' but usable for different purposes (Android only). */
    @NonNull
    public MessageNotification tag(@Nullable String tag) {
        this.tag = tag;
        return this;
    }

    /** An identifier similar to 'group' but usable for different purposes (iOS only). */
    @NonNull
    public MessageNotification threadIdentifier(@Nullable String threadIdentifier) {
        this.threadIdentifier = threadIdentifier;
        return this;
    }

    /** Variable string values to be used in place of the format specifiers in titleLocArgs to use to localize the title text to the user's current localization. */
    @NonNull
    public MessageNotification titleLocArgs(@Nullable List<String> titleLocArgs) {
        this.titleLocArgs = titleLocArgs;
        return this;
    }

    /** The key to the title string in the app's string resources to use to localize the title text to the user's current localization. */
    @NonNull
    public MessageNotification titleLocKey(@Nullable String titleLocKey) {
        this.titleLocKey = titleLocKey;
        return this;
    }

    // Tracker methods

    @Override
    public @NonNull Map<String, Object> getDataPayload() {
        HashMap<String,Object> payload = new HashMap<>();
        payload.put(PARAM_TITLE, title);
        payload.put(PARAM_BODY, body);
        payload.put(PARAM_TRIGGER, trigger.name());
        if (action != null) {
            payload.put(PARAM_ACTION, action);
        }
        if (attachments != null && attachments.size() > 0) {
            payload.put(PARAM_MESSAGENOTIFICATIONATTACHMENTS, attachments);
        }
        if (bodyLocArgs != null) {
            payload.put(PARAM_BODYLOCARGS, bodyLocArgs);
        }
        if (bodyLocKey != null) {
            payload.put(PARAM_BODYLOCKEY, bodyLocKey);
        }
        if (category != null) {
            payload.put(PARAM_CATEGORY, category);
        }
        if (contentAvailable != null) {
            payload.put(PARAM_CONTENTAVAILABLE, contentAvailable);
        }
        if (group != null) {
            payload.put(PARAM_GROUP, group);
        }
        if (icon != null) {
            payload.put(PARAM_ICON, icon);
        }
        if (notificationCount != null) {
            payload.put(PARAM_NOTIFICATIONCOUNT, notificationCount);
        }
        if (notificationTimestamp != null) {
            payload.put(PARAM_NOTIFICATIONTIMESTAMP, notificationTimestamp);
        }
        if (sound != null) {
            payload.put(PARAM_SOUND, sound);
        }
        if (subtitle != null) {
            payload.put(PARAM_SUBTITLE, subtitle);
        }
        if (tag != null) {
            payload.put(PARAM_TAG, tag);
        }
        if (threadIdentifier != null) {
            payload.put(PARAM_THREADIDENTIFIER, threadIdentifier);
        }
        if (titleLocArgs != null) {
            payload.put(PARAM_TITLELOCARGS, titleLocArgs);
        }
        if (titleLocKey != null) {
            payload.put(PARAM_TITLELOCKEY, titleLocKey);
        }
        return payload;
    }

    @Override
    public @NonNull String getSchema() {
        return SCHEMA;
    }
}

