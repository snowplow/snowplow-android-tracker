/*
 * Copyright (c) 2015-2022 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */

package com.snowplowanalytics.snowplow.internal.tracker;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.snowplowanalytics.snowplow.entity.DeepLink;
import com.snowplowanalytics.snowplow.event.Background;
import com.snowplowanalytics.snowplow.event.DeepLinkReceived;
import com.snowplowanalytics.snowplow.event.Foreground;
import com.snowplowanalytics.snowplow.internal.utils.NotificationCenter;
import com.snowplowanalytics.snowplow.tracker.BuildConfig;
import com.snowplowanalytics.snowplow.tracker.DevicePlatform;
import com.snowplowanalytics.snowplow.internal.emitter.Emitter;
import com.snowplowanalytics.snowplow.internal.emitter.Executor;
import com.snowplowanalytics.snowplow.internal.gdpr.Gdpr;
import com.snowplowanalytics.snowplow.tracker.InspectableEvent;
import com.snowplowanalytics.snowplow.tracker.LoggerDelegate;
import com.snowplowanalytics.snowplow.internal.session.Session;
import com.snowplowanalytics.snowplow.internal.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.globalcontexts.GlobalContext;
import com.snowplowanalytics.snowplow.event.Event;
import com.snowplowanalytics.snowplow.event.TrackerError;
import com.snowplowanalytics.snowplow.payload.Payload;
import com.snowplowanalytics.snowplow.payload.TrackerPayload;
import com.snowplowanalytics.snowplow.internal.session.ProcessObserver;
import com.snowplowanalytics.snowplow.tracker.LogLevel;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.internal.utils.Util;
import com.snowplowanalytics.snowplow.util.Basis;


/**
 * Builds a Tracker object which is used to send events to a Snowplow Collector.
 */
public class Tracker {

    /**
     * Builder for the Tracker
     */
    public static class TrackerBuilder {

        final @NonNull Emitter emitter; // Required
        final @NonNull String namespace; // Required
        final @NonNull String appId; // Required
        final @NonNull Context context; // Required
        @Nullable Subject subject = null; // Optional
        boolean base64Encoded = true; // Optional
        @Nullable
        DevicePlatform devicePlatform = DevicePlatform.Mobile; // Optional
        LogLevel logLevel = LogLevel.OFF; // Optional
        boolean sessionContext = false; // Optional
        long foregroundTimeout = 1800; // Optional - 30 minutes
        long backgroundTimeout = 1800; // Optional - 30 minutes
        @NonNull Runnable[] sessionCallbacks = new Runnable[]{}; // Optional
        int threadCount = 10; // Optional
        TimeUnit timeUnit = TimeUnit.SECONDS; // Optional
        boolean geoLocationContext = false; // Optional
        boolean mobileContext = false; // Optional
        boolean applicationCrash = true; // Optional
        boolean trackerDiagnostic = false; // Optional
        boolean lifecycleEvents = false; // Optional
        boolean deepLinkContext = true; // Optional
        boolean screenContext = false; // Optional
        boolean activityTracking = false; // Optional
        boolean installTracking = false; // Optional
        boolean applicationContext = false; // Optional
        boolean userAnonymisation = false; // Optional
        @Nullable Gdpr gdpr = null; // Optional
        @Nullable String trackerVersionSuffix = null; // Optional

        /**
         * @param emitter Emitter to which events will be sent
         * @param namespace Identifier for the Tracker instance
         * @param appId Application ID
         * @param context The Android application context
         */
        public TrackerBuilder(@NonNull Emitter emitter, @NonNull String namespace, @NonNull String appId, @NonNull Context context) {
            this.emitter = emitter;
            this.namespace = namespace;
            this.appId = appId;
            this.context = context;
        }

        /**
         * @param isEnabled Whether application contexts are sent with all events
         * @return itself
         */
        @NonNull
        public TrackerBuilder applicationContext(boolean isEnabled) {
            this.applicationContext = isEnabled;
            return this;
        }

        /**
         * Enables GDPR context to be sent with every event.
         * @param basisForProcessing GDPR Basis for processing
         * @param documentId ID of a GDPR basis document
         * @param documentVersion Version of the document
         * @param documentDescription Description of the document
         * @return itself
         */
        @NonNull
        public TrackerBuilder gdprContext(@NonNull Basis basisForProcessing, @Nullable String documentId, @Nullable String documentVersion, @Nullable String documentDescription) {
            this.gdpr = new Gdpr(basisForProcessing, documentId, documentVersion, documentDescription);
            return this;
        }

        /**
         * @param willTrack Whether install events will be tracked
         * @return itself
         */
        @NonNull
        public TrackerBuilder installTracking(boolean willTrack) {
            this.installTracking = willTrack;
            return this;
        }

