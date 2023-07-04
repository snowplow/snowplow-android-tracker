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
package com.snowplowanalytics.core.remoteconfiguration

import android.content.Context
import com.snowplowanalytics.snowplow.configuration.RemoteConfiguration
import java.io.*

class RemoteConfigurationCache(private val remoteConfiguration: RemoteConfiguration) {
    private var cacheFilePath: String? = null
    private var configuration: RemoteConfigurationBundle? = null
    
    @Synchronized
    fun readCache(context: Context): RemoteConfigurationBundle? {
        if (configuration != null) { return configuration }
        
        loadCache(context)
        return configuration
    }

    @Synchronized
    fun writeCache(context: Context, configuration: RemoteConfigurationBundle) {
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
            configuration = objectIn.readObject() as? RemoteConfigurationBundle
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

    private fun storeCache(context: Context, configuration: RemoteConfigurationBundle) {
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
