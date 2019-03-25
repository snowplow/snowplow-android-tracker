/*
 * Copyright (c) 2015-2017 Snowplow Analytics Ltd. All rights reserved.
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

import android.annotation.TargetApi;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.arch.lifecycle.ProcessLifecycleOwner;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.events.Event;
import com.snowplowanalytics.snowplow.tracker.events.Timing;
import com.snowplowanalytics.snowplow.tracker.payload.TrackerPayload;
import com.snowplowanalytics.snowplow.tracker.tracker.ActivityLifecycleHandler;
import com.snowplowanalytics.snowplow.tracker.tracker.ExceptionHandler;
import com.snowplowanalytics.snowplow.tracker.tracker.InstallTracker;
import com.snowplowanalytics.snowplow.tracker.tracker.ProcessObserver;
import com.snowplowanalytics.snowplow.tracker.tracker.ScreenState;
import com.snowplowanalytics.snowplow.tracker.utils.LogLevel;
import com.snowplowanalytics.snowplow.tracker.events.EcommerceTransaction;
import com.snowplowanalytics.snowplow.tracker.events.EcommerceTransactionItem;
import com.snowplowanalytics.snowplow.tracker.events.PageView;
import com.snowplowanalytics.snowplow.tracker.events.ScreenView;
import com.snowplowanalytics.snowplow.tracker.events.Structured;
import com.snowplowanalytics.snowplow.tracker.events.SelfDescribing;
import com.snowplowanalytics.snowplow.tracker.events.ConsentWithdrawn;
import com.snowplowanalytics.snowplow.tracker.events.ConsentGranted;
import com.snowplowanalytics.snowplow.tracker.events.ConsentDocument;
import com.snowplowanalytics.snowplow.tracker.utils.Logger;
import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.utils.Util;

/**
 * Builds a Tracker object which is used to
 * send events to a Snowplow Collector.
 */