        /**
         * @param subject Subject to be tracked
         * @return itself
         */
        @NonNull
        public TrackerBuilder subject(@Nullable Subject subject) {
            this.subject = subject;
            return this;
        }

        /**
         * @param base64 Whether JSONs in the payload should be base-64 encoded
         * @return itself
         */
        @NonNull
        public TrackerBuilder base64(@Nullable Boolean base64) {
            this.base64Encoded = base64;
            return this;
        }

        /**
         * @param platform The device platform the tracker is running on
         * @return itself
         */
        @NonNull
        public TrackerBuilder platform(@Nullable DevicePlatform platform) {
            this.devicePlatform = platform;
            return this;
        }

        /**
         * @param log The log level for the Tracker class
         * @return itself
         */
        @NonNull
        public TrackerBuilder level(@Nullable LogLevel log) {
            this.logLevel = log;
            return this;
        }

        /**
         * @param delegate The logger delegate that receive logs from the tracker.
         * @return itself
         */
        @NonNull
        public TrackerBuilder loggerDelegate(@Nullable LoggerDelegate delegate) {
            Logger.setDelegate(delegate);
            return this;
        }

        /**
         * @param sessionContext whether to add a session context
         * @return itself
         */
        @NonNull
        public TrackerBuilder sessionContext(boolean sessionContext) {
            this.sessionContext = sessionContext;
            return this;
        }

        /**
         * @param timeout The session foreground timeout
         * @return itself
         */
        @NonNull
        public TrackerBuilder foregroundTimeout(long timeout) {
            this.foregroundTimeout = timeout;
            return this;
        }

        /**
         * @param timeout The session background timeout
         * @return itself
         */
        @NonNull
        public TrackerBuilder backgroundTimeout(long timeout) {
            this.backgroundTimeout = timeout;
            return this;
        }

        /**
         * @param foregroundTransitionCallback Called when session transitions to foreground
         * @param backgroundTransitionCallback Called when session transitions to background
         * @param foregroundTimeoutCallback Called when foregrounded session times-out
         * @param backgroundTimeoutCallback Called when backgrounded session times-out
         * @return itself
         */
        @NonNull
        public TrackerBuilder sessionCallbacks(@NonNull Runnable foregroundTransitionCallback,
                                               @NonNull Runnable backgroundTransitionCallback,
                                               @NonNull Runnable foregroundTimeoutCallback,
                                               @NonNull Runnable backgroundTimeoutCallback)
        {
            this.sessionCallbacks = new Runnable[]{
                    foregroundTransitionCallback, backgroundTransitionCallback,
                    foregroundTimeoutCallback, backgroundTimeoutCallback
            };
            return this;
        }

        /**
         * @param threadCount the amount of threads to use for concurrency
         * @return itself
         */
        @NonNull
        public TrackerBuilder threadCount(int threadCount) {
            this.threadCount = threadCount;
            return this;
        }

        /**
         * @param timeUnit a valid TimeUnit
         * @return itself
         */
        @NonNull
        public TrackerBuilder timeUnit(@Nullable TimeUnit timeUnit) {
            this.timeUnit = timeUnit;
            return this;
        }

        /**
         * @param geoLocationContext whether to add a geo-location context
         * @return itself
         */
        @NonNull
        public TrackerBuilder geoLocationContext(@NonNull Boolean geoLocationContext) {
            this.geoLocationContext = geoLocationContext;
            return this;
        }

        /**
         * @param mobileContext whether to add a mobile context
         * @return itself
         */
        @NonNull
        public TrackerBuilder mobileContext(@NonNull Boolean mobileContext) {
            this.mobileContext = mobileContext;
            return this;
        }

        /**
         * @param applicationCrash whether to automatically track application crashes
         * @return itself
         */
        @NonNull
        public TrackerBuilder applicationCrash(@NonNull Boolean applicationCrash) {
            this.applicationCrash = applicationCrash;
            return this;
        }

        /**
         * @param trackerDiagnostic whether to automatically track error within the tracker.
         * @return itself
         */
        @NonNull
        public TrackerBuilder trackerDiagnostic(@NonNull Boolean trackerDiagnostic) {
            this.trackerDiagnostic = trackerDiagnostic;
            return this;
        }

        /**
         * @param lifecycleEvents whether to automatically track transition
         *                        from foreground to background
         * @return itself
         */
        @NonNull
        public TrackerBuilder lifecycleEvents(@NonNull Boolean lifecycleEvents) {
            this.lifecycleEvents = lifecycleEvents;
            return this;
        }

        @NonNull
        public TrackerBuilder deepLinkContext(@NonNull Boolean deepLinkContext) {
            this.deepLinkContext = deepLinkContext;
            return this;
        }

