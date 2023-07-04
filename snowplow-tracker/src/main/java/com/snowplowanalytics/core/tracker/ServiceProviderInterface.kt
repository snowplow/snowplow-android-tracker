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

import com.snowplowanalytics.core.ecommerce.EcommerceControllerImpl
import com.snowplowanalytics.core.emitter.*
import com.snowplowanalytics.core.gdpr.GdprControllerImpl
import com.snowplowanalytics.core.globalcontexts.GlobalContextsControllerImpl
import com.snowplowanalytics.core.session.SessionControllerImpl
import com.snowplowanalytics.snowplow.configuration.PluginIdentifiable
import com.snowplowanalytics.snowplow.media.controller.MediaController
import com.snowplowanalytics.snowplow.configuration.*

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
    val mediaController: MediaController
    val ecommerceController: EcommerceControllerImpl

    // Configuration Updates
    val trackerConfiguration: TrackerConfiguration
    val networkConfiguration: NetworkConfiguration
    val subjectConfiguration: SubjectConfiguration
    val emitterConfiguration: EmitterConfiguration
    val sessionConfiguration: SessionConfiguration
    val gdprConfiguration: GdprConfiguration

    // Plugins
    val pluginConfigurations: List<PluginIdentifiable>
    fun addPlugin(plugin: PluginIdentifiable)
    fun removePlugin(identifier: String)
}
