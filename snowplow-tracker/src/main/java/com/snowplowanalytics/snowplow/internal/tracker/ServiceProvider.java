package com.snowplowanalytics.snowplow.internal.tracker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.snowplowanalytics.snowplow.configuration.Configuration;
import com.snowplowanalytics.snowplow.configuration.EmitterConfiguration;
import com.snowplowanalytics.snowplow.configuration.GdprConfiguration;
import com.snowplowanalytics.snowplow.configuration.GlobalContextsConfiguration;
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration;
import com.snowplowanalytics.snowplow.configuration.SessionConfiguration;
import com.snowplowanalytics.snowplow.configuration.SubjectConfiguration;
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration;
import com.snowplowanalytics.snowplow.internal.emitter.Emitter;
import com.snowplowanalytics.snowplow.internal.emitter.EmitterControllerImpl;
import com.snowplowanalytics.snowplow.internal.emitter.NetworkControllerImpl;
import com.snowplowanalytics.snowplow.internal.gdpr.Gdpr;
import com.snowplowanalytics.snowplow.internal.gdpr.GdprControllerImpl;
import com.snowplowanalytics.snowplow.internal.globalcontexts.GlobalContextsControllerImpl;
import com.snowplowanalytics.snowplow.internal.session.SessionControllerImpl;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ServiceProvider implements ServiceProviderInterface {

    @NonNull
    private final Context context;
    @NonNull
    private final String namespace;
    @NonNull
    private String appId;

    // Internal services
    @Nullable
    private Tracker tracker;
    @Nullable
    private Emitter emitter;
    @Nullable
    private Subject subject;

    // Controllers
    @Nullable
    private TrackerControllerImpl trackerController;
    @Nullable
    private EmitterControllerImpl emitterController;
    @Nullable
    private NetworkControllerImpl networkController;
    @Nullable
    private SubjectControllerImpl subjectController;
    @Nullable
    private SessionControllerImpl sessionController;
    @Nullable
    private GdprControllerImpl gdprController;
    @Nullable
    private GlobalContextsControllerImpl globalContextsController;

    // Original configurations
    @NonNull
    private NetworkConfiguration networkConfiguration;
    @NonNull
    private TrackerConfiguration trackerConfiguration;
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

    // Configuration updates
    @NonNull
    private TrackerConfigurationUpdate trackerConfigurationUpdate;

    // Constructors

    public ServiceProvider(@NonNull Context context, @NonNull String namespace, @NonNull NetworkConfiguration networkConfiguration, @NonNull List<Configuration> configurations) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(networkConfiguration);
        Objects.requireNonNull(configurations);
        this.context = context;
        appId = context.getPackageName();
        // Reset configurationUpdates
        trackerConfigurationUpdate = new TrackerConfigurationUpdate(appId);
        // Initialization
        this.namespace = namespace;
        this.networkConfiguration = networkConfiguration;
        trackerConfiguration = new TrackerConfiguration(context.getPackageName());
        processConfigurations(configurations);
        // Apply sourceConfig
        if (trackerConfigurationUpdate.sourceConfig == null) {
            trackerConfigurationUpdate.sourceConfig = new TrackerConfiguration(appId);
        }
    }

    public void reset(@NonNull List<Configuration> configurations) {
        stopServices();
        processConfigurations(configurations);
        resetServices();
        getTracker();
    }

    public void shutdown() {
        if (tracker != null) {
            tracker.pauseEventTracking();
        }
        stopServices();
        resetServices();
        resetControllers();
        resetConfigurationUpdates();
    }

    @NonNull
    public String getNamespace() {
        return namespace;
    }

    // Private methods

    private void processConfigurations(@NonNull List<Configuration> configurations) {
        for (Configuration configuration : configurations) {
            if (configuration instanceof NetworkConfiguration) {
                networkConfiguration = (NetworkConfiguration)configuration;
                continue;
            }
            if (configuration instanceof TrackerConfiguration) {
                trackerConfigurationUpdate.sourceConfig = (TrackerConfiguration)configuration;
                continue;
            }
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

    private void stopServices() {
        if (emitter != null) {
            emitter.shutdown();
        }
    }

    private void resetServices() {
        emitter = null;
        subject = null;
        tracker = null;
    }

    private void resetControllers() {
        trackerController = null;
        sessionController = null;
        emitterController = null;
        gdprController = null;
        globalContextsController = null;
        subjectController = null;
        networkController = null;
    }

    private void resetConfigurationUpdates() {
        trackerConfigurationUpdate = new TrackerConfigurationUpdate(appId);
    }

    // Getters

    @NonNull
    public Subject getSubject() {
        if (subject == null) {
            subject = makeSubject();
        }
        return subject;
    }

    @NonNull
    public Emitter getEmitter() {
        if (emitter == null) {
            emitter = makeEmitter();
        }
        return emitter;
    }

    @NonNull
    public Tracker getTracker() {
        if (tracker == null) {
            tracker = makeTracker();
        }
        return tracker;
    }

    @NonNull
    public TrackerControllerImpl getTrackerController() {
        if (trackerController == null) {
            trackerController = makeTrackerController();
        }
        return trackerController;
    }

    @NonNull
    public SessionControllerImpl getSessionController() {
        if (sessionController == null) {
            sessionController = makeSessionController();
        }
        return sessionController;
    }

    @NonNull
    public EmitterControllerImpl getEmitterController() {
        if (emitterController == null) {
            emitterController = makeEmitterController();
        }
        return emitterController;
    }

    @NonNull
    public GdprControllerImpl getGdprController() {
        if (gdprController == null) {
            gdprController = makeGdprController();
        }
        return gdprController;
    }

    @NonNull
    public GlobalContextsControllerImpl getGlobalContextsController() {
        if (globalContextsController == null) {
            globalContextsController = makeGlobalContextsController();
        }
        return globalContextsController;
    }

    @NonNull
    public SubjectControllerImpl getSubjectController() {
        if (subjectController == null) {
            subjectController = makeSubjectController();
        }
        return subjectController;
    }

    @NonNull
    public NetworkControllerImpl getNetworkController() {
        if (networkController == null) {
            networkController = makeNetworkController();
        }
        return networkController;
    }

    @NonNull
    @Override
    public TrackerConfigurationUpdate getTrackerConfigurationUpdate() {
        return trackerConfigurationUpdate;
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
                .customPostPath(networkConfiguration.customPostPath)
                .client(networkConfiguration.okHttpClient);
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
        TrackerConfigurationInterface trackerConfig = getTrackerConfigurationUpdate();
        Tracker.TrackerBuilder builder = new Tracker.TrackerBuilder(emitter, namespace, trackerConfig.getAppId(), context)
                .subject(subject)
                .base64(trackerConfig.isBase64encoding())
                .level(trackerConfig.getLogLevel())
                .loggerDelegate(trackerConfig.getLoggerDelegate())
                .platform(trackerConfig.getDevicePlatform())
                .sessionContext(trackerConfig.isSessionContext())
                .applicationContext(trackerConfig.isApplicationContext())
                .mobileContext(trackerConfig.isPlatformContext())
                .screenContext(trackerConfig.isScreenContext())
                .screenviewEvents(trackerConfig.isScreenViewAutotracking())
                .lifecycleEvents(trackerConfig.isLifecycleAutotracking())
                .installTracking(trackerConfig.isInstallAutotracking())
                .applicationCrash(trackerConfig.isExceptionAutotracking())
                .trackerDiagnostic(trackerConfig.isDiagnosticAutotracking());
        if (sessionConfiguration != null) {
            builder.backgroundTimeout(sessionConfiguration.backgroundTimeout.convert(TimeUnit.SECONDS));
            builder.foregroundTimeout(sessionConfiguration.foregroundTimeout.convert(TimeUnit.SECONDS));
        }
        if (gdprConfiguration != null) {
            builder.gdprContext(
                    gdprConfiguration.basisForProcessing,
                    gdprConfiguration.documentId,
                    gdprConfiguration.documentVersion,
                    gdprConfiguration.documentDescription);
        }
        Tracker tracker = builder.buildAndReset();
        if (globalContextsConfiguration != null) {
            tracker.setGlobalContextGenerators(globalContextsConfiguration.contextGenerators);
        }
        if (trackerConfigurationUpdate.isPaused) {
            tracker.pauseEventTracking();
        }
        return tracker;
    }

    @NonNull
    private TrackerControllerImpl makeTrackerController() {
        return new TrackerControllerImpl(this);
    }

    @NonNull
    private SessionControllerImpl makeSessionController() {
        return new SessionControllerImpl(this);
    }

    @NonNull
    private EmitterControllerImpl makeEmitterController() {
        return new EmitterControllerImpl(this);
    }

    @NonNull
    private GdprControllerImpl makeGdprController() {
        GdprControllerImpl controller = new GdprControllerImpl(this);
        Gdpr gdpr = getTracker().getGdprContext();
        if (gdpr != null) {
            controller.reset(gdpr.basisForProcessing, gdpr.documentId, gdpr.documentVersion, gdpr.documentDescription);
        }
        return controller;
    }

    @NonNull
    private GlobalContextsControllerImpl makeGlobalContextsController() {
        return new GlobalContextsControllerImpl(this);
    }

    @NonNull
    private SubjectControllerImpl makeSubjectController() {
        return new SubjectControllerImpl(this);
    }

    @NonNull
    private NetworkControllerImpl makeNetworkController() {
        return new NetworkControllerImpl(this);
    }
}
