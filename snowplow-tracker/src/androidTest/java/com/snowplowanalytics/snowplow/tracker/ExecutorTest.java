package com.snowplowanalytics.snowplow.tracker;

import android.test.AndroidTestCase;

import com.snowplowanalytics.snowplow.internal.emitter.Executor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ExecutorTest extends AndroidTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
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
