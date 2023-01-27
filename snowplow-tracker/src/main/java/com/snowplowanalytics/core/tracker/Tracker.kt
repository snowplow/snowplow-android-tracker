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
package com.snowplowanalytics.core.tracker

import android.content.Context
import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.core.emitter.Emitter
import com.snowplowanalytics.core.emitter.Executor.execute
import com.snowplowanalytics.core.gdpr.Gdpr
import com.snowplowanalytics.core.session.ProcessObserver.Companion.initialize
import com.snowplowanalytics.core.session.Session
import com.snowplowanalytics.core.session.Session.Companion.getInstance
import com.snowplowanalytics.core.tracker.Logger.d
import com.snowplowanalytics.core.tracker.Logger.track
import com.snowplowanalytics.core.tracker.Logger.updateLogLevel
import com.snowplowanalytics.core.tracker.Logger.v
import com.snowplowanalytics.core.utils.NotificationCenter.FunctionalObserver
import com.snowplowanalytics.core.utils.NotificationCenter.addObserver
import com.snowplowanalytics.core.utils.NotificationCenter.removeObserver
import com.snowplowanalytics.core.utils.Util.getApplicationContext
import com.snowplowanalytics.core.utils.Util.getGeoLocationContext
import com.snowplowanalytics.snowplow.entity.DeepLink
import com.snowplowanalytics.snowplow.event.*
import com.snowplowanalytics.snowplow.globalcontexts.GlobalContext
import com.snowplowanalytics.snowplow.payload.Payload
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow.payload.TrackerPayload
import com.snowplowanalytics.snowplow.tracker.*
import com.snowplowanalytics.snowplow.util.Basis
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

/**
 * Builds a Tracker object which is used to send events to a Snowplow Collector.
 *
 * @param emitter Emitter to which events will be sent
 * @param namespace Identifier for the Tracker instance
 * @param appId Application ID
 * @param context The Android application context
 * @param builder A closure to set Tracker configuration
 */
class Tracker(emitter: Emitter, val namespace: String, var appId: String, context: Context, builder: ((Tracker) -> Unit)? = null) {
    private var builderFinished = false
    private val context: Context
    private val stateManager = StateManager()
    
    fun getScreenState(): ScreenState? {
        val state = stateManager.trackerState.getState("ScreenContext")
            ?: // Legacy initialization
            return ScreenState()

        return if (state is ScreenState) {
            state
        } else null
    }
    
    private var trackerVersion = BuildConfig.TRACKER_LABEL
        set(version) {
            if (!builderFinished) {
                field = version
            }
        }
    
    private val _dataCollection = AtomicBoolean(true)
    val dataCollection: Boolean
        get() = _dataCollection.get()
    
    private val platformContextManager = PlatformContext(context)

    var emitter: Emitter = emitter
        set(emitter) {
            // Need to shutdown previous emitter before updating
            field.shutdown()
            field = emitter
        }

    var subject: Subject? = null
    var session: Session? = null
    
    var base64Encoded: Boolean = TrackerDefaults.base64Encoded
        set(base64) {
            if (!builderFinished) {
                field = base64
            }
        }
    
    var platform: DevicePlatform = TrackerDefaults.devicePlatform
    var logLevel: LogLevel = TrackerDefaults.logLevel
        set(level) {
            if (!builderFinished) {
                field = level
            }
        }
    
    var foregroundTimeout: Long = TrackerDefaults.foregroundTimeout
        set(timeout) {
            if (!builderFinished) {
                field = timeout
            }
        }
    
    var backgroundTimeout: Long = TrackerDefaults.backgroundTimeout
        set(timeout) {
            if (!builderFinished) {
                field = timeout
            }
        }

    /**
     * This configuration option is not published in the TrackerConfiguration class.
     * Create a Tracker directly, not via the Snowplow interface, to configure threadCount.
     */
    var threadCount: Int = TrackerDefaults.threadCount
        set(threadCount) {
            if (!builderFinished) {
                field = max(threadCount, 2)
            }
        }

