package com.snowplowanalytics.snowplow.event

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class MessageNotificationTest {
    @Test
    fun testExpectedForm() {
        val event = MessageNotification("title", "body", MessageNotificationTrigger.push)
            .notificationTimestamp("2020-12-31T15:59:60-08:00")
            .action("action")
            .bodyLocKey("loc key")
            .bodyLocArgs(Arrays.asList("loc arg1", "loc arg2"))
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
