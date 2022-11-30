package com.snowplowanalytics.snowplow.configuration

import java.io.Serializable

interface Configuration : Serializable {
    fun copy(): Configuration
}
