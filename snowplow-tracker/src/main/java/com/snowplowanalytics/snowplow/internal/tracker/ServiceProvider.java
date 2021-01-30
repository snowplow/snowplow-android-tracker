package com.snowplowanalytics.snowplow.internal.tracker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.configuration.Configuration;
import com.snowplowanalytics.snowplow.configuration.EmitterConfiguration;
import com.snowplowanalytics.snowplow.configuration.GdprConfiguration;
import com.snowplowanalytics.snowplow.configuration.GlobalContextsConfiguration;
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration;
import com.snowplowanalytics.snowplow.configuration.SessionConfiguration;
import com.snowplowanalytics.snowplow.configuration.SubjectConfiguration;
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration;
import com.snowplowanalytics.snowplow.controller.TrackerController;
import com.snowplowanalytics.snowplow.internal.emitter.Emitter;
import com.snowplowanalytics.snowplow.network.HttpMethod;
import com.snowplowanalytics.snowplow.network.Protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ServiceProvider {

    @NonNull
    private final Context context;
    @NonNull
    private final NetworkConfiguration networkConfiguration;
    @NonNull
    private final TrackerConfiguration trackerConfiguration;
    @Nullable
    private EmitterConfiguration emitterConfiguration;
    @Nullable
    private SubjectConfiguration subjectConfiguration;
    @Nullable
    private SessionConfiguration sessionConfiguration;
    @Nullable
    private GdprConfiguration gdprConfiguration;
    @Nullable
    private GlobalContextsConfiguration globalContextsConfiguration;

    @Nullable
    private Tracker tracker;
    @Nullable
    private Emitter emitter;
    @Nullable
    private Subject subject;
    @Nullable
    private TrackerController trackerController;

    // Constructors

    private ServiceProvider(@NonNull Context context, @NonNull NetworkConfiguration networkConfiguration, @NonNull TrackerConfiguration trackerConfiguration, @NonNull List<Configuration> configurations) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(networkConfiguration);
        Objects.requireNonNull(trackerConfiguration);
        Objects.requireNonNull(configurations);
        this.context = context;
        this.networkConfiguration = networkConfiguration;
        this.trackerConfiguration = trackerConfiguration;
        for (Configuration configuration : configurations) {
            if (configuration instanceof SubjectConfiguration) {
                subjectConfiguration = (SubjectConfiguration)configuration;
                continue;
            }
            if (configuration instanceof SessionConfiguration) {
                sessionConfiguration = (SessionConfiguration)configuration;
                continue;
            }
            if (configuration instanceof EmitterConfiguration) {
                emitterConfiguration = (EmitterConfiguration)configuration;
                continue;
            }
            if (configuration instanceof GdprConfiguration) {
                gdprConfiguration = (GdprConfiguration)configuration;
                continue;
            }
            if (configuration instanceof GlobalContextsConfiguration) {
                globalContextsConfiguration = (GlobalContextsConfiguration)configuration;
                continue;
            }
        }
    }

    // Setup

    @NonNull
    public static TrackerController setup(@NonNull Context context, @NonNull String endpoint, @NonNull Protocol protocol, @NonNull HttpMethod method, @NonNull String namespace, @NonNull String appId) {
        NetworkConfiguration networkConfiguration = new NetworkConfiguration(endpoint, protocol, method);
        TrackerConfiguration trackerConfiguration = new TrackerConfiguration(namespace, appId);
        return setup(context, networkConfiguration, trackerConfiguration);
    }

    @NonNull
    public static TrackerController setup(@NonNull Context context, @NonNull NetworkConfiguration networkConfiguration, @NonNull TrackerConfiguration trackerConfiguration) {
        return setup(context, networkConfiguration, trackerConfiguration, new ArrayList<>());
    }

    @NonNull
    public static TrackerController setup(@NonNull Context context, @NonNull NetworkConfiguration networkConfiguration, @NonNull TrackerConfiguration trackerConfiguration, @NonNull List<Configuration> configurations) {
        ServiceProvider serviceProvider = new ServiceProvider(context, networkConfiguration, trackerConfiguration, configurations);
        return serviceProvider.getTrackerController();
    }

    // Getters

    @NonNull
    private Subject getSubject() {
        if (subject == null) {
            subject = makeSubject();
        }
        return subject;
    }

    @NonNull
    private Emitter getEmitter() {
        if (emitter == null) {
            emitter = makeEmitter();
        }
        return emitter;
    }

    @NonNull
    private Tracker getTracker() {
        if (tracker == null) {
            tracker = makeTracker();
        }
        return tracker;
    }

    @NonNull
    public TrackerController getTrackerController() {
        if (trackerController == null) {
            trackerController = makeTrackerController();
        }
        return trackerController;
    }

    // Factories

    @NonNull
    private Subject makeSubject() {
        return new Subject.SubjectBuilder()
                .context(context)
                .subjectConfiguration(subjectConfiguration)
                .build();
    }

    @NonNull
    private Emitter makeEmitter() {
        Emitter.EmitterBuilder builder = new Emitter.EmitterBuilder(networkConfiguration.getEndpoint(), context)
                .networkConnection(networkConfiguration.networkConnection)
                .method(networkConfiguration.getMethod())
                .security(networkConfiguration.getProtocol())
                .customPostPath(networkConfiguration.customPostPath);
        if (emitterConfiguration != null) {
            builder.sendLimit(emitterConfiguration.emitRange)
                    .option(emitterConfiguration.bufferOption)
                    .eventStore(emitterConfiguration.eventStore)
                    .byteLimitPost(emitterConfiguration.byteLimitPost)
                    .byteLimitGet(emitterConfiguration.byteLimitGet)
                    .threadPoolSize(emitterConfiguration.threadPoolSize)
                    .callback(emitterConfiguration.requestCallback);
        }
        return builder.build();
    }

    @NonNull
    private Tracker makeTracker() {
        Emitter emitter = getEmitter();
        Subject subject = getSubject();
        Tracker.TrackerBuilder builder = new Tracker.TrackerBuilder(emitter, trackerConfiguration.namespace, trackerConfiguration.appId, context)
                .subject(subject)
                .base64(trackerConfiguration.base64encoding)
                .level(trackerConfiguration.logLevel)
                .loggerDelegate(trackerConfiguration.loggerDelegate)
                .platform(trackerConfiguration.devicePlatform)
                .sessionContext(trackerConfiguration.sessionContext)
                .applicationContext(trackerConfiguration.applicationContext)
                .mobileContext(trackerConfiguration.platformContext)
                .screenContext(trackerConfiguration.screenContext)
                .screenviewEvents(trackerConfiguration.screenViewAutotracking)
                .lifecycleEvents(trackerConfiguration.lifecycleAutotracking)
                .installTracking(trackerConfiguration.installAutotracking)
                .applicationCrash(trackerConfiguration.exceptionAutotracking)
                .trackerDiagnostic(trackerConfiguration.diagnosticAutotracking);
        if (sessionConfiguration != null) {
            builder.backgroundTimeout(sessionConfiguration.backgroundTimeout.convert(TimeUnit.SECONDS));
            builder.foregroundTimeout(sessionConfiguration.foregroundTimeout.convert(TimeUnit.SECONDS));
        }
        if (globalContextsConfiguration != null) {
            // TODO: Uniform Global Context
        }
        if (gdprConfiguration != null) {
            builder.gdprContext(
                    gdprConfiguration.basisForProcessing,
                    gdprConfiguration.documentId,
                    gdprConfiguration.documentVersion,
                    gdprConfiguration.documentDescription);
        }
        return builder.build();
    }

    @NonNull
    private TrackerControllerImpl makeTrackerController() {
        return new TrackerControllerImpl(getTracker());
    }
}
