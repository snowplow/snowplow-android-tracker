package com.snowplowanalytics.snowplow.event;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class MessageNotificationTest {

    @Test
    public void testExpectedForm() {
        MessageNotification event = new MessageNotification("title", "body", MessageNotificationTrigger.push)
                .notificationTimestamp("2020-12-31T15:59:60-08:00")
                .action("action")
                .bodyLocKey("loc key")
                .bodyLocArgs(Arrays.asList("loc arg1", "loc arg2"))
                .sound("chime.mp3")
                .notificationCount(9)
                .category("category1")
                .attachments(Arrays.asList(new MessageNotificationAttachment("id", "type", "url")));

        Map<String, Object> payload = event.getDataPayload();
        assertNotNull(payload);
        assertEquals("title", payload.get(MessageNotification.PARAM_TITLE));
        assertEquals("body", payload.get(MessageNotification.PARAM_BODY));
        assertEquals("push", payload.get(MessageNotification.PARAM_TRIGGER));
        assertEquals("2020-12-31T15:59:60-08:00", payload.get(MessageNotification.PARAM_NOTIFICATIONTIMESTAMP));
        assertEquals("action", payload.get(MessageNotification.PARAM_ACTION));
        assertEquals("loc key", payload.get(MessageNotification.PARAM_BODYLOCKEY));
        List<String> locArgs = (List<String>)payload.get(MessageNotification.PARAM_BODYLOCARGS);
        assertNotNull(locArgs);
        assertEquals(2, locArgs.size());
        assertEquals("loc arg1", locArgs.get(0));
        assertEquals("loc arg2", locArgs.get(1));
        assertEquals("chime.mp3", payload.get(MessageNotification.PARAM_SOUND));
        assertEquals(9, payload.get(MessageNotification.PARAM_NOTIFICATIONCOUNT));
        assertEquals("category1", payload.get(MessageNotification.PARAM_CATEGORY));
        List<MessageNotificationAttachment> attachments = (List<MessageNotificationAttachment>)payload.get(MessageNotification.PARAM_MESSAGENOTIFICATIONATTACHMENTS);
        assertNotNull(attachments);
        assertEquals(1, attachments.size());
        MessageNotificationAttachment attachment = attachments.get(0);
        assertEquals("id", attachment.get(MessageNotificationAttachment.PARAM_IDENTIFIER));
        assertEquals("type", attachment.get(MessageNotificationAttachment.PARAM_TYPE));
        assertEquals("url", attachment.get(MessageNotificationAttachment.PARAM_URL));
    }
}
