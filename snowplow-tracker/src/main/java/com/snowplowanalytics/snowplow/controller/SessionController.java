package com.snowplowanalytics.snowplow.controller;

import androidx.annotation.NonNull;

import com.snowplowanalytics.snowplow.internal.session.SessionConfigurationInterface;

public interface SessionController extends SessionConfigurationInterface {

    int getSessionIndex();
    @NonNull
    String getSessionId();
    @NonNull
    String getUserId();

    boolean isInBackground();
    int getBackgroundIndex();
    int getForegroundIndex();

    void pause();
    void resume();
    void startNewSession();
}
