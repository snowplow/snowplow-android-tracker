package com.snowplowanalytics.snowplow.internal.tracker;

import androidx.annotation.NonNull;

import com.snowplowanalytics.snowplow.internal.emitter.Emitter;
import com.snowplowanalytics.snowplow.internal.emitter.EmitterControllerImpl;
import com.snowplowanalytics.snowplow.internal.emitter.NetworkControllerImpl;
import com.snowplowanalytics.snowplow.internal.gdpr.GdprControllerImpl;
import com.snowplowanalytics.snowplow.internal.globalcontexts.GlobalContextsControllerImpl;
import com.snowplowanalytics.snowplow.internal.session.SessionControllerImpl;

public interface ServiceProviderInterface {

    @NonNull
    public String getNamespace();

    @NonNull
    public Tracker getTracker();

    @NonNull
    public Emitter getEmitter();

    @NonNull
    public Subject getSubject();

    @NonNull
    public TrackerControllerImpl getTrackerController();

    @NonNull
    public EmitterControllerImpl getEmitterController();

    @NonNull
    public NetworkControllerImpl getNetworkController();

    @NonNull
    public GdprControllerImpl getGdprController();

    @NonNull
    public GlobalContextsControllerImpl getGlobalContextsController();

    @NonNull
    public SubjectControllerImpl getSubjectController();

    @NonNull
    public SessionControllerImpl getSessionController();
}
