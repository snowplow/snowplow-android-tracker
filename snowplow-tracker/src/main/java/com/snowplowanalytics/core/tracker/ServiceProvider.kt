/*
 * Copyright (c) 2015-2023 Snowplow Analytics Ltd. All rights reserved.
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
import androidx.annotation.RestrictTo
import com.snowplowanalytics.core.ecommerce.EcommerceControllerImpl
import com.snowplowanalytics.core.emitter.*
import com.snowplowanalytics.core.gdpr.Gdpr
import com.snowplowanalytics.core.gdpr.GdprControllerImpl
import com.snowplowanalytics.core.globalcontexts.GlobalContextsControllerImpl
import com.snowplowanalytics.core.media.controller.MediaControllerImpl
import com.snowplowanalytics.core.session.SessionControllerImpl
import com.snowplowanalytics.snowplow.configuration.*
import com.snowplowanalytics.snowplow.media.controller.MediaController
import java.util.concurrent.TimeUnit

@RestrictTo(RestrictTo.Scope.LIBRARY)
class ServiceProvider(
    context: Context,
    override val namespace: String,
    networkConfiguration: NetworkConfiguration,
    configurations: List<Configuration>
) : ServiceProviderInterface {
    private val context: Context
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
    override val ecommerceController: EcommerceControllerImpl by lazy {
        EcommerceControllerImpl(this)
    }
    override val pluginsController: PluginsControllerImpl by lazy {
        PluginsControllerImpl(this)
    }
    override val mediaController: MediaController by lazy {
        MediaControllerImpl(this)
    }

    // Configurations
    override lateinit var trackerConfiguration: TrackerConfiguration
    override lateinit var networkConfiguration: NetworkConfiguration
    override lateinit var subjectConfiguration: SubjectConfiguration
    override lateinit var emitterConfiguration: EmitterConfiguration
    override lateinit var sessionConfiguration: SessionConfiguration
    override lateinit var gdprConfiguration: GdprConfiguration
    override var pluginConfigurations: MutableList<PluginIdentifiable> = ArrayList()
        private set

    init {
        // Initialization
        this.context = context

        // Reset configurationUpdates 
        trackerConfiguration = TrackerConfiguration()
        this.networkConfiguration = NetworkConfiguration()
        subjectConfiguration = SubjectConfiguration()
        emitterConfiguration = EmitterConfiguration()
        sessionConfiguration = SessionConfiguration()
        gdprConfiguration = GdprConfiguration()
        
        // Process configurations
        this.networkConfiguration.sourceConfig = networkConfiguration
        processConfigurations(configurations)

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
            when (configuration) {
                is NetworkConfiguration -> {
                    this.networkConfiguration.sourceConfig = configuration
                }
                is TrackerConfiguration -> {
                    trackerConfiguration.sourceConfig = configuration
                }
                is SubjectConfiguration -> {
                    subjectConfiguration.sourceConfig = configuration
                }
                is SessionConfiguration -> {
                    sessionConfiguration.sourceConfig = configuration
                }
                is EmitterConfiguration -> {
                    emitterConfiguration.sourceConfig = configuration
                }
                is GdprConfiguration -> {
                    gdprConfiguration.sourceConfig = configuration
                }
                is GlobalContextsConfiguration -> {
                    for (plugin in configuration.toPluginConfigurations()) {
                        pluginConfigurations.add(plugin)
                    }
                }
                is PluginIdentifiable -> {
                    pluginConfigurations.add(configuration)
                }
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
        subjectController = null
        networkController = null
    }

    private fun resetConfigurationUpdates() {
        // Don't reset networkConfiguration as it's needed in case it's not passed in the new configurations.
        // Set a default trackerConfiguration to reset to default if not passed.
        trackerConfiguration.sourceConfig = null
        subjectConfiguration.sourceConfig = null
        emitterConfiguration.sourceConfig = null
        sessionConfiguration.sourceConfig = null
        gdprConfiguration.sourceConfig = null
    }

    private fun initializeConfigurationUpdates() {
        this.networkConfiguration = NetworkConfiguration()
        trackerConfiguration = TrackerConfiguration()
        emitterConfiguration = EmitterConfiguration()
        subjectConfiguration = SubjectConfiguration()
        sessionConfiguration = SessionConfiguration()
        gdprConfiguration = GdprConfiguration()
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
        return GlobalContextsControllerImpl(this)
    }

    override fun getOrMakeSubjectController(): SubjectControllerImpl {
        return subjectController ?: makeSubjectController().also { subjectController = it }
    }

    override fun getOrMakeNetworkController(): NetworkControllerImpl {
        return networkController ?: makeNetworkController().also { networkController = it }
    }

    // Factories
    private fun makeSubject(): Subject {
        return Subject(context, subjectConfiguration)
    }

    private fun makeEmitter(): Emitter {
        val endpoint = networkConfiguration.endpoint ?: ""
        
        val builder = { emitter: Emitter ->
            emitter.httpMethod = networkConfiguration.method
            networkConfiguration.protocol?.let { emitter.requestSecurity = it }
            
            emitter.networkConnection = networkConfiguration.networkConnection
            emitter.customPostPath = networkConfiguration.customPostPath
            emitter.client = networkConfiguration.okHttpClient
            emitter.cookieJar = networkConfiguration.okHttpCookieJar
            emitter.emitTimeout = networkConfiguration.timeout
            emitter.emitRange = emitterConfiguration.emitRange
            emitter.bufferOption = emitterConfiguration.bufferOption
            emitter.eventStore = emitterConfiguration.eventStore
            emitter.byteLimitPost = emitterConfiguration.byteLimitPost
            emitter.byteLimitGet = emitterConfiguration.byteLimitGet
            emitter.threadPoolSize = emitterConfiguration.threadPoolSize
            emitter.requestCallback = emitterConfiguration.requestCallback
            emitter.customRetryForStatusCodes = emitterConfiguration.customRetryForStatusCodes
            emitter.serverAnonymisation = emitterConfiguration.serverAnonymisation
            emitter.requestHeaders = networkConfiguration.requestHeaders
            emitter.retryFailedRequests = emitterConfiguration.retryFailedRequests
        }
        
        val emitter = Emitter(context, endpoint, builder)
        if (emitterConfiguration.isPaused) {
            emitter.pauseEmit()
        }
        return emitter
    }

    private fun makeTracker(): Tracker {
        val emitter = getOrMakeEmitter()
        val subject = getOrMakeSubject()

        val builder = { tracker: Tracker ->
            tracker.subject = subject
            tracker.trackerVersionSuffix = trackerConfiguration.trackerVersionSuffix
            tracker.base64Encoded = trackerConfiguration.base64encoding
            tracker.platform = trackerConfiguration.devicePlatform
            tracker.logLevel = trackerConfiguration.logLevel
            tracker.loggerDelegate = trackerConfiguration.loggerDelegate
            tracker.sessionContext = trackerConfiguration.sessionContext
            tracker.applicationContext = trackerConfiguration.applicationContext
            tracker.platformContextEnabled = trackerConfiguration.platformContext
            tracker.geoLocationContext = trackerConfiguration.geoLocationContext
            tracker.deepLinkContext = trackerConfiguration.deepLinkContext
            tracker.screenContext = trackerConfiguration.screenContext
            tracker.screenViewAutotracking = trackerConfiguration.screenViewAutotracking
            tracker.lifecycleAutotracking = trackerConfiguration.lifecycleAutotracking
            tracker.installAutotracking = trackerConfiguration.installAutotracking
            tracker.exceptionAutotracking = trackerConfiguration.exceptionAutotracking
            tracker.diagnosticAutotracking = trackerConfiguration.diagnosticAutotracking
            tracker.userAnonymisation = trackerConfiguration.userAnonymisation
            tracker.trackerVersionSuffix = trackerConfiguration.trackerVersionSuffix

            gdprConfiguration.sourceConfig?.let { tracker.gdprContext = Gdpr(
                basisForProcessing = it.basisForProcessing,
                documentId = it.documentId,
                documentVersion = it.documentVersion,
                documentDescription = it.documentDescription) }

            tracker.backgroundTimeout = sessionConfiguration.backgroundTimeout.convert(TimeUnit.SECONDS)
            tracker.foregroundTimeout = sessionConfiguration.foregroundTimeout.convert(TimeUnit.SECONDS)

            for (plugin in pluginConfigurations) {
                tracker.addOrReplaceStateMachine(plugin.toStateMachine())
            }
        }
        
        val tracker = Tracker(
            emitter,
            namespace,
            trackerConfiguration.appId,
            trackerConfiguration.platformContextProperties,
            context,
            builder
        )
        
        if (trackerConfiguration.isPaused) {
            tracker.pauseEventTracking()
        }
        if (sessionConfiguration.isPaused) {
            tracker.pauseSessionChecking()
        }
        val session = tracker.session
        if (session != null) {
            val onSessionUpdate = sessionConfiguration.onSessionUpdate
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

    private fun makeSubjectController(): SubjectControllerImpl {
        return SubjectControllerImpl(this)
    }

    private fun makeNetworkController(): NetworkControllerImpl {
        return NetworkControllerImpl(this)
    }

    // Plugins

    override fun addPlugin(plugin: PluginIdentifiable) {
        removePlugin(plugin.identifier)
        pluginConfigurations.add(plugin)
        tracker?.addOrReplaceStateMachine(plugin.toStateMachine())
    }

    override fun removePlugin(identifier: String) {
        pluginConfigurations.removeAll {
            it.identifier == identifier
        }
        tracker?.removeStateMachine(identifier)
    }
}
