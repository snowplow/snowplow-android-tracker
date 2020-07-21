/*
 * Copyright (c) 2015-2020 Snowplow Analytics Ltd. All rights reserved.
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

package com.snowplowanalytics.snowplow.tracker;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.arch.lifecycle.ProcessLifecycleOwner;
import android.os.Handler;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.contexts.global.GlobalContext;
import com.snowplowanalytics.snowplow.tracker.contexts.global.GlobalContextUtils;
import com.snowplowanalytics.snowplow.tracker.events.Event;
import com.snowplowanalytics.snowplow.tracker.events.TrackerError;
import com.snowplowanalytics.snowplow.tracker.payload.Payload;
import com.snowplowanalytics.snowplow.tracker.payload.TrackerPayload;
import com.snowplowanalytics.snowplow.tracker.tracker.ActivityLifecycleHandler;
import com.snowplowanalytics.snowplow.tracker.tracker.ExceptionHandler;
import com.snowplowanalytics.snowplow.tracker.tracker.InstallTracker;
import com.snowplowanalytics.snowplow.tracker.tracker.ProcessObserver;
import com.snowplowanalytics.snowplow.tracker.tracker.ScreenState;
import com.snowplowanalytics.snowplow.tracker.utils.LogLevel;
import com.snowplowanalytics.snowplow.tracker.events.ScreenView;
import com.snowplowanalytics.snowplow.tracker.events.SelfDescribing;
import com.snowplowanalytics.snowplow.tracker.utils.Logger;
import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.utils.Util;
import com.snowplowanalytics.snowplow.tracker.Gdpr.Basis;


/**
 * Builds a Tracker object which is used to
 * send events to a Snowplow Collector.
 */
public class Tracker implements DiagnosticLogger {

    private final static String TAG = Tracker.class.getSimpleName();
    private final String trackerVersion = BuildConfig.TRACKER_LABEL;

    // --- Singleton Access

    private static Tracker spTracker = null;
    private static ScheduledExecutorService sessionExecutor = null;

    public static Tracker init(Tracker newTracker) {
        if (spTracker == null) {
            spTracker = newTracker;
            spTracker.resumeSessionChecking();
            spTracker.getEmitter().flush();
            spTracker.initializeScreenviewTracking();
        }
        return instance();
    }

    public static Tracker instance() {
        if (spTracker == null) {
            throw new IllegalStateException("FATAL: Tracker must be initialized first!");
        }

        if (spTracker.getApplicationCrash() && !(Thread.getDefaultUncaughtExceptionHandler() instanceof ExceptionHandler)) {
            Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
        }
        return spTracker;
    }

    public static void close() {
        if (spTracker != null) {
            spTracker.pauseSessionChecking();
            spTracker.getEmitter().shutdown();
            spTracker = null;
        }
    }

    // --- Builder

    private final Context context;
    private Emitter emitter;
    private Subject subject;
    private Session trackerSession;
    private String namespace;
    private String appId;
    private boolean base64Encoded;
    private DevicePlatforms devicePlatform;
    private LogLevel level;
    private boolean sessionContext;
    private long sessionCheckInterval;
    private Runnable[] sessionCallbacks;
    private int threadCount;
    private TimeUnit timeUnit;
    private boolean geoLocationContext;
    private boolean mobileContext;
    private boolean applicationCrash;
    private boolean trackerDiagnostic;
    private boolean lifecycleEvents;
    private boolean screenviewEvents;
    private boolean screenContext;
    private InstallTracker installTracker;
    private boolean installTracking;
    private boolean activityTracking;
    private boolean applicationContext;
    private Gdpr gdpr;
    private ScreenState screenState;

    private AtomicBoolean dataCollection = new AtomicBoolean(true);

    /**
     * Builder for the Tracker
     */
    public static class TrackerBuilder {

