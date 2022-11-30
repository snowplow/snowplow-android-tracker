package com.snowplowanalytics.snowplow.configuration

/**
 * State of retrieved remote configuration that states where the configuration was retrieved from.
 */
enum class ConfigurationState {
    /**
     * The default configuration was used.
     */
    DEFAULT,

    /**
     * The configuration was retrieved from local cache.
     */
    CACHED,

    /**
     * The configuration was retrieved from the remote configuration endpoint.
     */
    FETCHED
}