        /**
         * @param screenContext whether to send a screen context (info pertaining
         *                      to current screen) with every event
         * @return itself
         */
        @NonNull
        public synchronized TrackerBuilder screenContext(@NonNull Boolean screenContext) {
            this.screenContext = screenContext;
            return this;
        }

        /**
         * @param screenviewEvents whether to auto-track screenviews
         * @return itself
         */
        @NonNull
        public TrackerBuilder screenviewEvents(@NonNull Boolean screenviewEvents) {
            this.activityTracking = screenviewEvents;
            return this;
        }

        /**
         * @param userAnonymisation whether to anonymise client-side user identifiers in session (userId, previousSessionId), subject (userId, networkUserId, domainUserId, ipAddress) and platform context entities (IDFA)
         * @return itself
         */
        @NonNull
        public TrackerBuilder userAnonymisation(@NonNull Boolean userAnonymisation) {
            boolean changedUserAnonymisation = this.userAnonymisation != userAnonymisation;
            this.userAnonymisation = userAnonymisation;
            return this;
        }

        /**
         * Internal use only.
         * Decorate the `tv` (tracker version) field in the tracker protocol.
         */
        @NonNull
        public TrackerBuilder trackerVersionSuffix(@Nullable String trackerVersionSuffix) {
            this.trackerVersionSuffix = trackerVersionSuffix;
            return this;
        }
    }

    // ----

    private final static String TAG = Tracker.class.getSimpleName();
    private String trackerVersion = BuildConfig.TRACKER_LABEL;

    // --- Builder

    final Context context;
    Emitter emitter;
    Subject subject;
    Session trackerSession;
    String namespace;
    String appId;
    boolean base64Encoded;
    DevicePlatform devicePlatform;
    LogLevel level;
    private boolean sessionContext;
    Runnable[] sessionCallbacks;
    int threadCount;
    boolean geoLocationContext;
    boolean mobileContext;
    boolean applicationCrash;
    boolean trackerDiagnostic;
    boolean lifecycleEvents;
    boolean installTracking;
    boolean activityTracking;
    boolean applicationContext;
    private boolean userAnonymisation;
    String trackerVersionSuffix;

    private boolean deepLinkContext;
    private boolean screenContext;

    private Gdpr gdpr;
    private final StateManager stateManager;

    private final TimeUnit timeUnit;
    private final long foregroundTimeout;
    private final long backgroundTimeout;

    @NonNull
    private final PlatformContext platformContext;

    private final Map<String, GlobalContext> globalContextGenerators = Collections.synchronizedMap(new HashMap<>());

    private final NotificationCenter.FunctionalObserver receiveLifecycleNotification = new NotificationCenter.FunctionalObserver() {
        @Override
        public void apply(@NonNull Map<String, Object> data) {
            Session session = getSession();
            if (session == null || !lifecycleEvents) {
                return;
            }
            Boolean isForeground = (Boolean) data.get("isForeground");
            if (isForeground == null) {
                return;
            }
            if (session.isBackground() == !isForeground) {
                // if the new lifecycle state confirms the session state, there isn't any lifecycle transition
                return;
            }
            if (isForeground) {
                track(new Foreground().foregroundIndex(session.getForegroundIndex() + 1));
            } else {
                track(new Background().backgroundIndex(session.getBackgroundIndex() + 1));
            }
            session.setBackground(!isForeground);
        }
    };
    private final NotificationCenter.FunctionalObserver receiveScreenViewNotification = new NotificationCenter.FunctionalObserver() {
        @Override
        public void apply(@NonNull Map<String, Object> data) {
            if (activityTracking) {
                Event event = (Event) data.get("event");
                if (event != null) {
                    track(event);
                }
            }
        }
    };
    private final NotificationCenter.FunctionalObserver receiveInstallNotification = new NotificationCenter.FunctionalObserver() {
        @Override
        public void apply(@NonNull Map<String, Object> data) {
            if (installTracking) {
                Event event = (Event) data.get("event");
                if (event != null) {
                    track(event);
                }
            }
        }
    };
    private final NotificationCenter.FunctionalObserver receiveDiagnosticNotification = new NotificationCenter.FunctionalObserver() {
        @Override
        public void apply(@NonNull Map<String, Object> data) {
            if (trackerDiagnostic) {
                Event event = (Event) data.get("event");
                if (event != null) {
                    track(event);
                }
            }
        }
    };
    private final NotificationCenter.FunctionalObserver receiveCrashReportingNotification = new NotificationCenter.FunctionalObserver() {
        @Override
        public void apply(@NonNull Map<String, Object> data) {
            if (applicationCrash) {
                Event event = (Event) data.get("event");
                if (event != null) {
                    track(event);
                }
            }
        }
    };

