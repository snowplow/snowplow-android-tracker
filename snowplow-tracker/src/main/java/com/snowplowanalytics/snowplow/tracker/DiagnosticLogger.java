package com.snowplowanalytics.snowplow.tracker;

public interface DiagnosticLogger {

    /**
     * Log tracker errors.
     *
     * @param source Where the error has been generated.
     * @param errorMessage A message to describe the error.
     * @param throwable A throwable that caused the error.
     */
    void log(String source, String errorMessage, Throwable throwable);
}
