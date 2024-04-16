/*
 * Copyright (c) 2015-present Snowplow Analytics Ltd. All rights reserved.
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
        val expectation = Object()
        val exceptionRaised = AtomicBoolean(false)
        execute({ throw NullPointerException() }) { t: Throwable? ->
            exceptionRaised.set(t is NullPointerException)
            synchronized(expectation) { expectation.notify() }
        }
        synchronized(expectation) { expectation.wait(10000) }
        Assert.assertTrue(exceptionRaised.get())
    }
}
