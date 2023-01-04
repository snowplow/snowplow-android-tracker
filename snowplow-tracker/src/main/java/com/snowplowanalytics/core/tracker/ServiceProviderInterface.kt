package com.snowplowanalytics.core.tracker

import com.snowplowanalytics.core.emitter.*
import com.snowplowanalytics.core.gdpr.GdprConfigurationUpdate
import com.snowplowanalytics.core.gdpr.GdprControllerImpl
import com.snowplowanalytics.core.globalcontexts.GlobalContextsControllerImpl
import com.snowplowanalytics.core.session.SessionConfigurationUpdate
import com.snowplowanalytics.core.session.SessionControllerImpl

interface ServiceProviderInterface {
    val namespace: String

    // Internal services
    val isTrackerInitialized: Boolean
    fun orMakeTracker(): Tracker
    fun orMakeEmitter(): Emitter
    fun orMakeSubject(): Subject

    // Controllers
    fun orMakeTrackerController(): TrackerControllerImpl
    fun orMakeEmitterController(): EmitterControllerImpl
    fun orMakeNetworkController(): NetworkControllerImpl
    fun orMakeGdprController(): GdprControllerImpl
    fun orMakeGlobalContextsController(): GlobalContextsControllerImpl
    fun orMakeSubjectController(): SubjectControllerImpl
    fun orMakeSessionController(): SessionControllerImpl

    // Configuration Updates
    val trackerConfigurationUpdate: TrackerConfigurationUpdate
    val networkConfigurationUpdate: NetworkConfigurationUpdate
    val subjectConfigurationUpdate: SubjectConfigurationUpdate
    val emitterConfigurationUpdate: EmitterConfigurationUpdate
    val sessionConfigurationUpdate: SessionConfigurationUpdate
    val gdprConfigurationUpdate: GdprConfigurationUpdate
}