    AtomicBoolean dataCollection = new AtomicBoolean(true);

    /**
     * Creates a new Snowplow Tracker.
     * @param builder The builder that constructs a tracker
     */
    public Tracker(@NonNull TrackerBuilder builder) {
        this.stateManager = new StateManager();
        this.context = builder.context;

        this.emitter = builder.emitter;
        this.emitter.flush();

        this.namespace = builder.namespace;
        this.emitter.setNamespace(namespace);

        this.appId = builder.appId;
        this.base64Encoded = builder.base64Encoded;

        this.subject = builder.subject;
        this.devicePlatform = builder.devicePlatform;
        this.sessionContext = builder.sessionContext;
        this.sessionCallbacks = builder.sessionCallbacks;
        this.threadCount = Math.max(builder.threadCount, 2);
        this.geoLocationContext = builder.geoLocationContext;
        this.mobileContext = builder.mobileContext;
        this.applicationCrash = builder.applicationCrash;
        this.trackerDiagnostic = builder.trackerDiagnostic;
        this.lifecycleEvents = builder.lifecycleEvents;
        this.activityTracking = builder.activityTracking;
        this.installTracking = builder.installTracking;
        this.applicationContext = builder.applicationContext;
        this.gdpr = builder.gdpr;
        this.level = builder.logLevel;
        this.trackerVersionSuffix = builder.trackerVersionSuffix;
        this.timeUnit = builder.timeUnit;
        this.foregroundTimeout = builder.foregroundTimeout;
        this.backgroundTimeout = builder.backgroundTimeout;
        this.userAnonymisation = builder.userAnonymisation;

        this.platformContext = new PlatformContext(this.context);

        setScreenContext(builder.screenContext);
        setDeepLinkContext(builder.deepLinkContext);

        if (trackerVersionSuffix != null) {
            String suffix = trackerVersionSuffix.replaceAll("[^A-Za-z0-9.-]", "");
            if (!suffix.isEmpty()) {
                trackerVersion = trackerVersion + " " + suffix;
            }
        }

        if (trackerDiagnostic && (level == LogLevel.OFF)) {
            level = LogLevel.ERROR;
        }

        Logger.updateLogLevel(level);

        // When session context is enabled
        if (this.sessionContext) {
            Runnable[] callbacks = {null, null, null, null};
            if (sessionCallbacks.length == 4) {
                callbacks = sessionCallbacks;
            }
            trackerSession = Session.getInstance(context, foregroundTimeout, backgroundTimeout, timeUnit, namespace, callbacks);
        }

        // Register notification receivers from singleton services
        registerNotificationHandlers();

        // Initialization of services shared with all the tracker instances
        initializeCrashReporting();
        initializeInstallTracking();
        initializeScreenviewTracking();
        initializeLifecycleTracking();

        // Resume session
        resumeSessionChecking();

        Logger.v(TAG, "Tracker created successfully.");
    }

    // --- Private init functions

    private void registerNotificationHandlers() {
        NotificationCenter.addObserver("SnowplowTrackerDiagnostic", receiveDiagnosticNotification);
        NotificationCenter.addObserver("SnowplowScreenView", receiveScreenViewNotification);
        NotificationCenter.addObserver("SnowplowLifecycleTracking", receiveLifecycleNotification);
        NotificationCenter.addObserver("SnowplowInstallTracking", receiveInstallNotification);
        NotificationCenter.addObserver("SnowplowCrashReporting", receiveCrashReportingNotification);
    }

    private void unregisterNotificationHandlers() {
        NotificationCenter.removeObserver(receiveDiagnosticNotification);
        NotificationCenter.removeObserver(receiveScreenViewNotification);
        NotificationCenter.removeObserver(receiveLifecycleNotification);
        NotificationCenter.removeObserver(receiveInstallNotification);
        NotificationCenter.removeObserver(receiveCrashReportingNotification);
    }

    private void initializeInstallTracking() {
        if (installTracking) {
            InstallTracker.getInstance(context);
        }
    }

    private void initializeScreenviewTracking() {
        if (activityTracking) {
            ActivityLifecycleHandler.getInstance(context);
        }
    }

    private void initializeLifecycleTracking() {
        if (lifecycleEvents) {
            ProcessObserver.initialize(context);
            // Initialize LifecycleStateMachine for lifecycle entities
            stateManager.addOrReplaceStateMachine(new LifecycleStateMachine(), "Lifecycle");
        }
    }

    private void initializeCrashReporting() {
        if (applicationCrash && !(Thread.getDefaultUncaughtExceptionHandler() instanceof ExceptionHandler)) {
            Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
        }
    }

    public void close() {
        unregisterNotificationHandlers();
        pauseSessionChecking();
        getEmitter().shutdown();
    }

