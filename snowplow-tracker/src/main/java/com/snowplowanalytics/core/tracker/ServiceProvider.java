package com.snowplowanalytics.core.tracker;

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
import com.snowplowanalytics.core.emitter.Emitter;
import com.snowplowanalytics.core.emitter.EmitterConfigurationInterface;
import com.snowplowanalytics.core.emitter.EmitterConfigurationUpdate;
import com.snowplowanalytics.core.emitter.EmitterControllerImpl;
import com.snowplowanalytics.core.emitter.NetworkConfigurationInterface;
import com.snowplowanalytics.core.emitter.NetworkConfigurationUpdate;
import com.snowplowanalytics.core.emitter.NetworkControllerImpl;
import com.snowplowanalytics.core.gdpr.Gdpr;
import com.snowplowanalytics.core.gdpr.GdprConfigurationUpdate;
import com.snowplowanalytics.core.gdpr.GdprControllerImpl;
import com.snowplowanalytics.core.globalcontexts.GlobalContextsControllerImpl;
import com.snowplowanalytics.core.session.Session;
import com.snowplowanalytics.core.session.SessionConfigurationInterface;
import com.snowplowanalytics.core.session.SessionConfigurationUpdate;
import com.snowplowanalytics.core.session.SessionControllerImpl;
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
        networkConfigurationUpdate.setSourceConfig(networkConfiguration);
        trackerConfiguration = new TrackerConfiguration(appId);
        processConfigurations(configurations);
        if (trackerConfigurationUpdate.sourceConfig == null) {
            trackerConfigurationUpdate.sourceConfig = new TrackerConfiguration(appId);
        }
        getOrMakeTracker(); // Build tracker to initialize NotificationCenter receivers
    }

    public void reset(@NonNull List<Configuration> configurations) {
        stopServices();
        resetConfigurationUpdates();
        processConfigurations(configurations);
        resetServices();
        getOrMakeTracker();
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
                networkConfigurationUpdate.setSourceConfig((NetworkConfiguration) configuration);
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
                sessionConfigurationUpdate.setSourceConfig((SessionConfiguration) configuration);
                continue;
            }
            if (configuration instanceof EmitterConfiguration) {
                emitterConfigurationUpdate.setSourceConfig((EmitterConfiguration) configuration);
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
        emitterConfigurationUpdate.setSourceConfig(null);
        sessionConfigurationUpdate.setSourceConfig(null);
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
    @Override
    public Subject getOrMakeSubject() {
        if (subject == null) {
            subject = makeSubject();
        }
        return subject;
    }

    @NonNull
    @Override
    public Emitter getOrMakeEmitter() {
        if (emitter == null) {
            emitter = makeEmitter();
        }
        return emitter;
    }

    @NonNull
    @Override
    public Boolean isTrackerInitialized() {
        return tracker != null;
    }

    @NonNull
    @Override
    public Tracker getOrMakeTracker() {
        if (tracker == null) {
            tracker = makeTracker();
        }
        return tracker;
    }

    @NonNull
    @Override
    public TrackerControllerImpl getOrMakeTrackerController() {
        if (trackerController == null) {
            trackerController = makeTrackerController();
        }
        return trackerController;
    }

    @NonNull
    @Override
    public SessionControllerImpl getOrMakeSessionController() {
        if (sessionController == null) {
            sessionController = makeSessionController();
        }
        return sessionController;
    }

    @NonNull
    @Override
    public EmitterControllerImpl getOrMakeEmitterController() {
        if (emitterController == null) {
            emitterController = makeEmitterController();
        }
        return emitterController;
    }

    @NonNull
    @Override
    public GdprControllerImpl getOrMakeGdprController() {
        if (gdprController == null) {
            gdprController = makeGdprController();
        }
        return gdprController;
    }

    @NonNull
    @Override
    public GlobalContextsControllerImpl getOrMakeGlobalContextsController() {
        if (globalContextsController == null) {
            globalContextsController = makeGlobalContextsController();
        }
        return globalContextsController;
    }

    @NonNull
    @Override
    public SubjectControllerImpl getOrMakeSubjectController() {
        if (subjectController == null) {
            subjectController = makeSubjectController();
        }
        return subjectController;
    }

    @NonNull
    @Override
    public NetworkControllerImpl getOrMakeNetworkController() {
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

    @NonNull
    @Override
    public EmitterConfigurationUpdate getEmitterConfigurationUpdate() {
        return emitterConfigurationUpdate;
    }

    @NonNull
    @Override
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
                .cookieJar(networkConfig.getOkHttpCookieJar())
                .sendLimit(emitterConfig.getEmitRange())
                .option(emitterConfig.getBufferOption())
                .eventStore(emitterConfig.getEventStore())
                .byteLimitPost(emitterConfig.getByteLimitPost())
                .byteLimitGet(emitterConfig.getByteLimitGet())
                .threadPoolSize(emitterConfig.getThreadPoolSize())
                .callback(emitterConfig.getRequestCallback())
                .customRetryForStatusCodes(emitterConfig.getCustomRetryForStatusCodes())
                .serverAnonymisation(emitterConfig.isServerAnonymisation());
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
        Emitter emitter = new Emitter(context, endpoint, builder);
        if (emitterConfigurationUpdate.isPaused()) {
            emitter.pauseEmit();
        }
        return emitter;
    }

    @NonNull
    private Tracker makeTracker() {
        Emitter emitter = getOrMakeEmitter();
        Subject subject = getOrMakeSubject();
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
                .foregroundTimeout(sessionConfig.getForegroundTimeout().convert(TimeUnit.SECONDS))
                .userAnonymisation(trackerConfig.isUserAnonymisation());
        GdprConfigurationUpdate gdprConfig = getGdprConfigurationUpdate();
        if (gdprConfig.sourceConfig != null) {
            builder.gdprContext(
                    gdprConfig.basisForProcessing(),
                    gdprConfig.documentId(),
                    gdprConfig.documentVersion(),
                    gdprConfig.documentDescription());
        }
        Tracker tracker = new Tracker(builder);
        if (globalContextsConfiguration != null) {
            tracker.setGlobalContextGenerators(globalContextsConfiguration.getContextGenerators());
        }
        if (trackerConfigurationUpdate.isPaused) {
            tracker.pauseEventTracking();
        }
        if (sessionConfigurationUpdate.isPaused()) {
            tracker.pauseSessionChecking();
        }
        Session session = tracker.getSession();
        if (session != null) {
            Consumer<SessionState> onSessionUpdate = sessionConfigurationUpdate.onSessionUpdate();
            if (onSessionUpdate != null) {
                session.setOnSessionUpdate(onSessionUpdate);
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
        Gdpr gdpr = getOrMakeTracker().getGdprContext();
        if (gdpr != null) {
            controller.reset(gdpr.getBasisForProcessing(), gdpr.getDocumentId(), gdpr.getDocumentVersion(), gdpr.getDocumentDescription());
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
