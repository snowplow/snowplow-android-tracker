package com.snowplowanalytics.core.tracker

import android.content.Context
import androidx.annotation.RestrictTo
import com.snowplowanalytics.core.emitter.*
import com.snowplowanalytics.core.gdpr.GdprConfigurationUpdate
import com.snowplowanalytics.core.gdpr.GdprControllerImpl
import com.snowplowanalytics.core.globalcontexts.GlobalContextsControllerImpl
import com.snowplowanalytics.core.session.SessionConfigurationInterface
import com.snowplowanalytics.core.session.SessionConfigurationUpdate
import com.snowplowanalytics.core.session.SessionControllerImpl
import com.snowplowanalytics.core.tracker.Tracker.TrackerBuilder
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
        orMakeTracker() // Build tracker to initialize NotificationCenter receivers
    }

    fun reset(configurations: List<Configuration>) {
        stopServices()
        resetConfigurationUpdates()
        processConfigurations(configurations)
        resetServices()
        orMakeTracker()
    }

    fun shutdown() {
        if (tracker != null) {
            tracker!!.pauseEventTracking()
        }
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
        if (tracker != null) {
            tracker!!.close()
        }
        if (emitter != null) {
            emitter!!.shutdown()
        }
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
    override fun orMakeSubject(): Subject {
        if (subject == null) {
            subject = makeSubject()
        }
        return subject!!
    }

    override fun orMakeEmitter(): Emitter {
        if (emitter == null) {
            emitter = makeEmitter()
        }
        return emitter!!
    }
    

    override fun orMakeTracker(): Tracker {
        if (tracker == null) {
            tracker = makeTracker()
        }
        return tracker!!
    }

    override fun orMakeTrackerController(): TrackerControllerImpl {
        if (trackerController == null) {
            trackerController = makeTrackerController()
        }
        return trackerController!!
    }

    override fun orMakeSessionController(): SessionControllerImpl {
        if (sessionController == null) {
            sessionController = makeSessionController()
        }
        return sessionController!!
    }

    override fun orMakeEmitterController(): EmitterControllerImpl {
        if (emitterController == null) {
            emitterController = makeEmitterController()
        }
        return emitterController!!
    }

    override fun orMakeGdprController(): GdprControllerImpl {
        if (gdprController == null) {
            gdprController = makeGdprController()
        }
        return gdprController!!
    }

    override fun orMakeGlobalContextsController(): GlobalContextsControllerImpl {
        if (globalContextsController == null) {
            globalContextsController = makeGlobalContextsController()
        }
        return globalContextsController!!
    }

    override fun orMakeSubjectController(): SubjectControllerImpl {
        if (subjectController == null) {
            subjectController = makeSubjectController()
        }
        return subjectController!!
    }

    override fun orMakeNetworkController(): NetworkControllerImpl {
        if (networkController == null) {
            networkController = makeNetworkController()
        }
        return networkController!!
    }

    // Factories
    private fun makeSubject(): Subject {
        return Subject(context, subjectConfigurationUpdate)
    }

    private fun makeEmitter(): Emitter {
        val networkConfig: NetworkConfigurationInterface = networkConfigurationUpdate
        val emitterConfig: EmitterConfigurationInterface = emitterConfigurationUpdate
        val endpoint = networkConfig.endpoint ?: ""
        
        val builder = { emitter: Emitter ->
            networkConfig.method?.let { emitter.httpMethod = it }
            networkConfig.protocol?.let { emitter.requestSecurity = it }
            
            emitter.networkConnection = networkConfig.networkConnection
            emitter.customPostPath = networkConfig.customPostPath
            emitter.client = networkConfig.okHttpClient
            emitter.cookieJar = networkConfig.okHttpCookieJar
            emitter.sendLimit = emitterConfig.emitRange
            emitter.bufferOption = emitterConfig.bufferOption
            emitter.eventStore = emitterConfig.eventStore
            emitter.byteLimitPost = emitterConfig.byteLimitPost
            emitter.byteLimitGet = emitterConfig.byteLimitGet
            emitter.threadPoolSize = emitterConfig.threadPoolSize
            emitter.requestCallback = emitterConfig.requestCallback
            emitter.customRetryForStatusCodes = emitterConfig.customRetryForStatusCodes
            emitter.serverAnonymisation = emitterConfig.isServerAnonymisation
        }
        
        val emitter = Emitter(context, endpoint, builder)
        if (emitterConfigurationUpdate.isPaused) {
            emitter.pauseEmit()
        }
        return emitter
    }

    private fun makeTracker(): Tracker {
        val emitter = orMakeEmitter()
        val subject = orMakeSubject()
        val trackerConfig: TrackerConfigurationInterface = trackerConfigurationUpdate
        val sessionConfig: SessionConfigurationInterface = sessionConfigurationUpdate
        val builder = TrackerBuilder(emitter, namespace, trackerConfig.appId, context)
            .subject(subject)
            .trackerVersionSuffix(trackerConfig.trackerVersionSuffix)
            .base64(trackerConfig.base64encoding)
            .level(trackerConfig.logLevel)
            .loggerDelegate(trackerConfig.loggerDelegate)
            .platform(trackerConfig.devicePlatform)
            .sessionContext(trackerConfig.sessionContext)
            .applicationContext(trackerConfig.applicationContext)
            .mobileContext(trackerConfig.platformContext)
            .deepLinkContext(trackerConfig.deepLinkContext)
            .screenContext(trackerConfig.screenContext)
            .screenviewEvents(trackerConfig.screenViewAutotracking)
            .lifecycleEvents(trackerConfig.lifecycleAutotracking)
            .installTracking(trackerConfig.installAutotracking)
            .applicationCrash(trackerConfig.exceptionAutotracking)
            .trackerDiagnostic(trackerConfig.diagnosticAutotracking)
            .backgroundTimeout(sessionConfig.backgroundTimeout.convert(TimeUnit.SECONDS))
            .foregroundTimeout(sessionConfig.foregroundTimeout.convert(TimeUnit.SECONDS))
            .userAnonymisation(trackerConfig.userAnonymisation)
        val gdprConfig = gdprConfigurationUpdate
        if (gdprConfig.sourceConfig != null) {
            builder.gdprContext(
                gdprConfig.basisForProcessing(),
                gdprConfig.documentId(),
                gdprConfig.documentVersion(),
                gdprConfig.documentDescription()
            )
        }
        val tracker = Tracker(builder)
        if (globalContextsConfiguration != null) {
            tracker.setGlobalContextGenerators(globalContextsConfiguration!!.contextGenerators)
        }
        if (trackerConfigurationUpdate.isPaused) {
            tracker.pauseEventTracking()
        }
        if (sessionConfigurationUpdate.isPaused) {
            tracker.pauseSessionChecking()
        }
        val session = tracker.session
        if (session != null) {
            val onSessionUpdate = sessionConfigurationUpdate.onSessionUpdate()
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
        val gdpr = orMakeTracker().gdprContext
        if (gdpr != null) {
            controller.reset(
                gdpr.basisForProcessing,
                gdpr.documentId!!,
                gdpr.documentVersion!!,
                gdpr.documentDescription!!
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
