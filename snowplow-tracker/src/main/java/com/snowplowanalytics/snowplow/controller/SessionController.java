package com.snowplowanalytics.snowplow.controller;

import androidx.annotation.NonNull;

import com.snowplowanalytics.snowplow.internal.session.SessionConfigurationInterface;

public interface SessionController extends SessionConfigurationInterface {

    /**
     * The session index.
     * A increasing number which helps to order the sequence of sessions.
     */
    int getSessionIndex();

    /**
     * The session identifier.
     * A unique identifier which is used to identify the session.
     */
    @NonNull
    String getSessionId();

    /**
     * The session user identifier.
     * It identifies this app installation and it doesn't change for the life of the app.
     * It will change only when the app is uninstalled and installed again.
     * An app update doesn't change the value.
     */
    @NonNull
    String getUserId();

    /**
     * Whether the app is currently in background state or in foreground state.
     */
    boolean isInBackground();

    /**
     * Count the number of background transitions in the current session.
     */
    int getBackgroundIndex();
    /**
     * Count the number of foreground transitions in the current session.
     */
    int getForegroundIndex();

    /**
     * Pause the session tracking.
     * Meanwhile the session is paused it can't expire and can't be updated.
     */
    void pause();

    /**
     * Resume the session tracking.
     */
    void resume();

    /**
     * Expire the current session also if the timeout is not triggered.
     */
    void startNewSession();
}