    /**
    * This configuration option is not published in the TrackerConfiguration class.
    * Create a Tracker directly, not via the Snowplow interface, to configure timeUnit.
    */
    var timeUnit: TimeUnit = TrackerDefaults.timeUnit
        set(timeunit) {
            if (!builderFinished) {
                field = timeunit
            }
        }

    var exceptionAutotracking: Boolean = TrackerDefaults.exceptionAutotracking
        set(willTrack) {
            if (!builderFinished) {
                field = willTrack
            }
        }

    var diagnosticAutotracking: Boolean = TrackerDefaults.diagnosticAutotracking
        set(willTrack) {
            if (!builderFinished) {
                field = willTrack
            }
        }
    
    var lifecycleAutotracking: Boolean = TrackerDefaults.lifecycleAutotracking
        set(willTrack) {
            if (!builderFinished) {
                field = willTrack
            }
        }
    
    var installAutotracking: Boolean = TrackerDefaults.installAutotracking
        set(willTrack) {
            if (!builderFinished) {
                field = willTrack
            }
        }

    var screenViewAutotracking: Boolean = TrackerDefaults.screenViewAutotracking
        set(willTrack) {
            if (!builderFinished) {
                field = willTrack
            }
        }

    /** Internal use only  */
    var userAnonymisation: Boolean = TrackerDefaults.userAnonymisation
        set(userAnonymisation) {
            if (!builderFinished) {
                field = userAnonymisation
            } else if ((field != userAnonymisation) && builderFinished) {
                    field = userAnonymisation
                    session?.startNewSession()
            }
        }

    /**
     * Internal use only.
     * Decorate the `tv` (tracker version) field in the tracker protocol.
     */
    var trackerVersionSuffix: String? = null
        set(suffix) {
            if (!builderFinished) {
                field = suffix
            }
        }

    /**
     * This configuration option is not published in the TrackerConfiguration class.
     * Create a Tracker directly, not via the Snowplow interface, to configure sessionCallbacks.
     * 
     * A set of callbacks. Four callbacks must be provided, in this order, even if some are null: 
     * * foregroundTransitionCallback. Called when session transitions to foreground
     * * backgroundTransitionCallback. Called when session transitions to background
     * * foregroundTimeoutCallback. Called when foregrounded session times-out
     * * backgroundTimeoutCallback. Called when backgrounded session times-out
     */
    var sessionCallbacks: Array<Runnable?> = arrayOf(null, null, null, null)
        set(callbacksArray) {
            if (!builderFinished) {
                field = callbacksArray
            }
        }

    /**
     * Whether the session context should be sent with events
     */
    var sessionContext: Boolean = TrackerDefaults.sessionContext
        @Synchronized
        set(sessionContext) {
            field = sessionContext
            
            if (session != null && !sessionContext) {
                pauseSessionChecking()
                session = null
            } else if (session == null && sessionContext) {
                var callbacks = arrayOf<Runnable?>(null, null, null, null)
                if (sessionCallbacks.size == 4) {
                    callbacks = sessionCallbacks
                }
                session = getInstance(
                    context,
                    foregroundTimeout,
                    backgroundTimeout,
                    timeUnit,
                    namespace,
                    callbacks
                )
            }
        }
    
    var geoLocationContext: Boolean = TrackerDefaults.geoLocationContext
        set(geolocation) {
            if (!builderFinished) {
                field = geolocation
            }
        }
    
    var platformContextEnabled: Boolean = TrackerDefaults.platformContext
        set(mobile) {
            if (!builderFinished) {
                field = mobile
            }
        }
    
    var applicationContext: Boolean = TrackerDefaults.applicationContext
        set(application) {
            if (!builderFinished) {
                field = application
            }
        }
    
    /** Internal use only  */
    var deepLinkContext = false
        set(deepLinkContext) {
            field = deepLinkContext
            if (deepLinkContext) {
                stateManager.addOrReplaceStateMachine(DeepLinkStateMachine(), "DeepLinkContext")
            } else {
                stateManager.removeStateMachine("DeepLinkContext")
            }
        }

    /** Internal use only  */
    var screenContext = false
        set(screenContext) {
            field = screenContext
            if (screenContext) {
                stateManager.addOrReplaceStateMachine(ScreenStateMachine(), "ScreenContext")
            } else {
                stateManager.removeStateMachine("ScreenContext")
            }
        }
    
