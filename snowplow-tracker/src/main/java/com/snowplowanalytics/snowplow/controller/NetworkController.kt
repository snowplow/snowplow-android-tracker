package com.snowplowanalytics.snowplow.controller

import com.snowplowanalytics.snowplow.network.HttpMethod

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
    var timeout: Int
}
