package com.snowplowanalytics.snowplow.tracker;

public interface LoggerDelegate {

    /**
     * Error Level Logging
     *
     * @param tag the log tag
     * @param msg the log message
     */
    void error(String tag, String msg);

    /**
     * Debug Level Logging
     *
     * @param tag the log tag
     * @param msg the log message
     */
    void debug(String tag, String msg);

    /**
     * Verbose Level Logging
     *
     * @param tag the log tag
     * @param msg the log message
     */
    void verbose(String tag, String msg);
}