    // --- Event Tracking Functions

    /**
     * Handles tracking the different types of events that
     * the Tracker can encounter.
     *
     * @param event the event to track
     * @return The event ID or null in case tracking is paused
     */
    public UUID track(final @NonNull Event event) {
        if (!dataCollection.get()) {
            return null;
        }
        event.beginProcessing(this);
        TrackerStateSnapshot stateSnapshot;
        TrackerEvent trackerEvent;
        synchronized (this) {
            stateSnapshot = stateManager.trackerStateForProcessedEvent(event);
            trackerEvent = new TrackerEvent(event, stateSnapshot);
            workaroundForIncoherentSessionContext(trackerEvent);
        }
        boolean reportsOnDiagnostic = !(event instanceof TrackerError);
        Executor.execute(reportsOnDiagnostic, TAG, () -> {
            transformEvent(trackerEvent);
            Payload payload = payloadWithEvent(trackerEvent);
            Logger.v(TAG, "Adding new payload to event storage: %s", payload);
            this.emitter.add(payload);
            event.endProcessing(this);
        });
        return trackerEvent.eventId;
    }

    private void transformEvent(@NonNull TrackerEvent event) {
        // Application_install event needs the timestamp to the real installation event.
        if (event.schema != null
                && event.schema.equals(TrackerConstants.SCHEMA_APPLICATION_INSTALL)
                && event.trueTimestamp != null)
        {
            event.timestamp = event.trueTimestamp;
            event.trueTimestamp = null;
        }
        // Payload can be optionally updated with values based on internal state
        stateManager.addPayloadValuesToEvent(event);
    }

    private @NonNull Payload payloadWithEvent(@NonNull TrackerEvent event) {
        TrackerPayload payload = new TrackerPayload();
        addBasicPropertiesToPayload(payload, event);
        if (event.isPrimitive) {
            addPrimitivePropertiesToPayload(payload, event);
        } else {
            addSelfDescribingPropertiesToPayload(payload, event);
        }
        List<SelfDescribingJson> contexts = event.contexts;
        addBasicContextsToContexts(contexts, event);
        addGlobalContextsToContexts(contexts, event);
        addStateMachineEntitiesToContexts(contexts, event);
        wrapContextsToPayload(payload, contexts);
        if (!event.isPrimitive) {
            // TODO: To remove when Atomic table refactoring is finished
            workaroundForCampaignAttributionEnrichment(payload, event, contexts);
        }
        return payload;
    }

    private void addBasicPropertiesToPayload(@NonNull Payload payload, @NonNull TrackerEvent event) {
        payload.add(Parameters.EID, event.eventId.toString());
        payload.add(Parameters.DEVICE_TIMESTAMP, Long.toString(event.timestamp));
        if (event.trueTimestamp != null) {
            payload.add(Parameters.TRUE_TIMESTAMP, event.trueTimestamp.toString());
        }
        payload.add(Parameters.APPID, this.appId);
        payload.add(Parameters.NAMESPACE, this.namespace);
        payload.add(Parameters.TRACKER_VERSION, this.trackerVersion);
        if (this.subject != null) {
            payload.addMap(new HashMap<>(this.subject.getSubject(userAnonymisation)));
        }
        payload.add(Parameters.PLATFORM, this.devicePlatform.getValue());
    }

    private void addPrimitivePropertiesToPayload(@NonNull Payload payload, @NonNull TrackerEvent event) {
        payload.add(Parameters.EVENT, event.eventName);
        payload.addMap(event.payload);
    }

    private void addSelfDescribingPropertiesToPayload(@NonNull Payload payload, @NonNull TrackerEvent event) {
        payload.add(Parameters.EVENT, TrackerConstants.EVENT_UNSTRUCTURED);

        SelfDescribingJson data = new SelfDescribingJson(event.schema, event.payload);
        HashMap<String, Object> unstructuredEventPayload = new HashMap<>();
        unstructuredEventPayload.put(Parameters.SCHEMA, TrackerConstants.SCHEMA_UNSTRUCT_EVENT);
        unstructuredEventPayload.put(Parameters.DATA, data.getMap());
        payload.addMap(unstructuredEventPayload, base64Encoded, Parameters.UNSTRUCTURED_ENCODED, Parameters.UNSTRUCTURED);
    }

