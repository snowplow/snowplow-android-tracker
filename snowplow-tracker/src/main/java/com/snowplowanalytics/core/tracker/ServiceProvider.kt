package com.snowplowanalytics.core.tracker

import android.content.Context
import androidx.annotation.RestrictTo
import com.snowplowanalytics.core.emitter.*
import com.snowplowanalytics.core.gdpr.Gdpr
import com.snowplowanalytics.core.gdpr.GdprConfigurationUpdate
import com.snowplowanalytics.core.gdpr.GdprControllerImpl
import com.snowplowanalytics.core.globalcontexts.GlobalContextsControllerImpl
import com.snowplowanalytics.core.session.SessionConfigurationUpdate
import com.snowplowanalytics.core.session.SessionControllerImpl
import com.snowplowanalytics.snowplow.configuration.*
import java.util.concurrent.TimeUnit

@RestrictTo(RestrictTo.Scope.LIBRARY)
class ServiceProvider(
    context: Context,
    override val namespace: String,
    networkConfiguration: NetworkConfiguration,
    configurations: List<Configuration>
) : ServiceProviderInterface {
    private val context: Context
    private val appId: String
    override val isTrackerInitialized: Boolean
        get() = tracker != null

    // Internal services
    private var tracker: Tracker? = null
    private var emitter: Emitter? = null
    private var subject: Subject? = null

    // Controllers
    private var trackerController: TrackerControllerImpl? = null
    private var emitterController: EmitterControllerImpl? = null
    private var networkController: NetworkControllerImpl? = null
    private var subjectController: SubjectControllerImpl? = null
    private var sessionController: SessionControllerImpl? = null
    private var gdprController: GdprControllerImpl? = null
    private var globalContextsController: GlobalContextsControllerImpl? = null

    // Original configurations
    private val trackerConfiguration: TrackerConfiguration
    private val emitterConfiguration: EmitterConfiguration? = null
    private val subjectConfiguration: SubjectConfiguration? = null
    private val sessionConfiguration: SessionConfiguration? = null
    private val gdprConfiguration: GdprConfiguration? = null
    private var globalContextsConfiguration: GlobalContextsConfiguration? = null

    // Configuration updates
    override lateinit var trackerConfigurationUpdate: TrackerConfigurationUpdate
    override lateinit var networkConfigurationUpdate: NetworkConfigurationUpdate
    override lateinit var subjectConfigurationUpdate: SubjectConfigurationUpdate
    override lateinit var emitterConfigurationUpdate: EmitterConfigurationUpdate
    override lateinit var sessionConfigurationUpdate: SessionConfigurationUpdate
    override lateinit var gdprConfigurationUpdate: GdprConfigurationUpdate

    init {
        // Initialization
        this.context = context
        appId = context.packageName
        
        // Reset configurationUpdates 
        trackerConfigurationUpdate = TrackerConfigurationUpdate(appId)
        networkConfigurationUpdate = NetworkConfigurationUpdate()
        subjectConfigurationUpdate = SubjectConfigurationUpdate()
        emitterConfigurationUpdate = EmitterConfigurationUpdate()
        sessionConfigurationUpdate = SessionConfigurationUpdate()
        gdprConfigurationUpdate = GdprConfigurationUpdate()
        
        // Process configurations
        networkConfigurationUpdate.sourceConfig = networkConfiguration
        trackerConfiguration = TrackerConfiguration(appId)
        processConfigurations(configurations)
        
        if (trackerConfigurationUpdate.sourceConfig == null) {
            trackerConfigurationUpdate.sourceConfig = TrackerConfiguration(appId)
        }
        getOrMakeTracker() // Build tracker to initialize NotificationCenter receivers
    }

    fun reset(configurations: List<Configuration>) {
        stopServices()
        resetConfigurationUpdates()
        processConfigurations(configurations)
        resetServices()
        getOrMakeTracker()
    }

    fun shutdown() {
        tracker?.pauseEventTracking()
        stopServices()
        resetServices()
        resetControllers()
        initializeConfigurationUpdates()
    }

    // Private methods
    private fun processConfigurations(configurations: List<Configuration>) {
        for (configuration in configurations) {
            if (configuration is NetworkConfiguration) {
                networkConfigurationUpdate.sourceConfig = configuration
                continue
            }
            if (configuration is TrackerConfiguration) {
                trackerConfigurationUpdate.sourceConfig = configuration
                continue
            }
            if (configuration is SubjectConfiguration) {
                subjectConfigurationUpdate.sourceConfig = configuration
                continue
            }
            if (configuration is SessionConfiguration) {
                sessionConfigurationUpdate.sourceConfig = configuration
                continue
            }
            if (configuration is EmitterConfiguration) {
                emitterConfigurationUpdate.sourceConfig = configuration
                continue
            }
            if (configuration is GdprConfiguration) {
                gdprConfigurationUpdate.sourceConfig = configuration
                continue
            }
            if (configuration is GlobalContextsConfiguration) {
                globalContextsConfiguration = configuration
                continue
            }
        }
    }

    private fun stopServices() {
        tracker?.close()
        emitter?.shutdown()
    }

    private fun resetServices() {
        emitter = null
        subject = null
        tracker = null
    }

    private fun resetControllers() {
        trackerController = null
        sessionController = null
        emitterController = null
        gdprController = null
        globalContextsController = null
        subjectController = null
        networkController = null
    }

    private fun resetConfigurationUpdates() {
        // Don't reset networkConfiguration as it's needed in case it's not passed in the new configurations.
        // Set a default trackerConfiguration to reset to default if not passed.
        trackerConfigurationUpdate.sourceConfig = TrackerConfiguration(appId)
        subjectConfigurationUpdate.sourceConfig = null
        emitterConfigurationUpdate.sourceConfig = null
        sessionConfigurationUpdate.sourceConfig = null
        gdprConfigurationUpdate.sourceConfig = null
    }

    private fun initializeConfigurationUpdates() {
        networkConfigurationUpdate = NetworkConfigurationUpdate()
        trackerConfigurationUpdate = TrackerConfigurationUpdate(appId)
        emitterConfigurationUpdate = EmitterConfigurationUpdate()
        subjectConfigurationUpdate = SubjectConfigurationUpdate()
        sessionConfigurationUpdate = SessionConfigurationUpdate()
        gdprConfigurationUpdate = GdprConfigurationUpdate()
    }

    // Getters
    override fun getOrMakeSubject(): Subject {
        return subject ?: makeSubject().also { subject = it }
    }

    override fun getOrMakeEmitter(): Emitter {
        return emitter ?: makeEmitter().also { emitter = it }
    }
    
    override fun getOrMakeTracker(): Tracker {
        return tracker ?: makeTracker().also { tracker = it }
    }

    override fun getOrMakeTrackerController(): TrackerControllerImpl {
        return trackerController ?: makeTrackerController().also { trackerController = it }
    }

    override fun getOrMakeSessionController(): SessionControllerImpl {
        return sessionController ?: makeSessionController().also { sessionController = it }
    }

    override fun getOrMakeEmitterController(): EmitterControllerImpl {
        return emitterController ?: makeEmitterController().also { emitterController = it }
    }

    override fun getOrMakeGdprController(): GdprControllerImpl {
        return gdprController ?: makeGdprController().also { gdprController = it }
    }

    override fun getOrMakeGlobalContextsController(): GlobalContextsControllerImpl {
        return globalContextsController ?: makeGlobalContextsController().also { globalContextsController = it }
    }

    override fun getOrMakeSubjectController(): SubjectControllerImpl {
        return subjectController ?: makeSubjectController().also { subjectController = it }
    }

    override fun getOrMakeNetworkController(): NetworkControllerImpl {
        return networkController ?: makeNetworkController().also { networkController = it }
    }

    // Factories
    private fun makeSubject(): Subject {
        return Subject(context, subjectConfigurationUpdate)
    }

    private fun makeEmitter(): Emitter {
        val networkConfig = networkConfigurationUpdate
        val emitterConfig = emitterConfigurationUpdate
        val endpoint = networkConfig.endpoint ?: ""
        
        val builder = { emitter: Emitter ->
            networkConfig.method?.let { emitter.httpMethod = it }
            networkConfig.protocol?.let { emitter.requestSecurity = it }
            
            emitter.networkConnection = networkConfig.networkConnection
            emitter.customPostPath = networkConfig.customPostPath
            emitter.client = networkConfig.okHttpClient
            emitter.cookieJar = networkConfig.okHttpCookieJar
            emitter.emitTimeout = networkConfig.timeout
            emitter.sendLimit = emitterConfig.emitRange
            emitter.bufferOption = emitterConfig.bufferOption
            emitter.eventStore = emitterConfig.eventStore
            emitter.byteLimitPost = emitterConfig.byteLimitPost
            emitter.byteLimitGet = emitterConfig.byteLimitGet
            emitter.threadPoolSize = emitterConfig.threadPoolSize
            emitter.requestCallback = emitterConfig.requestCallback
            emitter.customRetryForStatusCodes = emitterConfig.customRetryForStatusCodes
            emitter.serverAnonymisation = emitterConfig.serverAnonymisation
        }
        
        val emitter = Emitter(context, endpoint, builder)
        if (emitterConfig.isPaused) {
            emitter.pauseEmit()
        }
        return emitter
    }

    private fun makeTracker(): Tracker {
        val emitter = getOrMakeEmitter()
        val subject = getOrMakeSubject()
        val trackerConfig = trackerConfigurationUpdate
        val sessionConfig = sessionConfigurationUpdate
        val gdprConfig = gdprConfigurationUpdate

        val builder = { tracker: Tracker ->
            tracker.subject = subject
            tracker.trackerVersionSuffix = trackerConfig.trackerVersionSuffix
            tracker.base64Encoded = trackerConfig.base64encoding
            tracker.platform = trackerConfig.devicePlatform
            tracker.logLevel = trackerConfig.logLevel
            tracker.loggerDelegate = trackerConfig.loggerDelegate
            tracker.sessionContext = trackerConfig.sessionContext
            tracker.applicationContext = trackerConfig.applicationContext
            tracker.platformContextEnabled = trackerConfig.platformContext
            tracker.geoLocationContext = trackerConfig.geoLocationContext
            tracker.deepLinkContext = trackerConfig.deepLinkContext
            tracker.screenContext = trackerConfig.screenContext
            tracker.screenViewAutotracking = trackerConfig.screenViewAutotracking
            tracker.lifecycleAutotracking = trackerConfig.lifecycleAutotracking
            tracker.installAutotracking = trackerConfigurationUpdate.installAutotracking
            tracker.exceptionAutotracking = trackerConfig.exceptionAutotracking
            tracker.diagnosticAutotracking = trackerConfig.diagnosticAutotracking
            tracker.userAnonymisation = trackerConfig.userAnonymisation
            tracker.trackerVersionSuffix = trackerConfig.trackerVersionSuffix

            gdprConfig.sourceConfig?.let { tracker.gdprContext = Gdpr(
                gdprConfig.basisForProcessing,
                gdprConfig.documentId,
                gdprConfig.documentVersion,
                gdprConfig.documentDescription) }

            tracker.backgroundTimeout = sessionConfig.backgroundTimeout.convert(TimeUnit.SECONDS)
            tracker.foregroundTimeout = sessionConfig.foregroundTimeout.convert(TimeUnit.SECONDS)
        }
        
        val tracker = Tracker(emitter, namespace, trackerConfig.appId, context, builder)
        
        globalContextsConfiguration?.let { tracker.setGlobalContextGenerators(it.contextGenerators) }
        if (trackerConfigurationUpdate.isPaused) {
            tracker.pauseEventTracking()
        }
        if (sessionConfigurationUpdate.isPaused) {
            tracker.pauseSessionChecking()
        }
        val session = tracker.session
        if (session != null) {
            val onSessionUpdate = sessionConfig.onSessionUpdate
            if (onSessionUpdate != null) {
                session.onSessionUpdate = onSessionUpdate
            }
        }
        return tracker
    }

    private fun makeTrackerController(): TrackerControllerImpl {
        return TrackerControllerImpl(this)
    }

    private fun makeSessionController(): SessionControllerImpl {
        return SessionControllerImpl(this)
    }

    private fun makeEmitterController(): EmitterControllerImpl {
        return EmitterControllerImpl(this)
    }

    private fun makeGdprController(): GdprControllerImpl {
        val controller = GdprControllerImpl(this)
        val gdpr = getOrMakeTracker().gdprContext
        gdpr?.let {
            controller.reset(
                it.basisForProcessing,
                it.documentId,
                it.documentVersion,
                it.documentDescription
            )
        }
        return controller
    }

    private fun makeGlobalContextsController(): GlobalContextsControllerImpl {
        return GlobalContextsControllerImpl(this)
    }

    private fun makeSubjectController(): SubjectControllerImpl {
        return SubjectControllerImpl(this)
    }

    private fun makeNetworkController(): NetworkControllerImpl {
        return NetworkControllerImpl(this)
    }
}
