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
package com.snowplowanalytics.snowplow.event

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MessageNotificationTest {
    @Test
    fun testExpectedForm() {
        val event = MessageNotification("title", "body", MessageNotificationTrigger.push)
            .notificationTimestamp("2020-12-31T15:59:60-08:00")
            .action("action")
            .bodyLocKey("loc key")
            .bodyLocArgs(listOf("loc arg1", "loc arg2"))
            .sound("chime.mp3")
            .notificationCount(9)
            .category("category1")
            .attachments(listOf(MessageNotificationAttachment("id", "type", "url")))
        val payload = event.dataPayload
        Assert.assertNotNull(payload)
        Assert.assertEquals("title", payload[MessageNotification.PARAM_TITLE])
        Assert.assertEquals("body", payload[MessageNotification.PARAM_BODY])
        Assert.assertEquals("push", payload[MessageNotification.PARAM_TRIGGER])
        Assert.assertEquals(
            "2020-12-31T15:59:60-08:00",
            payload[MessageNotification.PARAM_NOTIFICATIONTIMESTAMP]
        )
        Assert.assertEquals("action", payload[MessageNotification.PARAM_ACTION])
        Assert.assertEquals("loc key", payload[MessageNotification.PARAM_BODYLOCKEY])
        val locArgs = payload[MessageNotification.PARAM_BODYLOCARGS] as List<String>?
        Assert.assertNotNull(locArgs)
        Assert.assertEquals(2, locArgs!!.size.toLong())
        Assert.assertEquals("loc arg1", locArgs[0])
        Assert.assertEquals("loc arg2", locArgs[1])
        Assert.assertEquals("chime.mp3", payload[MessageNotification.PARAM_SOUND])
        Assert.assertEquals(9, payload[MessageNotification.PARAM_NOTIFICATIONCOUNT])
        Assert.assertEquals("category1", payload[MessageNotification.PARAM_CATEGORY])
        val attachments =
            payload[MessageNotification.PARAM_MESSAGENOTIFICATIONATTACHMENTS] as List<MessageNotificationAttachment>?
        Assert.assertNotNull(attachments)
        Assert.assertEquals(1, attachments!!.size.toLong())
        val attachment = attachments[0]
        Assert.assertEquals("id", attachment[MessageNotificationAttachment.PARAM_IDENTIFIER])
        Assert.assertEquals("type", attachment[MessageNotificationAttachment.PARAM_TYPE])
        Assert.assertEquals("url", attachment[MessageNotificationAttachment.PARAM_URL])
    }
}
