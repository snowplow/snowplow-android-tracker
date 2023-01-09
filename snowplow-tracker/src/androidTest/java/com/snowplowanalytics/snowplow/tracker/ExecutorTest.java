package com.snowplowanalytics.snowplow.tracker;

import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.snowplowanalytics.core.emitter.Executor;

import org.junit.Before;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(AndroidJUnit4.class)
public class ExecutorTest {

    @Before
    public void setUp() throws Exception {
        ExecutorService es = Executor.shutdown();
        if (es != null) {
            es.awaitTermination(60, TimeUnit.SECONDS);
        }
    }

    public void testExecutorRaisingException() throws InterruptedException {
        final Object expectation = new Object();
        AtomicBoolean exceptionRaised = new AtomicBoolean(false);
        Executor.execute(() -> { throw new NullPointerException(); }, t -> {
            exceptionRaised.set(t instanceof NullPointerException);
            synchronized (expectation) {
                expectation.notify();
            }
        });
        synchronized (expectation) {
            expectation.wait(10000);
        }
        assertTrue(exceptionRaised.get());
    }
}