    /*
     This is needed because the campaign-attribution-enrichment (in the pipeline) is able to parse
     the `url` and `referrer` only if they are part of a PageView event.
     The PageView event is an atomic event but the DeepLinkReceived and ScreenView are SelfDescribing events.
     For this reason we copy these two fields in the atomic fields in order to let the enrichment
     to process correctly the fields even if the event is not a PageView and it's a SelfDescribing event.
     This is a hack that should be removed once the atomic event table is dismissed and all the events
     will be SelfDescribing.
    */
    private void workaroundForCampaignAttributionEnrichment(@NonNull Payload payload, @NonNull TrackerEvent event, List<SelfDescribingJson> contexts) {
        String url = null;
        String referrer = null;

        if (event.schema.equals(DeepLinkReceived.SCHEMA) && event.payload != null) {
            url = (String)event.payload.get(DeepLinkReceived.PARAM_URL);
            referrer = (String)event.payload.get(DeepLinkReceived.PARAM_REFERRER);
        }
        else if (event.schema.equals(TrackerConstants.SCHEMA_SCREEN_VIEW) && contexts != null) {
            for (SelfDescribingJson entity : contexts) {
                if (entity instanceof DeepLink) {
                    DeepLink deepLink = (DeepLink) entity;
                    url = deepLink.getUrl();
                    referrer = deepLink.getReferrer();
                    break;
                }
            }
        }

        if (url != null) { payload.add(Parameters.PAGE_URL, url); }
        if (referrer != null) { payload.add(Parameters.PAGE_REFR, referrer); }
    }

    /*
     The session context should be computed as part of `addBasicContextsToContexts` but that method
     is executed in a separate thread. At the moment, the Session management is performed by the legacy
     solution that doesn't make use of the tracker state snapshot (taken as soon as the event is tracked)
     needed to keep a coherent state between events tracked in sequence but processed in parallel.
     Due to this parallel processing the session context could be incoherent with the event sequence.
     This limit is a serious problem when we process lifecycle events because the `application_background`
     event should be checked with the session foreground timeout and the `application_foreground` event
     should be checked with the session background timeout. Without a coherent session management it's
     impossible to grant this behaviour causing serious issues on the calculation of the session expiring.
     */
    private void workaroundForIncoherentSessionContext(@NonNull TrackerEvent event) {
        if (!event.isService && sessionContext) {
            String eventId = event.eventId.toString();
            long eventTimestamp = event.timestamp;
            Session sessionManager = trackerSession;
            if (sessionManager == null) {
                Logger.track(TAG, "Session not ready or method getHasLoadedFromFile returned false with eventId: %s", eventId);
                return;
            }
            SelfDescribingJson sessionContextJson = sessionManager.getSessionContext(eventId, eventTimestamp, userAnonymisation);
            event.contexts.add(sessionContextJson);
        }
    }

    private void addBasicContextsToContexts(@NonNull List<SelfDescribingJson> contexts, @NonNull TrackerEvent event) {
        if (applicationContext) {
            contexts.add(Util.getApplicationContext(this.context));
        }

        if (mobileContext) {
            contexts.add(platformContext.getMobileContext(userAnonymisation));
        }

        if (event.isService) {
            return;
        }

        if (geoLocationContext) {
            contexts.add(Util.getGeoLocationContext(this.context));
        }

        if (gdpr != null) {
            contexts.add(gdpr.getContext());
        }
    }

    private void addGlobalContextsToContexts(@NonNull List<SelfDescribingJson> contexts, @NonNull InspectableEvent event) {
        synchronized (globalContextGenerators) {
            for (GlobalContext generator : globalContextGenerators.values()) {
                contexts.addAll(generator.generateContexts(event));
            }
        }
    }

    private void addStateMachineEntitiesToContexts(@NonNull List<SelfDescribingJson> contexts, @NonNull InspectableEvent event) {
        List<SelfDescribingJson> stateManagerEntities = stateManager.entitiesForProcessedEvent(event);
        contexts.addAll(stateManagerEntities);
    }

    private void wrapContextsToPayload(@NonNull Payload payload, @NonNull List<SelfDescribingJson> contexts) {
        if (contexts.isEmpty()) {
            return;
        }
        List<Map<String, Object>> data = new LinkedList<>();
        for (SelfDescribingJson context : contexts) {
            if (context != null) {
                data.add(context.getMap());
            }
        }
        SelfDescribingJson finalContext = new SelfDescribingJson(TrackerConstants.SCHEMA_CONTEXTS, data);
        payload.addMap(finalContext.getMap(), base64Encoded, Parameters.CONTEXT_ENCODED, Parameters.CONTEXT);
    }

    // --- Helpers