        final Emitter emitter; // Required
        final String namespace; // Required
        final String appId; // Required
        final Context context; // Required
        Subject subject = null; // Optional
        boolean base64Encoded = true; // Optional
        DevicePlatforms devicePlatform = DevicePlatforms.Mobile; // Optional
        LogLevel logLevel = LogLevel.OFF; // Optional
        boolean sessionContext = false; // Optional
        long foregroundTimeout = 600; // Optional - 10 minutes
        long backgroundTimeout = 300; // Optional - 5 minutes
        long sessionCheckInterval = 15; // Optional - 15 seconds
        Runnable[] sessionCallbacks = new Runnable[]{}; // Optional
        int threadCount = 10; // Optional
        TimeUnit timeUnit = TimeUnit.SECONDS; // Optional
        boolean geoLocationContext = false; // Optional
        boolean mobileContext = false; // Optional
        boolean applicationCrash = true; // Optional
        boolean trackerDiagnostic = false; // Optional
        boolean lifecycleEvents = false; // Optional
        boolean screenviewEvents = false; // Optional
        boolean screenContext = false; // Optional
        boolean activityTracking = false; // Optional
        boolean installTracking = false; // Optional
        boolean applicationContext = false; // Optional
        Gdpr gdpr = null; // Optional

        /**
         * @param emitter Emitter to which events will be sent
         * @param namespace Identifier for the Tracker instance
         * @param appId Application ID
         * @param context The Android application context
         */
        public TrackerBuilder(Emitter emitter, String namespace, String appId, Context context) {
            this.emitter = emitter;
            this.namespace = namespace;
            this.appId = appId;
            this.context = context;
        }

        /**
         * @param isEnabled Whether application contexts are sent with all events
         * @return itself
         */
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
        public TrackerBuilder gdprContext(@NonNull Basis basisForProcessing, String documentId, String documentVersion, String documentDescription) {
            this.gdpr = new Gdpr(basisForProcessing, documentId, documentVersion, documentDescription);
            return this;
        }

        /**
         * @param willTrack Whether install events will be tracked
         * @return itself
         */
        public TrackerBuilder installTracking(boolean willTrack) {
            this.installTracking = willTrack;
            return this;
        }

        /**
         * @param subject Subject to be tracked
         * @return itself
         */
        public TrackerBuilder subject(Subject subject) {
            this.subject = subject;
            return this;
        }

        /**
         * @param base64 Whether JSONs in the payload should be base-64 encoded
         * @return itself
         */
        public TrackerBuilder base64(Boolean base64) {
            this.base64Encoded = base64;
            return this;
        }

        /**
         * @param platform The device platform the tracker is running on
         * @return itself
         */
        public TrackerBuilder platform(DevicePlatforms platform) {
            this.devicePlatform = platform;
            return this;
        }

        /**
         * @param log The log level for the Tracker class
         * @return itself
         */
        public TrackerBuilder level(LogLevel log) {
            this.logLevel = log;
            return this;
        }

        /**
         * @param delegate The logger delegate that receive logs from the tracker.
         * @return itself
         */
        public TrackerBuilder loggerDelegate(LoggerDelegate delegate) {
            Logger.setDelegate(delegate);
            return this;
        }

        /**
         * @param sessionContext whether to add a session context
         * @return itself
         */
        public TrackerBuilder sessionContext(boolean sessionContext) {
            this.sessionContext = sessionContext;
            return this;
        }

        /**
         * @param timeout The session foreground timeout
         * @return itself
         */
        public TrackerBuilder foregroundTimeout(long timeout) {
            this.foregroundTimeout = timeout;
            return this;
        }

        /**
         * @param timeout The session background timeout
         * @return itself
         */
        public TrackerBuilder backgroundTimeout(long timeout) {
            this.backgroundTimeout = timeout;
            return this;
        }

        /**
         * @param sessionCheckInterval The session check interval
         * @return itself
         */
        public TrackerBuilder sessionCheckInterval(long sessionCheckInterval) {
            this.sessionCheckInterval = sessionCheckInterval;
            return this;
        }

