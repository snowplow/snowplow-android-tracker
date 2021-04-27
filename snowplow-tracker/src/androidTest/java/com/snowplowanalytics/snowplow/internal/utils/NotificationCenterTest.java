package com.snowplowanalytics.snowplow.internal.utils;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class NotificationCenterTest {

    @Before
    public void setUp() throws Exception {
        NotificationCenter.removeAll();
    }

    // Tests

    @Test
    public void testRegisterAndNotifyObserver() {
        // It's a final array because the lambda expression needs an external reference "effectively final".
        final Map<String, Object>[] testResult = new Map[]{null};

        // Add observer
        NotificationCenter.addObserver("notification", new NotificationCenter.FunctionalObserver() {
            @Override
            public void apply(@NonNull Map<String, Object> data) {
                testResult[0] = data;
            }
        });

        // Send notification
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");
        NotificationCenter.postNotification("notification", data);

        // Check result
        Assert.assertEquals(data, testResult[0]);
    }

    @Test
    public void testRegisterAndRemoveObserver() {
        // It's a final array because the lambda expression needs an external reference "effectively final".
        final Map<String, Object>[] testResult = new Map[]{null};

        // Add observer
        NotificationCenter.FunctionalObserver observer = new NotificationCenter.FunctionalObserver() {
            @Override
            public void apply(@NonNull Map<String, Object> data) {
                testResult[0] = data;
            }
        };
        NotificationCenter.addObserver("notification", observer);

        // Remove observer
        NotificationCenter.removeObserver(observer);

        // Send notification
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");
        NotificationCenter.postNotification("notification", data);

        // Check result
        Assert.assertNull(testResult[0]);
    }

    @Test
    public void testRegisterMultipleObserversForSameNotification() {
        // It's a final array because the lambda expression needs an external reference "effectively final".
        final Map<String, Object>[] testResult = new Map[]{null, null, null};

        // Add observer
        NotificationCenter.FunctionalObserver observer0 = new NotificationCenter.FunctionalObserver() {
            @Override
            public void apply(@NonNull Map<String, Object> data) {
                testResult[0] = data;
            }
        };
        NotificationCenter.FunctionalObserver observer1 = new NotificationCenter.FunctionalObserver() {
            @Override
            public void apply(@NonNull Map<String, Object> data) {
                testResult[1] = data;
            }
        };
        NotificationCenter.FunctionalObserver observer2 = new NotificationCenter.FunctionalObserver() {
            @Override
            public void apply(@NonNull Map<String, Object> data) {
                testResult[2] = data;
            }
        };
        NotificationCenter.addObserver("notification", observer0);
        NotificationCenter.addObserver("notification", observer1);
        NotificationCenter.addObserver("notification", observer2);

        // Remove observer
        NotificationCenter.removeObserver(observer1);

        // Send notification
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");
        NotificationCenter.postNotification("notification", data);

        // Check result
        Assert.assertEquals(data, testResult[0]);
        Assert.assertNull(testResult[1]);
        Assert.assertEquals(data, testResult[2]);
    }

    @Test
    public void testRegisterMultipleObserversForMultipleNotifications() {
        // It's a final array because the lambda expression needs an external reference "effectively final".
        final Map<String, Object>[] testResult = new Map[]{null, null, null, null, null, null};

        // Add observers
        NotificationCenter.FunctionalObserver observer0 = new NotificationCenter.FunctionalObserver() {
            @Override
            public void apply(@NonNull Map<String, Object> data) {
                testResult[0] = data;
            }
        };
        NotificationCenter.FunctionalObserver observer1 = new NotificationCenter.FunctionalObserver() {
            @Override
            public void apply(@NonNull Map<String, Object> data) {
                testResult[1] = data;
            }
        };
        NotificationCenter.FunctionalObserver observer2 = new NotificationCenter.FunctionalObserver() {
            @Override
            public void apply(@NonNull Map<String, Object> data) {
                testResult[2] = data;
            }
        };
        NotificationCenter.FunctionalObserver observer3 = new NotificationCenter.FunctionalObserver() {
            @Override
            public void apply(@NonNull Map<String, Object> data) {
                testResult[3] = data;
            }
        };
        NotificationCenter.FunctionalObserver observer4 = new NotificationCenter.FunctionalObserver() {
            @Override
            public void apply(@NonNull Map<String, Object> data) {
                testResult[4] = data;
            }
        };
        NotificationCenter.FunctionalObserver observer5 = new NotificationCenter.FunctionalObserver() {
            @Override
            public void apply(@NonNull Map<String, Object> data) {
                testResult[5] = data;
            }
        };
        NotificationCenter.addObserver("notification1", observer0);
        NotificationCenter.addObserver("notification1", observer1);
        NotificationCenter.addObserver("notification1", observer2);
        NotificationCenter.addObserver("notification2", observer3);
        NotificationCenter.addObserver("notification2", observer4);
        NotificationCenter.addObserver("notification2", observer5);

        // Remove observers
        NotificationCenter.removeObserver(observer1);
        NotificationCenter.removeObserver(observer4);

        // Send notification1
        Map<String, Object> data1 = new HashMap<>();
        data1.put("key1", "value1");
        NotificationCenter.postNotification("notification1", data1);

        // Check result
        Assert.assertEquals(data1, testResult[0]);
        Assert.assertNull(testResult[1]);
        Assert.assertEquals(data1, testResult[2]);

        // Send notification2
        Map<String, Object> data2 = new HashMap<>();
        data2.put("key2", "value2");
        NotificationCenter.postNotification("notification2", data2);

        // Check result
        Assert.assertEquals(data2, testResult[3]);
        Assert.assertNull(testResult[4]);
        Assert.assertEquals(data2, testResult[5]);
    }
}
