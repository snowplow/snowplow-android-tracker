package com.snowplowanalytics.core.remoteconfiguration

import android.content.Context
import com.snowplowanalytics.snowplow.configuration.RemoteConfiguration
import java.io.*

class ConfigurationCache(private val remoteConfiguration: RemoteConfiguration) {
    private var cacheFilePath: String? = null
    private var configuration: FetchedConfigurationBundle? = null
    
    @Synchronized
    fun readCache(context: Context): FetchedConfigurationBundle? {
        if (configuration != null) { return configuration }
        
        loadCache(context)
        return configuration
    }

    @Synchronized
    fun writeCache(context: Context, configuration: FetchedConfigurationBundle) {
        this.configuration = configuration
        storeCache(context, configuration)
    }

    @Synchronized
    fun clearCache(context: Context) {
        val path = getCachePath(context)
        val file = File(path)
        file.delete()
    }

    // Private methods
    private fun getCachePath(context: Context): String {
        cacheFilePath?.let { return it }
        
        val cacheDirPath = context.cacheDir.absolutePath + File.separator + "snowplow-cache"
        val cacheDir = File(cacheDirPath)
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val fileName = "remoteConfig-" + remoteConfiguration.endpoint.hashCode() + ".data"
        cacheFilePath = cacheDir.absolutePath + File.separator + fileName
        return cacheFilePath!!
    }

    private fun loadCache(context: Context) {
        val path = getCachePath(context)
        var objectIn: ObjectInputStream? = null
        try {
            val fileIn = FileInputStream(path)
            objectIn = ObjectInputStream(fileIn)
            configuration = objectIn.readObject() as? FetchedConfigurationBundle
        } catch (e: FileNotFoundException) {
            // TODO log exception
        } catch (e: IOException) {
            // TODO log exception
        } catch (e: ClassNotFoundException) {
            // TODO log exception
        } finally {
            if (objectIn != null) {
                try {
                    objectIn.close()
                } catch (e: IOException) {
                }
            }
        }
    }

    private fun storeCache(context: Context, configuration: FetchedConfigurationBundle) {
        val path = getCachePath(context)
        var objectOut: ObjectOutputStream? = null
        try {
            val fileOut = FileOutputStream(path, false)
            objectOut = ObjectOutputStream(fileOut)
            objectOut.writeObject(configuration)
            fileOut.fd.sync()
        } catch (e: IOException) {
            // TODO log exception
        } finally {
            if (objectOut != null) {
                try {
                    objectOut.close()
                } catch (e: IOException) {
                }
            }
        }
    }
}
