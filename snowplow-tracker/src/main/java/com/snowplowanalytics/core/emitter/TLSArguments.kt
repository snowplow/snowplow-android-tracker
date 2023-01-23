/*
 * Copyright (c) 2015-2022 Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.core.emitter

import androidx.annotation.RestrictTo
import java.security.KeyManagementException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Builds an object to store arguments to pass to TLS connection configuration.
 *
 * @param tlsVersions Accepted TLS versions for connections
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class TLSArguments(private val tlsVersions: EnumSet<TLSVersion>) {
    /**
     * @return the trust manager argument
     */
    var trustManager: X509TrustManager? = null

    /**
     * @return the ssl socket factory argument
     */
    var sslSocketFactory: SSLSocketFactory? = null

    
    init {
        try {
            val trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            )
            trustManagerFactory.init(null as? KeyStore?)
            val trustManagers = trustManagerFactory.trustManagers
            
            check(!(trustManagers.size != 1 || trustManagers[0] !is X509TrustManager)) {
                ("Unexpected default trust managers:"
                        + Arrays.toString(trustManagers))
            }
            
            trustManager = trustManagers[0] as? X509TrustManager
            sslSocketFactory = TLSSocketFactory(versions)
        } catch (e: KeyStoreException) {
            e.printStackTrace()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: KeyManagementException) {
            e.printStackTrace()
        }
    }

    /**
     * @return TLS versions
     */
    val versions: Array<String?>
        get() {
            val acceptedVersions = arrayOfNulls<String>(tlsVersions.size)
            for ((i, version) in tlsVersions.withIndex()) {
                acceptedVersions[i] = version.toString()
            }
            return acceptedVersions
        }
}
