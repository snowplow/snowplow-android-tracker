package com.snowplowanalytics.snowplow.internal.tracker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.internal.emitter.Emitter;
import com.snowplowanalytics.snowplow.internal.emitter.EmitterConfigurationUpdate;
import com.snowplowanalytics.snowplow.internal.emitter.EmitterControllerImpl;
import com.snowplowanalytics.snowplow.internal.emitter.NetworkConfigurationUpdate;
import com.snowplowanalytics.snowplow.internal.emitter.NetworkControllerImpl;
import com.snowplowanalytics.snowplow.internal.gdpr.GdprConfigurationUpdate;
import com.snowplowanalytics.snowplow.internal.gdpr.GdprControllerImpl;
import com.snowplowanalytics.snowplow.internal.globalcontexts.GlobalContextsControllerImpl;
import com.snowplowanalytics.snowplow.internal.session.SessionConfigurationUpdate;
import com.snowplowanalytics.snowplow.internal.session.SessionControllerImpl;

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
