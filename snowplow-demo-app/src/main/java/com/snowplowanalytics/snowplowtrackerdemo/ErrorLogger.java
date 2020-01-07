package com.snowplowanalytics.snowplowtrackerdemo;

import android.util.Log;

import com.snowplowanalytics.snowplow.tracker.ErrorLogging;

public class ErrorLogger implements ErrorLogging {
    @Override
    public void log(String source, String errorMessage) {
        if (source.equals("Tracker") || source.equals("Session") || source.equals("ProcessObserver")) {
            Log.e("Error reported >>> " + source, errorMessage);
        }
    }
}
