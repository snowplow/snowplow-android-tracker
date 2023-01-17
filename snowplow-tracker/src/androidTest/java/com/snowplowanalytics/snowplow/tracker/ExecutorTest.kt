package com.snowplowanalytics.snowplow.tracker

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.snowplowanalytics.core.emitter.Executor.shutdown
import com.snowplowanalytics.core.emitter.Executor.execute
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.Exception
import java.lang.NullPointerException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.Throws

@RunWith(AndroidJUnit4::class)
class ExecutorTest {
    @Before
    @Throws(Exception::class)
    fun setUp() {
        val es = shutdown()
        es?.awaitTermination(60, TimeUnit.SECONDS)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testExecutorRaisingException() {
        val expectation = Any() as Object
        val exceptionRaised = AtomicBoolean(false)
        execute({ throw NullPointerException() }) { t: Throwable? ->
            exceptionRaised.set(t is NullPointerException)
            synchronized(expectation) { expectation.notify() }
        }
        synchronized(expectation) { expectation.wait(10000) }
        Assert.assertTrue(exceptionRaised.get())
    }
}