    var gdprContext: Gdpr? = null
    
    var loggerDelegate: LoggerDelegate? = null
        set(delegate) {
            if (!builderFinished) {
                field = delegate
                Logger.delegate = delegate
            }
        }
    
    private val globalContextGenerators =
        Collections.synchronizedMap(HashMap<String, GlobalContext>())
    
    private val receiveLifecycleNotification: FunctionalObserver = object : FunctionalObserver() {
        override fun apply(data: Map<String, Any>) {
            val session = session
            if (session == null || !lifecycleAutotracking) {
                return
            }
            val isForeground = data["isForeground"] as? Boolean? ?: return
            if (session.isBackground == !isForeground) {
                // if the new lifecycle state confirms the session state, there isn't any lifecycle transition
                return
            }
            if (isForeground) {
                track(Foreground().foregroundIndex(session.foregroundIndex + 1))
            } else {
                track(Background().backgroundIndex(session.backgroundIndex + 1))
            }
            session.setBackground(!isForeground)
        }
    }
    private val receiveScreenViewNotification: FunctionalObserver = object : FunctionalObserver() {
        override fun apply(data: Map<String, Any>) {
            if (screenViewAutotracking) {
                val event = data["event"] as? Event?
                event?.let { track(it) }
            }
        }
    }
    private val receiveInstallNotification: FunctionalObserver = object : FunctionalObserver() {
        override fun apply(data: Map<String, Any>) {
            if (installAutotracking) {
                val event = data["event"] as? Event?
                event?.let { track(it) }
            }
        }
    }
    private val receiveDiagnosticNotification: FunctionalObserver = object : FunctionalObserver() {
        override fun apply(data: Map<String, Any>) {
            if (diagnosticAutotracking) {
                val event = data["event"] as? Event?
                event?.let { track(it) }
            }
        }
    }
    private val receiveCrashReportingNotification: FunctionalObserver =
        object : FunctionalObserver() {
            override fun apply(data: Map<String, Any>) {
                if (exceptionAutotracking) {
                    val event = data["event"] as? Event?
                    event?.let { track(it) }
                }
            }
        }


    /**
     * Creates a new Snowplow Tracker.
     */
    init {
        this.context = context
        builder?.let { it(this) }
        
        emitter.flush()
        
        // Setting the emitter namespace has a side-effect of creating a SQLiteEventStore,
        // unless an EventStore was already provided through EmitterConfiguration
        emitter.namespace = namespace
        
        trackerVersionSuffix?.let {
            val suffix = it.replace("[^A-Za-z0-9.-]".toRegex(), "")
            if (suffix.isNotEmpty()) {
                trackerVersion = "$trackerVersion $suffix"
            }
        }
        
        if (diagnosticAutotracking && logLevel === LogLevel.OFF) {
            logLevel = LogLevel.ERROR
        }
        updateLogLevel(logLevel)

        // When session context is enabled
        if (sessionContext) {
            var callbacks = arrayOf<Runnable?>(null, null, null, null)
            if (sessionCallbacks.size == 4) {
                callbacks = sessionCallbacks
            }
            session = getInstance(
                context,
                foregroundTimeout,
                backgroundTimeout,
                timeUnit,
                namespace,
                callbacks
            )
        }

        // Register notification receivers from singleton services
        registerNotificationHandlers()

        // Initialization of services shared with all the tracker instances
        initializeCrashReporting()
        initializeInstallTracking()
        initializeScreenviewTracking()
        initializeLifecycleTracking()

        // Resume session
        resumeSessionChecking()
        builderFinished = true
        v(TAG, "Tracker created successfully.")
    }

    // --- Private init functions
    private fun registerNotificationHandlers() {
        addObserver("SnowplowTrackerDiagnostic", receiveDiagnosticNotification)
        addObserver("SnowplowScreenView", receiveScreenViewNotification)
        addObserver("SnowplowLifecycleTracking", receiveLifecycleNotification)
        addObserver("SnowplowInstallTracking", receiveInstallNotification)
        addObserver("SnowplowCrashReporting", receiveCrashReportingNotification)
    }

