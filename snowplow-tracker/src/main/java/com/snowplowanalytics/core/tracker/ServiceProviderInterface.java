package com.snowplowanalytics.core.tracker;

import androidx.annotation.NonNull;

import com.snowplowanalytics.core.emitter.Emitter;
import com.snowplowanalytics.core.emitter.EmitterConfigurationUpdate;
import com.snowplowanalytics.core.emitter.EmitterControllerImpl;
import com.snowplowanalytics.core.emitter.NetworkConfigurationUpdate;
import com.snowplowanalytics.core.emitter.NetworkControllerImpl;
import com.snowplowanalytics.core.gdpr.GdprConfigurationUpdate;
import com.snowplowanalytics.core.gdpr.GdprControllerImpl;
import com.snowplowanalytics.core.globalcontexts.GlobalContextsControllerImpl;
import com.snowplowanalytics.core.session.SessionConfigurationUpdate;
import com.snowplowanalytics.core.session.SessionControllerImpl;

public interface ServiceProviderInterface {

    @NonNull
    String getNamespace();

    // Internal services

    @NonNull
    Boolean isTrackerInitialized();

    @NonNull
    Tracker getOrMakeTracker();

    @NonNull
    Emitter getOrMakeEmitter();

    @NonNull
    Subject getOrMakeSubject();

    // Controllers

    @NonNull
    TrackerControllerImpl getOrMakeTrackerController();

    @NonNull
    EmitterControllerImpl getOrMakeEmitterController();

    @NonNull
    NetworkControllerImpl getOrMakeNetworkController();

    @NonNull
    GdprControllerImpl getOrMakeGdprController();

    @NonNull
    GlobalContextsControllerImpl getOrMakeGlobalContextsController();

    @NonNull
    SubjectControllerImpl getOrMakeSubjectController();

    @NonNull
    SessionControllerImpl getOrMakeSessionController();

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

    @NonNull
    GdprConfigurationUpdate getGdprConfigurationUpdate();
}