    /**
     * Builds and adds a finalized payload of a service event
     * by adding in extra information to the payload:
     * - The event contexts (limited to identify the device and app)
     * - The Tracker Subject
     * - The Tracker parameters
     *
     * @param payload Payload the raw event payload to be
     *                decorated.
     * @param contexts The raw context list
     */
    private void addServiceEventPayload(@NonNull Payload payload, @NonNull List<SelfDescribingJson> contexts) {
        // Add default parameters to the payload
        payload.add(Parameters.PLATFORM, this.devicePlatform.getValue());
        payload.add(Parameters.APPID, this.appId);
        payload.add(Parameters.NAMESPACE, this.namespace);
        payload.add(Parameters.TRACKER_VERSION, this.trackerVersion);

        // If there is a subject present for the Tracker add it
        if (this.subject != null) {
            payload.addMap(new HashMap<>(this.subject.getSubject(userAnonymisation)));
        }

        // Add Mobile Context
        if (this.mobileContext) {
            contexts.add(platformContext.getMobileContext(userAnonymisation));
        }

        // Add application context
        if (this.applicationContext) {
            contexts.add(Util.getApplicationContext(this.context));
        }

        // If there are contexts to nest
        if (contexts.size() > 0) {
            List<Map> contextMaps = new LinkedList<>();
            for (SelfDescribingJson selfDescribingJson : contexts) {
                if (selfDescribingJson != null) {
                    contextMaps.add(selfDescribingJson.getMap());
                }
            }
            SelfDescribingJson envelope = new SelfDescribingJson(TrackerConstants.SCHEMA_CONTEXTS, contextMaps);
            payload.addMap(envelope.getMap(), this.base64Encoded, Parameters.CONTEXT_ENCODED,
                    Parameters.CONTEXT);
        }

        // Add this payload to the emitter
        this.emitter.add(payload);
    }

    // --- Controls

    /**
     * Starts event collection processes
     * again.
     */
    public void resumeEventTracking() {
        if (dataCollection.compareAndSet(false, true)) {
            resumeSessionChecking();
            getEmitter().flush();
        }
    }

    /**
     * Stops event collection and ends all
     * concurrent processes.
     */
    public void pauseEventTracking() {
        if (dataCollection.compareAndSet(true, false)) {
            pauseSessionChecking();
            getEmitter().shutdown();
        }
    }

    /**
     * Starts session checking.
     */
    public void resumeSessionChecking() {
        Session trackerSession = this.trackerSession;
        if (trackerSession != null) {
            trackerSession.setIsSuspended(false);
            Logger.d(TAG, "Session checking has been resumed.");
        }
    }

    /**
     * Ends session checking.
     */
    public void pauseSessionChecking() {
        Session trackerSession = this.trackerSession;
        if (trackerSession != null) {
            trackerSession.setIsSuspended(true);
            Logger.d(TAG, "Session checking has been paused.");
        }
    }

    /**
     * Convenience function for starting a new session.
     */
    public void startNewSession() {
        trackerSession.startNewSession();
    }

    // --- GDPR context

    /**
     * Enables GDPR context to be sent with every event.
     * @param basisForProcessing GDPR Basis for processing
     * @param documentId ID of a GDPR basis document
     * @param documentVersion Version of the document
     * @param documentDescription Description of the document
     */
    public void enableGdprContext(@NonNull Basis basisForProcessing, @Nullable String documentId, @Nullable String documentVersion, @Nullable String documentDescription) {
        this.gdpr = new Gdpr(basisForProcessing, documentId, documentVersion, documentDescription);
    }

    /**
     * Disable GDPR context.
     */
    public synchronized void disableGdprContext() {
        this.gdpr = null;
    }

    @Nullable
    public Gdpr getGdprContext() {
        return gdpr;
    }

    // --- Setters

    /**
     * @param subject a valid subject object
     */
    public void setSubject(@Nullable Subject subject) {
        this.subject = subject;
    }

    /**
     * @param emitter a valid emitter object
     */
    public void setEmitter(@NonNull Emitter emitter) {
        // Need to shutdown prior emitter before updating
        getEmitter().shutdown();

        // Set the new emitter
        this.emitter = emitter;
    }

    /**
     * Whether the session context should be sent with events
     * @param sessionContext
     */
    public synchronized void setSessionContext(boolean sessionContext) {
        this.sessionContext = sessionContext;
        if (trackerSession != null && !sessionContext) {
            pauseSessionChecking();
            trackerSession = null;
        } else if (trackerSession == null && sessionContext) {
            Runnable[] callbacks = {null, null, null, null};
            if (sessionCallbacks.length == 4) {
                callbacks = sessionCallbacks;
            }
            trackerSession = Session.getInstance(context, foregroundTimeout, backgroundTimeout, timeUnit, namespace, callbacks);
        }
    }

    /**
     * @param platform a valid DevicePlatform object
     */
    public void setPlatform(@NonNull DevicePlatform platform) {
        this.devicePlatform = platform;
    }