    private fun unregisterNotificationHandlers() {
        removeObserver(receiveDiagnosticNotification)
        removeObserver(receiveScreenViewNotification)
        removeObserver(receiveLifecycleNotification)
        removeObserver(receiveInstallNotification)
        removeObserver(receiveCrashReportingNotification)
    }

    private fun initializeInstallTracking() {
        if (installAutotracking) {
            InstallTracker.getInstance(context)
        }
    }

    private fun initializeScreenviewTracking() {
        if (screenViewAutotracking) {
            ActivityLifecycleHandler.getInstance(context)
        }
    }

    private fun initializeLifecycleTracking() {
        if (lifecycleAutotracking) {
            initialize(context)
            // Initialize LifecycleStateMachine for lifecycle entities
            stateManager.addOrReplaceStateMachine(LifecycleStateMachine(), "Lifecycle")
        }
    }

    private fun initializeCrashReporting() {
        if (exceptionAutotracking && Thread.getDefaultUncaughtExceptionHandler() !is ExceptionHandler) {
            Thread.setDefaultUncaughtExceptionHandler(ExceptionHandler())
        }
    }

    fun close() {
        unregisterNotificationHandlers()
        pauseSessionChecking()
        emitter.shutdown()
    }
    // --- Event Tracking Functions
    
    /**
     * Handles tracking the different types of events that
     * the Tracker can encounter.
     *
     * @param event the event to track
     * @return The event ID or null in case tracking is paused
     */
    fun track(event: Event): UUID? {
        if (!dataCollection) {
            return null
        }
        
        event.beginProcessing(this)
        var stateSnapshot: TrackerStateSnapshot
        var trackerEvent: TrackerEvent
        synchronized(this) {
            stateSnapshot = stateManager.trackerStateForProcessedEvent(event)
            trackerEvent = TrackerEvent(event, stateSnapshot)
            workaroundForIncoherentSessionContext(trackerEvent)
        }
        val reportsOnDiagnostic = event !is TrackerError
        execute(reportsOnDiagnostic, TAG) {
            transformEvent(trackerEvent)
            val payload = payloadWithEvent(trackerEvent)
            v(TAG, "Adding new payload to event storage: %s", payload)
            emitter.add(payload)
            event.endProcessing(this)
        }
        return trackerEvent.eventId
    }

    private fun transformEvent(event: TrackerEvent) {
        // Application_install event needs the timestamp to the real installation event.
        if (event.schema != null && event.schema == TrackerConstants.SCHEMA_APPLICATION_INSTALL) {
            event.trueTimestamp?.let { event.timestamp = it }
            event.trueTimestamp = null
        }
        // Payload can be optionally updated with values based on internal state
        stateManager.addPayloadValuesToEvent(event)
    }

    private fun payloadWithEvent(event: TrackerEvent): Payload {
        val payload = TrackerPayload()
        addBasicPropertiesToPayload(payload, event)
        if (event.isPrimitive) {
            addPrimitivePropertiesToPayload(payload, event)
        } else {
            addSelfDescribingPropertiesToPayload(payload, event)
        }
        val contexts = event.contexts
        addBasicContextsToContexts(contexts, event)
        addGlobalContextsToContexts(contexts, event)
        addStateMachineEntitiesToContexts(contexts, event)
        wrapContextsToPayload(payload, contexts)
        if (!event.isPrimitive) {
            // TODO: To remove when Atomic table refactoring is finished
            workaroundForCampaignAttributionEnrichment(payload, event, contexts)
        }
        return payload
    }

    private fun addBasicPropertiesToPayload(payload: Payload, event: TrackerEvent) {
        payload.add(Parameters.EID, event.eventId.toString())
        payload.add(Parameters.DEVICE_TIMESTAMP, event.timestamp.toString())
        event.trueTimestamp?.let { payload.add(Parameters.TRUE_TIMESTAMP, it.toString()) }
        payload.add(Parameters.APPID, appId)
        payload.add(Parameters.NAMESPACE, namespace)
        payload.add(Parameters.TRACKER_VERSION, trackerVersion)
        subject?.let { payload.addMap(HashMap(it.getSubject(userAnonymisation))) }
        payload.add(Parameters.PLATFORM, platform.value)
    }