        /**
         * @param foregroundTransitionCallback Called when session transitions to foreground
         * @param backgroundTransitionCallback Called when session transitions to background
         * @param foregroundTimeoutCallback Called when foregrounded session times-out
         * @param backgroundTimeoutCallback Called when backgrounded session times-out
         * @return itself
         */
        public TrackerBuilder sessionCallbacks(Runnable foregroundTransitionCallback,
                                               Runnable backgroundTransitionCallback,
                                               Runnable foregroundTimeoutCallback,
                                               Runnable backgroundTimeoutCallback)
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
        public TrackerBuilder threadCount(int threadCount) {
            this.threadCount = threadCount;
            return this;
        }

        /**
         * @param timeUnit a valid TimeUnit
         * @return itself
         */
        public TrackerBuilder timeUnit(TimeUnit timeUnit) {
            this.timeUnit = timeUnit;
            return this;
        }

        /**
         * @apiNote Requires Location permissions accordingly to the requirements of the various
         * Android versions. Otherwise the whole context is skipped.
         *
         * @param geoLocationContext whether to add a geo-location context
         * @return itself
         */
        public TrackerBuilder geoLocationContext(Boolean geoLocationContext) {
            this.geoLocationContext = geoLocationContext;
            return this;
        }

        /**
         * @param mobileContext whether to add a mobile context
         * @return itself
         */
        public TrackerBuilder mobileContext(Boolean mobileContext) {
            this.mobileContext = mobileContext;
            return this;
        }

        /**
         * @param applicationCrash whether to automatically track application
         *                         crashes
         * @return itself
         */
        public TrackerBuilder applicationCrash(Boolean applicationCrash) {
            this.applicationCrash = applicationCrash;
            return this;
        }

        /**
         * @param trackerDiagnostic whether to automatically track error within the tracker.
         * @return itself
         */
        public TrackerBuilder trackerDiagnostic(Boolean trackerDiagnostic) {
            this.trackerDiagnostic = trackerDiagnostic;
            return this;
        }

        /**
         * @apiNote It needs the Foreground library installed.
         *
         * @param lifecycleEvents whether to automatically track transition
         *                        from foreground to background
         * @return itself
         */
        public TrackerBuilder lifecycleEvents(Boolean lifecycleEvents) {
            this.lifecycleEvents = lifecycleEvents;
            return this;
        }

        /**
         * @param screenContext whether to send a screen context (info pertaining
         *                      to current screen) with every event
         * @return itself
         */
        public TrackerBuilder screenContext(Boolean screenContext) {
            this.screenContext = screenContext;
            return this;
        }

        /**
         * @param screenviewEvents whether to auto-track screenviews
         * @return itself
         */
        public TrackerBuilder screenviewEvents(Boolean screenviewEvents) {
            this.activityTracking = screenviewEvents;
            return this;
        }

        /**
         * @param activities whether to auto-track screenviews (onStart of activities)
         * @param onlyTrackLabelledScreens track only activities or fragments that have a Snowplow tag
         * @deprecated onlyTrackLabelledScreens can't filter the activities. Use {@link #screenviewEvents} instead.
         * @return itself
         */
        @Deprecated
        public TrackerBuilder screenviewEvents(Boolean activities,
                                               Boolean onlyTrackLabelledScreens) {
            return screenviewEvents(activities);
        }

        /**
         * Creates a new Tracker or throws an
         * Exception of we cannot find a suitable
         * extensible class.
         *
         * @return the new Tracker object
         */
        public Tracker build(){
            return init(new Tracker(this));
        }
    }

