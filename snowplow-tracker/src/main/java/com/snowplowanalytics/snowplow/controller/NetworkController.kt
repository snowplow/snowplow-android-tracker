/*
 * Copyright (c) 2015-present Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.snowplow.controller

import com.snowplowanalytics.snowplow.network.HttpMethod

/**
 * Controller for managing the network connection to the event collector.
 */
interface NetworkController {
    /**
     * URL used to send events to the collector.
     */
    var endpoint: String

    /**
     * Method used to send events to the collector.
     */
    var method: HttpMethod
    
    /**
     * A custom path which will be added to the endpoint URL to specify the
     * complete URL of the collector when paired with the POST method.
     */
    var customPostPath: String?
    
    /**
     * The timeout set for the requests to the collector.
     */
    var timeout: Int?
}
