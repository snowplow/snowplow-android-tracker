package com.snowplowanalytics.core.tracker

import com.snowplowanalytics.core.emitter.*
import com.snowplowanalytics.core.gdpr.GdprConfigurationUpdate
import com.snowplowanalytics.core.gdpr.GdprControllerImpl
import com.snowplowanalytics.core.globalcontexts.GlobalContextsControllerImpl
import com.snowplowanalytics.core.session.SessionConfigurationUpdate
import com.snowplowanalytics.core.session.SessionControllerImpl
import com.snowplowanalytics.snowplow.configuration.PluginConfigurationInterface

interface ServiceProviderInterface {
    val namespace: String

    // Internal services
    val isTrackerInitialized: Boolean
    fun getOrMakeTracker(): Tracker
    fun getOrMakeEmitter(): Emitter
    fun getOrMakeSubject(): Subject

    // Controllers
    fun getOrMakeTrackerController(): TrackerControllerImpl
    fun getOrMakeEmitterController(): EmitterControllerImpl
    fun getOrMakeNetworkController(): NetworkControllerImpl
    fun getOrMakeGdprController(): GdprControllerImpl
    fun getOrMakeGlobalContextsController(): GlobalContextsControllerImpl
    fun getOrMakeSubjectController(): SubjectControllerImpl
    fun getOrMakeSessionController(): SessionControllerImpl
    val pluginsController: PluginsControllerImpl

    // Configuration Updates
    val trackerConfigurationUpdate: TrackerConfigurationUpdate
    val networkConfigurationUpdate: NetworkConfigurationUpdate
    val subjectConfigurationUpdate: SubjectConfigurationUpdate
    val emitterConfigurationUpdate: EmitterConfigurationUpdate
    val sessionConfigurationUpdate: SessionConfigurationUpdate
    val gdprConfigurationUpdate: GdprConfigurationUpdate

    // Plugins
    val pluginConfigurations: List<PluginConfigurationInterface>
    fun addPlugin(plugin: PluginConfigurationInterface)
    fun removePlugin(identifier: String)
}