    /**
     * Creates a new Snowplow Tracker.
     *
     * @param builder The builder that constructs a tracker
     */
    private Tracker(TrackerBuilder builder) {

        this.context = builder.context;
        this.emitter = builder.emitter;
        this.appId = builder.appId;
        this.base64Encoded = builder.base64Encoded;
        this.namespace = builder.namespace;
        this.subject = builder.subject;
        this.devicePlatform = builder.devicePlatform;
        this.sessionContext = builder.sessionContext;
        this.sessionCheckInterval = builder.sessionCheckInterval;
        this.sessionCallbacks = builder.sessionCallbacks;
        this.threadCount = Math.max(builder.threadCount, 2);
        this.timeUnit = builder.timeUnit;
        this.geoLocationContext = builder.geoLocationContext;
        this.mobileContext = builder.mobileContext;
        this.applicationCrash = builder.applicationCrash;
        this.trackerDiagnostic = builder.trackerDiagnostic;
        this.lifecycleEvents = builder.lifecycleEvents;
        this.screenviewEvents = builder.screenviewEvents;
        this.activityTracking = builder.activityTracking;
        this.screenState = new ScreenState();
        this.screenContext = builder.screenContext;
        this.installTracking = builder.installTracking;
        this.applicationContext = builder.applicationContext;
        this.gdpr = builder.gdpr;
        this.level = builder.logLevel;

        if (trackerDiagnostic) {
            if (level == LogLevel.OFF) {
                level = LogLevel.ERROR;
            }
            Logger.setErrorLogger(this);
            Logger.updateLogLevel(level);
        }

        if (this.installTracking) {
            this.installTracker = new InstallTracker(this.context);
        } else {
            this.installTracker = null;
        }

        // When session context is enabled
        if (this.sessionContext) {
            Runnable[] callbacks = {null, null, null, null};
            if (sessionCallbacks.length == 4) {
                callbacks = sessionCallbacks;
            }
            this.trackerSession = Session.getInstance(
                    builder.foregroundTimeout,
                    builder.backgroundTimeout,
                    builder.timeUnit,
                    builder.context,
                    callbacks[0],
                    callbacks[1],
                    callbacks[2],
                    callbacks[3]
            );
        }

        // If lifecycleEvents is True
        if ((this.lifecycleEvents || this.sessionContext) &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {

            // addObserver must execute on the mainThread
            Handler mainHandler = new Handler(context.getMainLooper());
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    ProcessLifecycleOwner.get().getLifecycle().addObserver(new ProcessObserver());
                }
            });
        }

        Logger.v(TAG, "Tracker created successfully.");
    }

    // --- Diagnostic

    @Override
    public void log(String source, String errorMessage, Throwable throwable) {
        this.track(new TrackerError(source, errorMessage, throwable));
    }

    // --- Private init functions

    private void initializeScreenviewTracking() {
        if (this.activityTracking) {
            ActivityLifecycleHandler handler = new ActivityLifecycleHandler();
            Application application = (Application) context.getApplicationContext();
            application.registerActivityLifecycleCallbacks(handler);
        }
    }

    // --- Event Tracking Functions

    /**
     * Handles tracking the different types of events that
     * the Tracker can encounter.
     *
     * @param event the event to track
     */
    public void track(final Event event) {
        if (!dataCollection.get()) {
            return;
        }

        if (event instanceof ScreenView && screenState != null) {
            ScreenView screenView = (ScreenView) event;
            screenView.updateScreenState(screenState);
        }

        boolean reportsOnDiagnostic = !(event instanceof TrackerError);
        Executor.execute(reportsOnDiagnostic, TAG, () -> {
            event.beginProcessing(this);
            processEvent(event);
            event.endProcessing(this);
        });
    }

    private void processEvent(@NonNull Event event) {
        TrackerEvent trackerEvent = new TrackerEvent(event);
        Payload payload = payloadWithEvent(trackerEvent);
        Logger.v(TAG, "Adding new payload to event storage: %s", payload);
        this.emitter.add(payload);
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
        addGlobalContextsToContexts(contexts, payload);
        wrapContextsToPayload(payload, contexts);
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
            payload.addMap(new HashMap<>(this.subject.getSubject()));
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

    private void addBasicContextsToContexts(@NonNull List<SelfDescribingJson> contexts, @NonNull TrackerEvent event) {
        if (applicationContext) {
            contexts.add(InstallTracker.getApplicationContext(this.context));
        }

        if (mobileContext) {
            contexts.add(Util.getMobileContext(this.context));
        }

        if (event.isService) {
            return;
        }

        if (sessionContext) {
            String eventId = event.eventId.toString();
            if (trackerSession.getHasLoadedFromFile()) {
                synchronized (trackerSession) {
                    SelfDescribingJson sessionContextJson = trackerSession.getSessionContext(eventId);
                    if (sessionContextJson == null) {
                        Logger.track(TAG, "Method getSessionContext method returned null with eventId: %s", eventId);
                    }
                    contexts.add(sessionContextJson);
                }
            } else {
                Logger.track(TAG, "Method getHasLoadedFromFile method returned false with eventId: %s", eventId);
            }
        }

        if (geoLocationContext) {
            contexts.add(Util.getGeoLocationContext(this.context));
        }

        if (screenContext) {
            contexts.add(screenState.getCurrentScreen(true));
        }

        if (gdpr != null) {
            contexts.add(gdpr.getContext());
        }
    }

    private void addGlobalContextsToContexts(@NonNull List<SelfDescribingJson> contexts, @NonNull TrackerPayload payload) {
        synchronized (globalContexts) {
            if (!globalContexts.isEmpty()) {
                contexts.addAll(GlobalContextUtils.evalGlobalContexts(payload, globalContexts));
            }
        }
    }

    private void wrapContextsToPayload(@NonNull Payload payload, @NonNull List<SelfDescribingJson> contexts) {
        if (contexts.isEmpty()) {
            return;
        }
        List<Map> data = new LinkedList<>();
        for (SelfDescribingJson context : contexts) {
            if (context != null) {
                data.add(context.getMap());
            }
        }
        SelfDescribingJson finalContext = new SelfDescribingJson(TrackerConstants.SCHEMA_CONTEXTS, data);
        if (finalContext == null) {
            return;
        }
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
    private void addServiceEventPayload(Payload payload, List<SelfDescribingJson> contexts) {
        // Add default parameters to the payload
        payload.add(Parameters.PLATFORM, this.devicePlatform.getValue());
        payload.add(Parameters.APPID, this.appId);
        payload.add(Parameters.NAMESPACE, this.namespace);
        payload.add(Parameters.TRACKER_VERSION, this.trackerVersion);

        // If there is a subject present for the Tracker add it
        if (this.subject != null) {
            payload.addMap(new HashMap<>(this.subject.getSubject()));
        }

        // Add Mobile Context
        if (this.mobileContext) {
            contexts.add(Util.getMobileContext(this.context));
        }

        // Add application context
        if (this.applicationContext) {
            contexts.add(InstallTracker.getApplicationContext(this.context));
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
     * Starts a polling session checker to
     * run at a defined interval.
     */
    public void resumeSessionChecking() {
        if (sessionExecutor == null && this.sessionContext) {
            Logger.d(TAG, "Session checking has been resumed.");
            final Session session = this.trackerSession;
            sessionExecutor = Executors.newSingleThreadScheduledExecutor();
            sessionExecutor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    try {
                        session.checkAndUpdateSession();
                    } catch (Exception e) {
                        Logger.track(TAG, "Method checkAndUpdateSession raised an exception: %s", e);
                    }
                }
            }, this.sessionCheckInterval, this.sessionCheckInterval, this.timeUnit);
        }
    }

    /**
     * Ends the polling session checker.
     */
    public void pauseSessionChecking() {
        if (sessionExecutor != null) {
            Logger.d(TAG, "Session checking has been paused.");
            sessionExecutor.shutdown();
            sessionExecutor = null;
        }
    }

    /**
     * Polling will continue, but only accessedLast time will be updated.
     * Effectively persists session, call the method with false to end suspension.
     */
    public void suspendSessionChecking(boolean isSuspended) {
        if (sessionExecutor != null && this.trackerSession != null) {
            final Session session = this.trackerSession;
            session.setIsSuspended(isSuspended);
        }
    }

    /**
     * Convenience function for starting a new session.
     */
    public void startNewSession() {
        pauseSessionChecking();
        resumeSessionChecking();
    }

    // --- GDPR context

    /**
     * Enables GDPR context to be sent with every event.
     * @param basisForProcessing GDPR Basis for processing
     * @param documentId ID of a GDPR basis document
     * @param documentVersion Version of the document
     * @param documentDescription Description of the document
     */
    public void enableGdprContext(@NonNull Basis basisForProcessing, String documentId, String documentVersion, String documentDescription) {
        this.gdpr = new Gdpr(basisForProcessing, documentId, documentVersion, documentDescription);
    }

    /**
     * Disable GDPR context.
     */
    public synchronized void disableGdprContext() {
        this.gdpr = null;
    }

    // --- Setters

    /**
     * @param subject a valid subject object
     */
    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    /**
     * @param emitter a valid emitter object
     */
    public void setEmitter(Emitter emitter) {
        // Need to shutdown prior emitter before updating
        getEmitter().shutdown();

        // Set the new emitter
        this.emitter = emitter;
    }

    /**
     * Whether the session context should be sent with events
     * @param shouldSend
     */
    public void setSessionContext(boolean shouldSend) { this.sessionContext = shouldSend; }

    /**
     * @param platform a valid DevicePlatforms object
     */
    public void setPlatform(DevicePlatforms platform) {
        this.devicePlatform = platform;
    }

    // --- Getters

    /**
     * @return the tracker version that was set
     */
    public String getTrackerVersion() {
        return this.trackerVersion;
    }

    /**
     * @return the trackers subject object
     */
    public Subject getSubject() {
        return this.subject;
    }

    /**
     * @return the emitter associated with the tracker
     */
    public Emitter getEmitter() {
        return this.emitter;
    }

    /**
     * @return the trackers namespace
     */
    public String getNamespace() {
        return this.namespace;
    }

    /**
     * @return the trackers set Application ID
     */
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
    public DevicePlatforms getPlatform() {
        return this.devicePlatform;
    }

    /**
     * @return the trackers logging level
     */
    public LogLevel getLogLevel() {
        return this.level;
    }

    /**
     * @return the trackers session object
     */
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
     * @return whether application lifecycle tracking is on
     * @deprecated onlyTrackLabelledScreens is always set to false.
     */
    @Deprecated
    public boolean getOnlyTrackLabelledScreens() {
        return false;
    }

    /**
     * @return screen state from tracker
     */
    public ScreenState getScreenState() {
        return this.screenState;
    }

    /**
     * Track a screen with a screen state (many options)
     * @deprecated Use track(Event) method passing a ScreenView event.
     */
    @Deprecated
    public void trackScreen(ScreenState screenState) {
        this.screenState = screenState;
        SelfDescribingJson data = screenState.getScreenViewEventJson();
        track(SelfDescribing.builder()
                .eventData(data)
                .build()
        );
    }

    /**
     * Track a screen only by name
     * @deprecated Use track(Event) method passing a ScreenView event.
     */
    @Deprecated
    public void trackScreen(String name) {
        screenState.updateScreenState(null, name, null, null);
        SelfDescribingJson data = screenState.getScreenViewEventJson();
        track(SelfDescribing.builder()
                .eventData(data)
                .build()
        );
    }

    // --- Global contexts

    private final List<GlobalContext> globalContexts = Collections.synchronizedList(new ArrayList<GlobalContext>());

    public void clearGlobalContexts() {
        globalContexts.clear();
    }

    public void addGlobalContext(GlobalContext context) {
        globalContexts.add(context);
    }

    public void addGlobalContexts(List<GlobalContext> contexts) {
        for (GlobalContext context : contexts) {
            addGlobalContext(context);
        }
    }

    public ArrayList<GlobalContext> getGlobalContexts() {
        return new ArrayList<>(globalContexts);
    }

    public void setGlobalContexts(List<GlobalContext> contexts) {
        clearGlobalContexts();
        addGlobalContexts(contexts);
    }

    public void removeGlobalContexts(List<String> tags) {
        for (String tag: tags) {
            removeGlobalContext(tag);
        }
    }

    public void removeGlobalContext(String tag) {
        synchronized (globalContexts) {
            Iterator<GlobalContext> it = globalContexts.iterator();

            while(it.hasNext()){
                GlobalContext globalContext = it.next();
                if (globalContext.tag().equals(tag)) {
                    it.remove();
                }
            }
        }
    }
}
