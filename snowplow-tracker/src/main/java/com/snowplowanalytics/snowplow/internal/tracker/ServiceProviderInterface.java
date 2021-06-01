package com.snowplowanalytics.snowplow.internal.tracker;

import androidx.annotation.NonNull;

import com.snowplowanalytics.snowplow.internal.emitter.Emitter;
import com.snowplowanalytics.snowplow.internal.emitter.EmitterConfigurationUpdate;
import com.snowplowanalytics.snowplow.internal.emitter.EmitterControllerImpl;
import com.snowplowanalytics.snowplow.internal.emitter.NetworkConfigurationUpdate;
import com.snowplowanalytics.snowplow.internal.emitter.NetworkControllerImpl;
import com.snowplowanalytics.snowplow.internal.gdpr.GdprControllerImpl;
import com.snowplowanalytics.snowplow.internal.globalcontexts.GlobalContextsControllerImpl;
import com.snowplowanalytics.snowplow.internal.session.SessionConfigurationUpdate;
import com.snowplowanalytics.snowplow.internal.session.SessionControllerImpl;

public interface ServiceProviderInterface {

    @NonNull
    String getNamespace();

    // Internal services

    @NonNull
    Tracker getTracker();

    @NonNull
    Emitter getEmitter();

    @NonNull
    Subject getSubject();

    // Controllers

    @NonNull
    TrackerControllerImpl getTrackerController();

    @NonNull
    EmitterControllerImpl getEmitterController();

    @NonNull
    NetworkControllerImpl getNetworkController();

    @NonNull
    GdprControllerImpl getGdprController();

    @NonNull
    GlobalContextsControllerImpl getGlobalContextsController();

    @NonNull
    SubjectControllerImpl getSubjectController();

    @NonNull
    SessionControllerImpl getSessionController();

    // Configuration Updates

    @NonNull
    TrackerConfigurationUpdate getTrackerConfigurationUpdate();

    @NonNull
    NetworkConfigurationUpdate getNetworkConfigurationUpdate();

    @NonNull
    SubjectConfigurationUpdate getSubjectConfigurationUpdate();

    @NonNull
    EmitterConfigurationUpdate getEmitterConfigurationUpdate();

    @NonNull
    SessionConfigurationUpdate getSessionConfigurationUpdate();
}
