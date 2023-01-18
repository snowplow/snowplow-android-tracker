package com.snowplowanalytics.snowplow.tracker

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.snowplowanalytics.snowplow.network.RequestResult
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class RequestResultTest {
    @Test
    fun testSuccessfulRequest() {
        val result = RequestResult(200, false, listOf(100L))
        Assert.assertTrue(result.isSuccessful)
        Assert.assertFalse(result.isOversize)
        Assert.assertFalse(result.shouldRetry(HashMap()))
        Assert.assertEquals(result.eventIds, listOf(100L))
    }

    @Test
    fun testFailedRequest() {
        val result = RequestResult(500, false, ArrayList())
        Assert.assertFalse(result.isSuccessful)
        Assert.assertTrue(result.shouldRetry(HashMap()))
    }

    @Test
    fun testOversizedFailedRequest() {
        val result = RequestResult(500, true, ArrayList())
        Assert.assertFalse(result.isSuccessful)
        Assert.assertFalse(result.shouldRetry(HashMap()))
    }

    @Test
    fun testFailedRequestWithNoRetryStatus() {
        val result = RequestResult(403, false, ArrayList())
        Assert.assertFalse(result.isSuccessful)
        Assert.assertFalse(result.shouldRetry(HashMap()))
    }

    @Test
    fun testFailedRequestWithCustomRetryRules() {
        val customRetry: MutableMap<Int, Boolean> = HashMap()
        customRetry[403] = true
        customRetry[500] = false
        var result = RequestResult(403, false, ArrayList())
        Assert.assertFalse(result.isSuccessful)
        Assert.assertTrue(result.shouldRetry(customRetry))
        result = RequestResult(500, false, ArrayList())
        Assert.assertFalse(result.isSuccessful)
        Assert.assertFalse(result.shouldRetry(customRetry))
    }
}
