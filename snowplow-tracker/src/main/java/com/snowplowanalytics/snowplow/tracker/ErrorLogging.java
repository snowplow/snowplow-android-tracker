package com.snowplowanalytics.snowplow.tracker;

public interface ErrorLogging {

    /**
     * Reports the internal tracker error to the app. It's just a log message as the error is
     * already handled by the tracker.
     * @param source Where the error has been generated.
     * @param errorMessage A message to describe the error.
     * @param throwable A throwable that caused the error.
     */
    void log(String source, String errorMessage, Throwable throwable);

}
