package com.snowplowanalytics.snowplow.internal.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.snowplowanalytics.core.utils.NotificationCenter.removeAll
import com.snowplowanalytics.core.utils.NotificationCenter.addObserver
import com.snowplowanalytics.core.utils.NotificationCenter.postNotification
import com.snowplowanalytics.core.utils.NotificationCenter.removeObserver
import kotlin.Throws
import com.snowplowanalytics.core.utils.NotificationCenter.FunctionalObserver
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.Exception
import java.util.HashMap

@RunWith(AndroidJUnit4::class)
class NotificationCenterTest {
    @Before
    @Throws(Exception::class)
    fun setUp() {
        removeAll()
    }

    // Tests
    @Test
    fun testRegisterAndNotifyObserver() {
        // It's a final array because the lambda expression needs an external reference "effectively final".
        val testResult: Array<Map<String, Any>?> = arrayOf(null)

        // Add observer
        addObserver("notification", object : FunctionalObserver() {
            override fun apply(data: Map<String, Any>) {
                testResult[0] = data
            }
        })

        // Send notification
        val data: MutableMap<String, Any> = HashMap()
        data["key"] = "value"
        postNotification("notification", data)

        // Check result
        Assert.assertEquals(data, testResult[0])
    }

    @Test
    fun testRegisterAndRemoveObserver() {
        // It's a final array because the lambda expression needs an external reference "effectively final".
        val testResult: Array<Map<String, Any>?> = arrayOf(null)

        // Add observer
        val observer: FunctionalObserver = object : FunctionalObserver() {
            override fun apply(data: Map<String, Any>) {
                testResult[0] = data
            }
        }
        addObserver("notification", observer)

        // Remove observer
        removeObserver(observer)

        // Send notification
        val data: MutableMap<String, Any> = HashMap()
        data["key"] = "value"
        postNotification("notification", data)

        // Check result
        Assert.assertNull(testResult[0])
    }

    @Test
    fun testRegisterMultipleObserversForSameNotification() {
        // It's a final array because the lambda expression needs an external reference "effectively final".
        val testResult: Array<Map<String, Any>?> = arrayOf(null, null, null)

        // Add observer
        val observer0: FunctionalObserver = object : FunctionalObserver() {
            override fun apply(data: Map<String, Any>) {
                testResult[0] = data
            }
        }
        val observer1: FunctionalObserver = object : FunctionalObserver() {
            override fun apply(data: Map<String, Any>) {
                testResult[1] = data
            }
        }
        val observer2: FunctionalObserver = object : FunctionalObserver() {
            override fun apply(data: Map<String, Any>) {
                testResult[2] = data
            }
        }
        addObserver("notification", observer0)
        addObserver("notification", observer1)
        addObserver("notification", observer2)

        // Remove observer
        removeObserver(observer1)

        // Send notification
        val data: MutableMap<String, Any> = HashMap()
        data["key"] = "value"
        postNotification("notification", data)

        // Check result
        Assert.assertEquals(data, testResult[0])
        Assert.assertNull(testResult[1])
        Assert.assertEquals(data, testResult[2])
    }

    @Test
    fun testRegisterMultipleObserversForMultipleNotifications() {
        // It's a final array because the lambda expression needs an external reference "effectively final".
        val testResult: Array<Map<String, Any>?> =
            arrayOf(null, null, null, null, null, null)

        // Add observers
        val observer0: FunctionalObserver = object : FunctionalObserver() {
            override fun apply(data: Map<String, Any>) {
                testResult[0] = data
            }
        }
        val observer1: FunctionalObserver = object : FunctionalObserver() {
            override fun apply(data: Map<String, Any>) {
                testResult[1] = data
            }
        }
        val observer2: FunctionalObserver = object : FunctionalObserver() {
            override fun apply(data: Map<String, Any>) {
                testResult[2] = data
            }
        }
        val observer3: FunctionalObserver = object : FunctionalObserver() {
            override fun apply(data: Map<String, Any>) {
                testResult[3] = data
            }
        }
        val observer4: FunctionalObserver = object : FunctionalObserver() {
            override fun apply(data: Map<String, Any>) {
                testResult[4] = data
            }
        }
        val observer5: FunctionalObserver = object : FunctionalObserver() {
            override fun apply(data: Map<String, Any>) {
                testResult[5] = data
            }
        }
        addObserver("notification1", observer0)
        addObserver("notification1", observer1)
        addObserver("notification1", observer2)
        addObserver("notification2", observer3)
        addObserver("notification2", observer4)
        addObserver("notification2", observer5)

        // Remove observers
        removeObserver(observer1)
        removeObserver(observer4)

        // Send notification1
        val data1: MutableMap<String, Any> = HashMap()
        data1["key1"] = "value1"
        postNotification("notification1", data1)

        // Check result
        Assert.assertEquals(data1, testResult[0])
        Assert.assertNull(testResult[1])
        Assert.assertEquals(data1, testResult[2])

        // Send notification2
        val data2: MutableMap<String, Any> = HashMap()
        data2["key2"] = "value2"
        postNotification("notification2", data2)

        // Check result
        Assert.assertEquals(data2, testResult[3])
        Assert.assertNull(testResult[4])
        Assert.assertEquals(data2, testResult[5])
    }
}
