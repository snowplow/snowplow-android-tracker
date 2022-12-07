package com.snowplowanalytics.core.globalcontexts

import com.snowplowanalytics.snowplow.globalcontexts.GlobalContext

interface GlobalContextsConfigurationInterface {
    /**
     * @return Set of tags associated to added GlobalContexts.
     */
    val tags: Set<String?>

    /**
     * Add a GlobalContext generator to the configuration of the tracker.
     * @param tag The label identifying the generator in the tracker.
     * @param contextGenerator The GlobalContext generator.
     * @return Whether the adding operation has succeeded.
     */
    fun add(tag: String, contextGenerator: GlobalContext): Boolean

    /**
     * Remove a GlobalContext generator from the configuration of the tracker.
     * @param tag The label identifying the generator in the tracker.
     * @return Whether the removing operation has succeeded.
     */
    fun remove(tag: String): GlobalContext?
}
