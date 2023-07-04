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

import androidx.annotation.RestrictTo
import com.snowplowanalytics.core.Controller
import com.snowplowanalytics.core.ecommerce.EcommerceControllerImpl
import com.snowplowanalytics.core.session.SessionControllerImpl
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration
import com.snowplowanalytics.snowplow.controller.*
import com.snowplowanalytics.snowplow.event.Event
import com.snowplowanalytics.snowplow.media.controller.MediaController
import com.snowplowanalytics.snowplow.tracker.BuildConfig
import com.snowplowanalytics.snowplow.tracker.DevicePlatform
import com.snowplowanalytics.snowplow.tracker.LogLevel
import com.snowplowanalytics.snowplow.tracker.LoggerDelegate
import java.util.*

@RestrictTo(RestrictTo.Scope.LIBRARY)
class TrackerControllerImpl  // Constructors
    (serviceProvider: ServiceProvider) : Controller(serviceProvider), TrackerController {
    // Sub-controllers
    override val network: NetworkController
        get() = serviceProvider.getOrMakeNetworkController()
    override val emitter: EmitterController
        get() = serviceProvider.getOrMakeEmitterController()
    override val subject: SubjectController
        get() = serviceProvider.getOrMakeSubjectController()
    override val gdpr: GdprController
        get() = serviceProvider.getOrMakeGdprController()
    override val globalContexts: GlobalContextsController
        get() = serviceProvider.getOrMakeGlobalContextsController()
    val sessionController: SessionControllerImpl
        get() = serviceProvider.getOrMakeSessionController()
    
    override val ecommerce: EcommerceControllerImpl
        get() = serviceProvider.ecommerceController
    override val session: SessionController?
        get() {
            val sessionController = sessionController
            return if (sessionController.isEnabled) sessionController else null
        }
    override val plugins: PluginsController
        get() = serviceProvider.pluginsController
    override val media: MediaController
        get() = serviceProvider.mediaController

    // Control methods
    override fun pause() {
        dirtyConfig.isPaused = true
        tracker.pauseEventTracking()
    }

    override fun resume() {
        dirtyConfig.isPaused = false
        tracker.resumeEventTracking()
    }

    override fun track(event: Event): UUID? {
        return tracker.track(event)
    }

    override val version: String
        get() = BuildConfig.TRACKER_LABEL
    override val isTracking: Boolean
        get() = tracker.dataCollection

    // Getters and Setters
    override val namespace: String
        get() = tracker.namespace
    
    override var appId: String
        get() = tracker.appId
        set(appId) {
            dirtyConfig.appId = appId
            tracker.appId = appId
        }
    
    override var devicePlatform: DevicePlatform
        get() = tracker.platform
        set(devicePlatform) {
            dirtyConfig.devicePlatform = devicePlatform
            tracker.platform = devicePlatform
        }
    
    override var base64encoding: Boolean
        get() = tracker.base64Encoded
        set(base64encoding) {
            dirtyConfig.base64encoding = base64encoding
            tracker.base64Encoded = base64encoding
        }
    
    override var logLevel: LogLevel
        get() = tracker.logLevel
        set(logLevel) {
            dirtyConfig.logLevel = logLevel
            tracker.logLevel = logLevel
        }
    
    override var loggerDelegate: LoggerDelegate?
        get() = Logger.delegate
        set(loggerDelegate) {
            dirtyConfig.loggerDelegate = loggerDelegate
            Logger.delegate = loggerDelegate
        }
    
    override var applicationContext: Boolean
        get() = tracker.applicationContext
        set(applicationContext) {
            dirtyConfig.applicationContext = applicationContext
            tracker.applicationContext = applicationContext
        }
    
    override var platformContext: Boolean
        get() = tracker.platformContextEnabled
        set(platformContext) {
            dirtyConfig.platformContext = platformContext
            tracker.platformContextEnabled = platformContext
        }
    
    override var geoLocationContext: Boolean
        get() = tracker.geoLocationContext
        set(geoLocationContext) {
            dirtyConfig.geoLocationContext = geoLocationContext
            tracker.geoLocationContext = geoLocationContext
        }
    
    override var sessionContext: Boolean
        get() = tracker.sessionContext
        set(sessionContext) {
            dirtyConfig.sessionContext = sessionContext
            tracker.sessionContext = sessionContext
        }
    
    override var deepLinkContext: Boolean
        get() = tracker.deepLinkContext
        set(deepLinkContext) {
            dirtyConfig.deepLinkContext = deepLinkContext
            tracker.deepLinkContext = deepLinkContext
        }
    
    override var screenContext: Boolean
        get() = tracker.screenContext
        set(screenContext) {
            dirtyConfig.screenContext = screenContext
            tracker.screenContext = screenContext
        }
    
    override var screenViewAutotracking: Boolean
        get() = tracker.screenViewAutotracking
        set(screenViewAutotracking) {
            dirtyConfig.screenViewAutotracking = screenViewAutotracking
            tracker.screenViewAutotracking = screenViewAutotracking
        }
    
    override var lifecycleAutotracking: Boolean
        get() = tracker.lifecycleAutotracking
        set(lifecycleAutotracking) {
            dirtyConfig.lifecycleAutotracking = lifecycleAutotracking
            tracker.lifecycleAutotracking = lifecycleAutotracking
        }
    
    override var installAutotracking: Boolean
        get() = tracker.installAutotracking
        set(installAutotracking) {
            dirtyConfig.installAutotracking = installAutotracking
            tracker.installAutotracking = installAutotracking
        }
    
    override var exceptionAutotracking: Boolean
        get() = tracker.exceptionAutotracking
        set(exceptionAutotracking) {
            dirtyConfig.exceptionAutotracking = exceptionAutotracking
            tracker.exceptionAutotracking = exceptionAutotracking
        }
    
    override var diagnosticAutotracking: Boolean
        get() = tracker.diagnosticAutotracking
        set(diagnosticAutotracking) {
            dirtyConfig.diagnosticAutotracking = diagnosticAutotracking
            tracker.diagnosticAutotracking = diagnosticAutotracking
        }
    
    override var userAnonymisation: Boolean
        get() = tracker.userAnonymisation
        set(userAnonymisation) {
            dirtyConfig.userAnonymisation = userAnonymisation
            tracker.userAnonymisation = userAnonymisation
        }

    // The trackerVersionSuffix shouldn't be updated.
    override var trackerVersionSuffix: String?
        get() = tracker.trackerVersionSuffix
        set(trackerVersionSuffix) {
            // The trackerVersionSuffix shouldn't be updated.
        }

    // Private methods
    private val tracker: Tracker
        get() {
            if (!serviceProvider.isTrackerInitialized) {
                loggerDelegate?.error(
                    TAG,
                    "Recreating tracker instance after it was removed. This will not be supported in future versions."
                )
            }
            return serviceProvider.getOrMakeTracker()
        }
    private val dirtyConfig: TrackerConfiguration
        get() = serviceProvider.trackerConfiguration

    companion object {
        private val TAG = TrackerControllerImpl::class.java.simpleName
    }
}
