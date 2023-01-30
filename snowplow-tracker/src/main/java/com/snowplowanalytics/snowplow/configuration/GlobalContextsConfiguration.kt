package com.snowplowanalytics.snowplow.configuration

import com.snowplowanalytics.core.globalcontexts.GlobalContextPluginConfiguration
import com.snowplowanalytics.core.globalcontexts.GlobalContextsConfigurationInterface
import com.snowplowanalytics.snowplow.globalcontexts.GlobalContext

/**
 * This class allows the setup of Global Contexts which are attached to selected events.
 */
class GlobalContextsConfiguration(contextGenerators: MutableMap<String, GlobalContext>?) :
    Configuration, GlobalContextsConfigurationInterface {
    
    val contextGenerators: MutableMap<String, GlobalContext>

    /**
     * Set of tags associated with added GlobalContexts.
     */
    override val tags: Set<String?>
        get() = contextGenerators.keys
    
    // Constructors
    
    /**
     * Allows for the creation of a map of tags and associated [GlobalContext] generators.
     * They are used by the tracker based on the filter settings defined on each [GlobalContext].
     * @param contextGenerators Map of Global Contexts generators.
     */
    init {
        this.contextGenerators = contextGenerators ?: HashMap()
    }
    
    // Methods
    

    /**
     * Add a GlobalContext generator to the configuration of the tracker.
     * @param tag The label identifying the generator in the tracker.
     * @param contextGenerator The GlobalContext generator.
     * @return Whether the adding operation has succeeded.
     */
    override fun add(tag: String, contextGenerator: GlobalContext): Boolean {
        contextGenerators[tag]?.let { return false }

        contextGenerators[tag] = contextGenerator
        return true
    }

    /**
     * Remove a GlobalContext generator from the configuration of the tracker.
     * @param tag The label identifying the generator in the tracker.
     * @return Whether the removing operation has succeded.
     */
    override fun remove(tag: String): GlobalContext? {
        return contextGenerators.remove(tag)
    }

    // Copyable
    override fun copy(): GlobalContextsConfiguration {
        return GlobalContextsConfiguration(contextGenerators)
    }

    internal fun toPluginConfigurations(): List<GlobalContextPluginConfiguration> {
        return contextGenerators.map {
            GlobalContextPluginConfiguration(
                identifier = it.key,
                globalContext = it.value
            )
        }
    }
}