    private fun addPrimitivePropertiesToPayload(payload: Payload, event: TrackerEvent) {
        payload.add(Parameters.EVENT, event.name)
        payload.addMap(event.payload)
    }

    private fun addSelfDescribingPropertiesToPayload(payload: Payload, event: TrackerEvent) {
        payload.add(Parameters.EVENT, TrackerConstants.EVENT_UNSTRUCTURED)
        event.schema?.let {
            val data = SelfDescribingJson(it, event.payload)
            val unstructuredEventPayload = HashMap<String?, Any?>()
            unstructuredEventPayload[Parameters.SCHEMA] = TrackerConstants.SCHEMA_UNSTRUCT_EVENT
            unstructuredEventPayload[Parameters.DATA] = data.map
            payload.addMap(
                unstructuredEventPayload,
                base64Encoded,
                Parameters.UNSTRUCTURED_ENCODED,
                Parameters.UNSTRUCTURED
            )
        }
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
    private fun workaroundForCampaignAttributionEnrichment(
        payload: Payload,
        event: TrackerEvent,
        contexts: List<SelfDescribingJson?>?
    ) {
        var url: String? = null
        var referrer: String? = null
        if (event.schema == DeepLinkReceived.schema) {
            url = event.payload[DeepLinkReceived.PARAM_URL] as? String?
            referrer = event.payload[DeepLinkReceived.PARAM_REFERRER] as? String?
        } else if (event.schema == TrackerConstants.SCHEMA_SCREEN_VIEW && contexts != null) {
            for (entity in contexts) {
                if (entity is DeepLink) {
                    url = entity.url
                    referrer = entity.referrer
                    break
                }
            }
        }
        if (url != null) {
            payload.add(Parameters.PAGE_URL, url)
        }
        if (referrer != null) {
            payload.add(Parameters.PAGE_REFR, referrer)
        }
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
    private fun workaroundForIncoherentSessionContext(event: TrackerEvent) {
        if (!event.isService && sessionContext) {
            val eventId = event.eventId.toString()
            val eventTimestamp = event.timestamp
            val sessionManager = session
            if (sessionManager == null) {
                track(
                    TAG,
                    "Session not ready or method getHasLoadedFromFile returned false with eventId: %s",
                    eventId
                )
                return
            }
            val sessionContextJson =
                sessionManager.getSessionContext(eventId, eventTimestamp, userAnonymisation)
            sessionContextJson?.let { event.contexts.add(it) }
        }
    }

    private fun addBasicContextsToContexts(
        contexts: MutableList<SelfDescribingJson>,
        event: TrackerEvent
    ) {
        if (applicationContext) {
            getApplicationContext(context)?.let { contexts.add(it) }
        }
        if (platformContextEnabled) {
            platformContextManager.getMobileContext(userAnonymisation)?.let { contexts.add(it) }
        }
        if (event.isService) {
            return
        }
        if (geoLocationContext) {
            getGeoLocationContext(context)?.let { contexts.add(it) }
        }
        gdprContext?.let { contexts.add(it.context) }
    }

    private fun addGlobalContextsToContexts(
        contexts: MutableList<SelfDescribingJson>,
        event: InspectableEvent
    ) {
        synchronized(globalContextGenerators) {
            for (generator in globalContextGenerators.values) {
                contexts.addAll(generator.generateContexts(event))
            }
        }
    }

    private fun addStateMachineEntitiesToContexts(
        contexts: MutableList<SelfDescribingJson>,
        event: InspectableEvent
    ) {
        val stateManagerEntities = stateManager.entitiesForProcessedEvent(event)
        contexts.addAll(stateManagerEntities)
    }

    private fun wrapContextsToPayload(payload: Payload, contexts: List<SelfDescribingJson>) {
        if (contexts.isEmpty()) {
            return
        }
        
        val data: MutableList<Map<String, Any?>> = LinkedList()
        for (context in contexts) {
            data.add(context.map)
        }
        val finalContext = SelfDescribingJson(TrackerConstants.SCHEMA_CONTEXTS, data)
        payload.addMap(
            finalContext.map,
            base64Encoded,
            Parameters.CONTEXT_ENCODED,
            Parameters.CONTEXT
        )
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
     * decorated.
     * @param contexts The raw context list
     */
    private fun addServiceEventPayload(
        payload: Payload,
        contexts: MutableList<SelfDescribingJson?>
    ) {
        // Add default parameters to the payload
        payload.add(Parameters.PLATFORM, platform.value)
        payload.add(Parameters.APPID, appId)
        payload.add(Parameters.NAMESPACE, namespace)
        payload.add(Parameters.TRACKER_VERSION, trackerVersion)

        // If there is a subject present for the Tracker add it
        subject?.let { payload.addMap(HashMap(it.getSubject(userAnonymisation))) }

        // Add Mobile Context
        if (platformContextEnabled) {
            contexts.add(platformContextManager.getMobileContext(userAnonymisation))
        }

        // Add application context
        if (applicationContext) {
            contexts.add(getApplicationContext(context))
        }

        // If there are contexts to nest
        if (contexts.size > 0) {
            val contextMaps: MutableList<Map<*, *>> = LinkedList()
            for (selfDescribingJson in contexts) {
                if (selfDescribingJson != null) {
                    contextMaps.add(selfDescribingJson.map)
                }
            }
            val envelope = SelfDescribingJson(TrackerConstants.SCHEMA_CONTEXTS, contextMaps)
            payload.addMap(
                envelope.map, base64Encoded, Parameters.CONTEXT_ENCODED,
                Parameters.CONTEXT
            )
        }

        // Add this payload to the emitter
        emitter.add(payload)
    }
    
    // --- Controls
    
    /**
     * Starts event collection processes
     * again.
     */
    fun resumeEventTracking() {
        if (_dataCollection.compareAndSet(false, true)) {
            resumeSessionChecking()
            emitter.flush()
        }
    }

    /**
     * Stops event collection and ends all
     * concurrent processes.
     */
    fun pauseEventTracking() {
        if (_dataCollection.compareAndSet(true, false)) {
            pauseSessionChecking()
            emitter.shutdown()
        }
    }

    /**
     * Starts session checking.
     */
    fun resumeSessionChecking() {
        val trackerSession = session
        if (trackerSession != null) {
            trackerSession.setIsSuspended(false)
            d(TAG, "Session checking has been resumed.")
        }
    }

    /**
     * Ends session checking.
     */
    fun pauseSessionChecking() {
        val trackerSession = session
        if (trackerSession != null) {
            trackerSession.setIsSuspended(true)
            d(TAG, "Session checking has been paused.")
        }
    }

    /**
     * Convenience function for starting a new session.
     */
    fun startNewSession() {
        session?.startNewSession()
    }
    
    // --- GDPR context
    
    /**
     * Enables GDPR context to be sent with every event.
     * @param basisForProcessing GDPR Basis for processing
     * @param documentId ID of a GDPR basis document
     * @param documentVersion Version of the document
     * @param documentDescription Description of the document
     */
    fun enableGdprContext(
        basisForProcessing: Basis,
        documentId: String?,
        documentVersion: String?,
        documentDescription: String?
    ) {
        gdprContext = Gdpr(basisForProcessing, documentId, documentVersion, documentDescription)
    }

    /**
     * Disable GDPR context.
     */
    @Synchronized
    fun disableGdprContext() {
        gdprContext = null
    }

    // --- Global contexts
    
    fun setGlobalContextGenerators(globalContexts: Map<String, GlobalContext>) {
        synchronized(globalContextGenerators) {
            globalContextGenerators.clear()
            globalContextGenerators.putAll(globalContexts)
        }
    }

    fun addGlobalContext(generator: GlobalContext, tag: String): Boolean {
        synchronized(globalContextGenerators) {
            if (globalContextGenerators.containsKey(tag)) {
                return false
            }
            globalContextGenerators[tag] = generator
            return true
        }
    }

    fun removeGlobalContext(tag: String): GlobalContext? {
        synchronized(globalContextGenerators) { return globalContextGenerators.remove(tag) }
    }

    val globalContextTags: Set<String>
        get() = globalContextGenerators.keys

    companion object {
        private val TAG = Tracker::class.java.simpleName
    }
}
