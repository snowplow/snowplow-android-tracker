package com.snowplowanalytics.snowplow.controller;

import com.snowplowanalytics.snowplow.internal.emitter.EmitterConfigurationInterface;

public interface EmitterController extends EmitterConfigurationInterface {

    long getDbCount();

    boolean isSending();
}
