package com.snowplowanalytics.snowplow.internal.tracker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.util.Consumer;

import com.snowplowanalytics.snowplow.configuration.Configuration;
import com.snowplowanalytics.snowplow.configuration.EmitterConfiguration;
import com.snowplowanalytics.snowplow.configuration.GdprConfiguration;
import com.snowplowanalytics.snowplow.configuration.GlobalContextsConfiguration;
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration;
import com.snowplowanalytics.snowplow.configuration.SessionConfiguration;
import com.snowplowanalytics.snowplow.configuration.SubjectConfiguration;
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration;
import com.snowplowanalytics.snowplow.internal.emitter.Emitter;
import com.snowplowanalytics.snowplow.internal.emitter.EmitterConfigurationInterface;
import com.snowplowanalytics.snowplow.internal.emitter.EmitterConfigurationUpdate;
import com.snowplowanalytics.snowplow.internal.emitter.EmitterControllerImpl;
import com.snowplowanalytics.snowplow.internal.emitter.NetworkConfigurationInterface;
import com.snowplowanalytics.snowplow.internal.emitter.NetworkConfigurationUpdate;
import com.snowplowanalytics.snowplow.internal.emitter.NetworkControllerImpl;
import com.snowplowanalytics.snowplow.internal.gdpr.Gdpr;
import com.snowplowanalytics.snowplow.internal.gdpr.GdprConfigurationInterface;
import com.snowplowanalytics.snowplow.internal.gdpr.GdprConfigurationUpdate;
import com.snowplowanalytics.snowplow.internal.gdpr.GdprControllerImpl;
import com.snowplowanalytics.snowplow.internal.globalcontexts.GlobalContextsControllerImpl;
import com.snowplowanalytics.snowplow.internal.session.Session;
import com.snowplowanalytics.snowplow.internal.session.SessionConfigurationInterface;
import com.snowplowanalytics.snowplow.internal.session.SessionConfigurationUpdate;
import com.snowplowanalytics.snowplow.internal.session.SessionControllerImpl;
import com.snowplowanalytics.snowplow.network.HttpMethod;
import com.snowplowanalytics.snowplow.network.Protocol;
import com.snowplowanalytics.snowplow.tracker.SessionState;

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
    private final String appId;

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
    @NonNull
    private NetworkConfigurationUpdate networkConfigurationUpdate;
    @NonNull
    private SubjectConfigurationUpdate subjectConfigurationUpdate;
    @NonNull
    private EmitterConfigurationUpdate emitterConfigurationUpdate;
    @NonNull
    private SessionConfigurationUpdate sessionConfigurationUpdate;
    @NonNull
    private GdprConfigurationUpdate gdprConfigurationUpdate;

    // Constructors

    public ServiceProvider(@NonNull Context context, @NonNull String namespace, @NonNull NetworkConfiguration networkConfiguration, @NonNull List<Configuration> configurations) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(networkConfiguration);
        Objects.requireNonNull(configurations);
        // Initialization
        this.namespace = namespace;
        this.context = context;
        appId = context.getPackageName();
        // Reset configurationUpdates
        trackerConfigurationUpdate = new TrackerConfigurationUpdate(appId);
        networkConfigurationUpdate = new NetworkConfigurationUpdate();
        subjectConfigurationUpdate = new SubjectConfigurationUpdate();
        emitterConfigurationUpdate = new EmitterConfigurationUpdate();
        sessionConfigurationUpdate = new SessionConfigurationUpdate();
        gdprConfigurationUpdate = new GdprConfigurationUpdate();
        // Process configurations
        networkConfigurationUpdate.sourceConfig = networkConfiguration;
        trackerConfiguration = new TrackerConfiguration(appId);
        processConfigurations(configurations);
        if (trackerConfigurationUpdate.sourceConfig == null) {
            trackerConfigurationUpdate.sourceConfig = new TrackerConfiguration(appId);
        }
        getTracker(); // Build tracker to initialize NotificationCenter receivers
    }

    public void reset(@NonNull List<Configuration> configurations) {
        stopServices();
        resetConfigurationUpdates();
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
        initializeConfigurationUpdates();
    }

    @NonNull
    public String getNamespace() {
        return namespace;
    }

    // Private methods

    private void processConfigurations(@NonNull List<Configuration> configurations) {
        for (Configuration configuration : configurations) {
            if (configuration instanceof NetworkConfiguration) {
                networkConfigurationUpdate.sourceConfig = (NetworkConfiguration)configuration;
                continue;
            }
            if (configuration instanceof TrackerConfiguration) {
                trackerConfigurationUpdate.sourceConfig = (TrackerConfiguration)configuration;
                continue;
            }
            if (configuration instanceof SubjectConfiguration) {
                subjectConfigurationUpdate.sourceConfig = (SubjectConfiguration)configuration;
                continue;
            }
            if (configuration instanceof SessionConfiguration) {
                sessionConfigurationUpdate.sourceConfig = (SessionConfiguration)configuration;
                continue;
            }
            if (configuration instanceof EmitterConfiguration) {
                emitterConfigurationUpdate.sourceConfig = (EmitterConfiguration)configuration;
                continue;
            }
            if (configuration instanceof GdprConfiguration) {
                gdprConfigurationUpdate.sourceConfig = (GdprConfiguration)configuration;
                continue;
            }
            if (configuration instanceof GlobalContextsConfiguration) {
                globalContextsConfiguration = (GlobalContextsConfiguration)configuration;
                continue;
            }
        }
    }

    private void stopServices() {
        if (tracker != null) {
            tracker.close();
        }
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
        // Don't reset networkConfiguration as it's needed in case it's not passed in the new configurations.
        // Set a default trackerConfiguration to reset to default if not passed.
        trackerConfigurationUpdate.sourceConfig = new TrackerConfiguration(appId);
        subjectConfigurationUpdate.sourceConfig = null;
        emitterConfigurationUpdate.sourceConfig = null;
        sessionConfigurationUpdate.sourceConfig = null;
        gdprConfigurationUpdate.sourceConfig = null;
    }

    private void initializeConfigurationUpdates() {
        networkConfigurationUpdate = new NetworkConfigurationUpdate();
        trackerConfigurationUpdate = new TrackerConfigurationUpdate(appId);
        emitterConfigurationUpdate = new EmitterConfigurationUpdate();
        subjectConfigurationUpdate = new SubjectConfigurationUpdate();
        sessionConfigurationUpdate = new SessionConfigurationUpdate();
        gdprConfigurationUpdate = new GdprConfigurationUpdate();
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

    @NonNull
    @Override
    public NetworkConfigurationUpdate getNetworkConfigurationUpdate() {
        return networkConfigurationUpdate;
    }

    @NonNull
    @Override
    public SubjectConfigurationUpdate getSubjectConfigurationUpdate() {
        return subjectConfigurationUpdate;
    }

    @Override
    @NonNull
    public EmitterConfigurationUpdate getEmitterConfigurationUpdate() {
        return emitterConfigurationUpdate;
    }

    @Override
    @NonNull
    public SessionConfigurationUpdate getSessionConfigurationUpdate() {
        return sessionConfigurationUpdate;
    }

    @NonNull
    @Override
    public GdprConfigurationUpdate getGdprConfigurationUpdate() {
        return gdprConfigurationUpdate;
    }

    // Factories

    @NonNull
    private Subject makeSubject() {
        return new Subject(context, subjectConfigurationUpdate);
    }

    @NonNull
    private Emitter makeEmitter() {
        NetworkConfigurationInterface networkConfig = networkConfigurationUpdate;
        EmitterConfigurationInterface emitterConfig = emitterConfigurationUpdate;
        Emitter.EmitterBuilder builder = new Emitter.EmitterBuilder()
                .networkConnection(networkConfig.getNetworkConnection())
                .customPostPath(networkConfig.getCustomPostPath())
                .client(networkConfig.getOkHttpClient())
                .sendLimit(emitterConfig.getEmitRange())
                .option(emitterConfig.getBufferOption())
                .eventStore(emitterConfig.getEventStore())
                .byteLimitPost(emitterConfig.getByteLimitPost())
                .byteLimitGet(emitterConfig.getByteLimitGet())
                .threadPoolSize(emitterConfig.getThreadPoolSize())
                .callback(emitterConfig.getRequestCallback());
        HttpMethod method = networkConfig.getMethod();
        if (method != null) {
            builder.method(method);
        }
        Protocol protocol = networkConfig.getProtocol();
        if (protocol != null) {
            builder.security(protocol);
        }
        String endpoint = networkConfig.getEndpoint();
        if (endpoint == null) {
            endpoint = "";
        }
        return new Emitter(context, endpoint, builder);
    }

    @NonNull
    private Tracker makeTracker() {
        Emitter emitter = getEmitter();
        Subject subject = getSubject();
        TrackerConfigurationInterface trackerConfig = getTrackerConfigurationUpdate();
        SessionConfigurationInterface sessionConfig = getSessionConfigurationUpdate();
        Tracker.TrackerBuilder builder = new Tracker.TrackerBuilder(emitter, namespace, trackerConfig.getAppId(), context)
                .subject(subject)
                .trackerVersionSuffix(trackerConfig.getTrackerVersionSuffix())
                .base64(trackerConfig.isBase64encoding())
                .level(trackerConfig.getLogLevel())
                .loggerDelegate(trackerConfig.getLoggerDelegate())
                .platform(trackerConfig.getDevicePlatform())
                .sessionContext(trackerConfig.isSessionContext())
                .applicationContext(trackerConfig.isApplicationContext())
                .mobileContext(trackerConfig.isPlatformContext())
                .deepLinkContext(trackerConfig.isDeepLinkContext())
                .screenContext(trackerConfig.isScreenContext())
                .screenviewEvents(trackerConfig.isScreenViewAutotracking())
                .lifecycleEvents(trackerConfig.isLifecycleAutotracking())
                .installTracking(trackerConfig.isInstallAutotracking())
                .applicationCrash(trackerConfig.isExceptionAutotracking())
                .trackerDiagnostic(trackerConfig.isDiagnosticAutotracking())
                .backgroundTimeout(sessionConfig.getBackgroundTimeout().convert(TimeUnit.SECONDS))
                .foregroundTimeout(sessionConfig.getForegroundTimeout().convert(TimeUnit.SECONDS));
        GdprConfigurationUpdate gdprConfig = getGdprConfigurationUpdate();
        if (gdprConfig.sourceConfig != null) {
            builder.gdprContext(
                    gdprConfig.getBasisForProcessing(),
                    gdprConfig.getDocumentId(),
                    gdprConfig.getDocumentVersion(),
                    gdprConfig.getDocumentDescription());
        }
        Tracker tracker = new Tracker(builder);
        if (globalContextsConfiguration != null) {
            tracker.setGlobalContextGenerators(globalContextsConfiguration.contextGenerators);
        }
        if (trackerConfigurationUpdate.isPaused) {
            tracker.pauseEventTracking();
        }
        if (sessionConfigurationUpdate.isPaused) {
            tracker.pauseSessionChecking();
        }
        if (emitterConfigurationUpdate.isPaused) {
            emitter.pauseEmit();
        }
        Session session = tracker.getSession();
        if (session != null) {
            Consumer<SessionState> onSessionUpdate = sessionConfigurationUpdate.getOnSessionUpdate();
            if (onSessionUpdate != null) {
                session.onSessionUpdate = onSessionUpdate;
            }
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