    /** Internal use only */
    public void setScreenContext(boolean screenContext) {
        this.screenContext = screenContext;
        if (screenContext) {
            stateManager.addOrReplaceStateMachine(new ScreenStateMachine(), "ScreenContext");
        } else {
            stateManager.removeStateMachine("ScreenContext");
        }
    }

    /** Internal use only */
    public void setDeepLinkContext(boolean deepLinkContext) {
        this.deepLinkContext = deepLinkContext;
        if (this.deepLinkContext) {
            stateManager.addOrReplaceStateMachine(new DeepLinkStateMachine(), "DeepLinkContext");
        } else {
            stateManager.removeStateMachine("DeepLinkContext");
        }
    }

    /** Internal use only */
    public void setUserAnonymisation(boolean userAnonymisation) {
        if (this.userAnonymisation != userAnonymisation) {
            this.userAnonymisation = userAnonymisation;
            if (trackerSession != null) {
                trackerSession.startNewSession();
            }
        }
    }

    // --- Getters

    /** Internal use only */
    public boolean getScreenContext() {
        return screenContext;
    }

    /** Internal use only */
    public boolean getDeepLinkContext() {
        return deepLinkContext;
    }

    /** Internal use only */
    public boolean getSessionContext() {
        return sessionContext;
    }

    /** Internal use only */
    public boolean isUserAnonymisation() {
        return userAnonymisation;
    }

    /**
     * @return the tracker version that was set
     */
    @NonNull
    public String getTrackerVersion() {
        return this.trackerVersion;
    }

    /**
     * @return the trackers subject object
     */
    @Nullable
    public Subject getSubject() {
        return this.subject;
    }

    /**
     * @return the emitter associated with the tracker
     */
    @NonNull
    public Emitter getEmitter() {
        return this.emitter;
    }

    /**
     * @return the trackers namespace
     */
    @NonNull
    public String getNamespace() {
        return this.namespace;
    }

    /**
     * @return the trackers set Application ID
     */
    @NonNull
    public String getAppId() {
        return this.appId;
    }

    /**
     * @return the base64 setting of the tracker
     */
    public boolean getBase64Encoded() {
        return this.base64Encoded;
    }

    /**
     * @return the install tracking setting of the tracker
     */
    public boolean getInstallTracking() { return this.installTracking; }

    /**
     * @return the application context setting of the tracker
     */
    public boolean getApplicationContext() {
        return this.applicationContext;
    }

    /**
     * @return the trackers device platform
     */
    @NonNull
    public DevicePlatform getPlatform() {
        return this.devicePlatform;
    }

    /**
     * @return the trackers logging level
     */
    @NonNull
    public LogLevel getLogLevel() {
        return this.level;
    }

    /**
     * @return the trackers session object
     */
    @Nullable
    public Session getSession() {
        return this.trackerSession;
    }

    /**
     * @return the state of data collection
     */
    public boolean getDataCollection() {
        return this.dataCollection.get();
    }

    /**
     * @return the amount of threads to use
     */
    public int getThreadCount() { return this.threadCount; }

    /**
     * @return whether application crash tracking is on
     */
    public boolean getApplicationCrash() {
        return this.applicationCrash;
    }

    /**
     * @return whether application lifecycle tracking is on
     */
    public boolean getLifecycleEvents() {
        return this.lifecycleEvents;
    }

    /**
     * @return whether application lifecycle tracking is on
     */
    public boolean getActivityTracking() {
        return this.activityTracking;
    }

    /**
     * Internal use only
     * @return screen state from tracker
     */
    @Nullable
    public ScreenState getScreenState() {
        State state = stateManager.trackerState.getState("ScreenContext");
        if (state == null) {
            // Legacy initialization
            return new ScreenState();
        };
        if (state instanceof ScreenState) {
            return (ScreenState) state;
        }
        return null;
    }

    // --- Global contexts

    public void setGlobalContextGenerators(@NonNull Map<String, GlobalContext> globalContexts) {
        Objects.requireNonNull(globalContexts);
        synchronized (globalContextGenerators) {
            globalContextGenerators.clear();
            globalContextGenerators.putAll(globalContexts);
        }
    }

    public boolean addGlobalContext(@NonNull GlobalContext generator, @NonNull String tag) {
        Objects.requireNonNull(generator);
        Objects.requireNonNull(tag);
        synchronized (globalContextGenerators) {
            if (globalContextGenerators.containsKey(tag)) {
                return false;
            }
            globalContextGenerators.put(tag, generator);
            return true;
        }
    }

    @Nullable
    public GlobalContext removeGlobalContext(@NonNull String tag) {
        Objects.requireNonNull(tag);
        synchronized (globalContextGenerators) {
            return globalContextGenerators.remove(tag);
        }
    }

    @NonNull
    public Set<String> getGlobalContextTags() {
        return globalContextGenerators.keySet();
    }
}