public class Tracker {

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
    private boolean lifecycleEvents;
    private boolean screenviewEvents;
    private boolean screenContext;
    private boolean installEvent;
    private InstallTracker installTracker;
    private boolean installTracking;
    private boolean activityTracking;
    private boolean onlyTrackLabelledScreens;
    private boolean applicationContext;
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
        boolean lifecycleEvents = false; // Optional
        boolean screenviewEvents = false; // Optional
        boolean screenContext = false; // Optional
        boolean activityTracking = false; // Optional
        boolean onlyTrackLabelledScreens = false; // Optional
        boolean installTracking = false; // Optional
        boolean applicationContext = false; // Optional

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
         * NOTE: Only available on API 14+ and with the Foreground library
         * installed.
         *
         * @param lifecycleEvents whether to automatically track transition
         *                        from foreground to background
         * @return itself
         */
        @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
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
            this.activityTracking = true;
            return this;
        }

        /**
         * @param activities whether to auto-track screenviews (onStart of activities)
         * @param onlyTrackLabelledScreens track only activities or fragments that have a Snowplow tag
         * @return itself
         */
        public TrackerBuilder screenviewEvents(Boolean activities,
                                               Boolean onlyTrackLabelledScreens) {
            this.activityTracking = activities;
            this.onlyTrackLabelledScreens = onlyTrackLabelledScreens;
            return this;
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
        this.level = builder.logLevel;
        this.sessionContext = builder.sessionContext;
        this.sessionCheckInterval = builder.sessionCheckInterval;
        this.sessionCallbacks = builder.sessionCallbacks;
        this.threadCount = builder.threadCount < 2 ? 2 : builder.threadCount;
        this.timeUnit = builder.timeUnit;
        this.geoLocationContext = builder.geoLocationContext;
        this.mobileContext = builder.mobileContext;
        this.applicationCrash = builder.applicationCrash;
        this.lifecycleEvents = builder.lifecycleEvents;
        this.screenviewEvents = builder.screenviewEvents;
        this.activityTracking = builder.activityTracking;
        this.onlyTrackLabelledScreens = builder.onlyTrackLabelledScreens;
        this.screenState = new ScreenState();
        this.screenContext = builder.screenContext;
        this.installTracking = builder.installTracking;
        this.applicationContext = builder.applicationContext;

        if (this.installTracking) {
            this.installTracker = new InstallTracker(this.context);
        } else {
            this.installTracker = null;
        }

        // When session context is enabled
        if (this.sessionContext) {
            if (sessionCallbacks.length == 4) {
                this.trackerSession = new Session(
                        builder.foregroundTimeout,
                        builder.backgroundTimeout,
                        builder.timeUnit,
                        builder.context,
                        builder.sessionCallbacks[0],
                        builder.sessionCallbacks[1],
                        builder.sessionCallbacks[2],
                        builder.sessionCallbacks[3]
                );
            } else {
                this.trackerSession = new Session(
                        builder.foregroundTimeout,
                        builder.backgroundTimeout,
                        builder.timeUnit,
                        builder.context
                );
            }

        }

        // If lifecycleEvents is True
        if ((this.lifecycleEvents || this.sessionContext) &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            // oddly adding this observer loads a file - needs to be called off main thread
            Executor.execute(new Runnable() {
                @Override
                public void run() {
                    ProcessLifecycleOwner.get().getLifecycle().addObserver(new ProcessObserver());
                }
            });
        }

        Logger.updateLogLevel(builder.logLevel);
        Logger.v(TAG, "Tracker created successfully.");
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

        Executor.execute(new Runnable() {
            @Override
            public void run() {
                List<SelfDescribingJson> context = event.getContext();
                String eventId = event.getEventId();

                // Figure out what type of event it is and track it!
                Class eClass = event.getClass();
                if (eClass.equals(PageView.class) || eClass.equals(Structured.class)) {
                    addEventPayload((TrackerPayload) event.getPayload(), context, eventId);
                } else if (eClass.equals(EcommerceTransaction.class)) {
                    addEventPayload((TrackerPayload) event.getPayload(), context, eventId);

                    // Track each item individually
                    EcommerceTransaction ecommerceTransaction = (EcommerceTransaction) event;
                    for(EcommerceTransactionItem item : ecommerceTransaction.getItems()) {
                        item.setDeviceCreatedTimestamp(ecommerceTransaction.getDeviceCreatedTimestamp());
                        addEventPayload(item.getPayload(), item.getContext(), item.getEventId());
                    }
                } else if (eClass.equals(SelfDescribing.class)) {

                    // Need to set the Base64 rule for SelfDescribing events
                    SelfDescribing selfDescribing = (SelfDescribing) event;
                    selfDescribing.setBase64Encode(base64Encoded);
                    addEventPayload(selfDescribing.getPayload(), context, eventId);
                } else if (eClass.equals(Timing.class) || eClass.equals(ScreenView.class)) {
                    SelfDescribing selfDescribing = SelfDescribing.builder()
                            .eventData((SelfDescribingJson) event.getPayload())
                            .customContext(context)
                            .deviceCreatedTimestamp(event.getDeviceCreatedTimestamp())
                            .eventId(event.getEventId())
                            .build();

                    // Need to set the Base64 rule for SelfDescribing events
                    selfDescribing.setBase64Encode(base64Encoded);
                    addEventPayload(selfDescribing.getPayload(), context, eventId);
                } else if (eClass.equals(ConsentGranted.class)) {
                    List<ConsentDocument> documents = ((ConsentGranted) event).getConsentDocuments();
                    List<SelfDescribingJson> sdjDocuments = new LinkedList<>();
                    for (ConsentDocument document : documents) {
                        sdjDocuments.add(document.getPayload());
                    }
                    context.addAll(sdjDocuments);

                    SelfDescribing selfDescribing = SelfDescribing.builder()
                            .eventData((SelfDescribingJson) event.getPayload())
                            .customContext(context)
                            .deviceCreatedTimestamp(event.getDeviceCreatedTimestamp())
                            .build();

                    // Need to set the Base64 rule for SelfDescribing events
                    selfDescribing.setBase64Encode(base64Encoded);
                    addEventPayload(selfDescribing.getPayload(), context, eventId);
                } else if (eClass.equals(ConsentWithdrawn.class)) {
                    List<ConsentDocument> documents = ((ConsentWithdrawn) event).getConsentDocuments();
                    List<SelfDescribingJson> sdjDocuments = new LinkedList<>();
                    for (ConsentDocument document : documents) {
                        sdjDocuments.add(document.getPayload());
                    }
                    context.addAll(sdjDocuments);

                    SelfDescribing selfDescribing = SelfDescribing.builder()
                            .eventData((SelfDescribingJson) event.getPayload())
                            .customContext(context)
                            .deviceCreatedTimestamp(event.getDeviceCreatedTimestamp())
                            .build();

                    // Need to set the Base64 rule for SelfDescribing events
                    selfDescribing.setBase64Encode(base64Encoded);
                    addEventPayload(selfDescribing.getPayload(), context, eventId);
                }
            }
        });
    }

    // --- Helpers

    /**
     * Builds and adds a finalized payload by adding in extra
     * information to the payload:
     * - The event contexts
     * - The Tracker Subject
     * - The Tracker parameters
     *
     * @param payload Payload the raw event payload to be
     *                decorated.
     * @param eventId The event id
     * @param context The raw context list
     */
    private void addEventPayload(TrackerPayload payload, List<SelfDescribingJson> context,
                                 String eventId) {

        // Add default parameters to the payload
        payload.add(Parameters.PLATFORM, this.devicePlatform.getValue());
        payload.add(Parameters.APPID, this.appId);
        payload.add(Parameters.NAMESPACE, this.namespace);
        payload.add(Parameters.TRACKER_VERSION, this.trackerVersion);

        // If there is a subject present for the Tracker add it
        if (this.subject != null) {
            payload.addMap(new HashMap<String,Object>(this.subject.getSubject()));
        }

        // Build the final context and add it
        SelfDescribingJson envelope = getFinalContext(context, eventId);
        if (envelope != null) {
            payload.addMap(envelope.getMap(), this.base64Encoded, Parameters.CONTEXT_ENCODED,
                    Parameters.CONTEXT);
        }

        // Add this payload to the emitter
        Logger.v(TAG, "Adding new payload to event storage: %s", payload);
        this.emitter.add(payload);
    }

    /**
     * Builds the final event context.
     *
     * @param contexts the base event context
     * @param eventId the event id
     * @return the final event context json with
     *         many contexts inside
     */
    private SelfDescribingJson getFinalContext(List<SelfDescribingJson> contexts, String eventId) {

        // Add session context
        if (this.sessionContext && this.trackerSession.getHasLoadedFromFile()) {
            contexts.add(this.trackerSession.getSessionContext(eventId));
        }

        // Add Geo-Location Context
        if (this.geoLocationContext) {
            contexts.add(Util.getGeoLocationContext(this.context));
        }

        // Add Mobile Context
        if (this.mobileContext) {
            contexts.add(Util.getMobileContext(this.context));
        }

        // Add screen context
        if (this.screenContext) {
            contexts.add(screenState.getCurrentScreen(true));
        }

        // Add application context
        if (this.applicationContext) {
            contexts.add(InstallTracker.getApplicationContext(this.context));
        }

        // If there are contexts to nest
        if (contexts.size() == 0) {
            return null;
        } else {
            List<Map> contextMaps = new LinkedList<>();
            for (SelfDescribingJson selfDescribingJson : contexts) {
                if (selfDescribingJson != null) {
                    contextMaps.add(selfDescribingJson.getMap());
                }
            }
            return new SelfDescribingJson(TrackerConstants.SCHEMA_CONTEXTS, contextMaps);
        }
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
                    session.checkAndUpdateSession();
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
     */
    public boolean getOnlyTrackLabelledScreens() {
        return this.onlyTrackLabelledScreens;
    }

    /**
     * @return screen state from tracker
     */
    public ScreenState getScreenState() {
        return this.screenState;
    }

    /**
     * Track a screen with a screen state (many options)
     */
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
     */
    public void trackScreen(String name) {
        screenState.newScreenState(name, null, null);
        SelfDescribingJson data = screenState.getScreenViewEventJson();
        track(SelfDescribing.builder()
                .eventData(data)
                .build()
        );
    }
}
