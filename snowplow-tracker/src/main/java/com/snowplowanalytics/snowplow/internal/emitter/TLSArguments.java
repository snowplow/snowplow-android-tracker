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

package com.snowplowanalytics.snowplow.internal.emitter;

import androidx.annotation.RestrictTo;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.EnumSet;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class TLSArguments {
    private X509TrustManager trustManager = null;
    private SSLSocketFactory sslSocketFactory = null;
    private EnumSet<TLSVersion> tlsVersions;

    /**
     * Builds an object to store arguments to pass to TLS connection configuration.
     *
     * @param versions Accepted TLS versions for connections
     */
    public TLSArguments(EnumSet<TLSVersion> versions) {
        this.tlsVersions = versions;
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                throw new IllegalStateException("Unexpected default trust managers:"
                        + Arrays.toString(trustManagers));
            }
            this.trustManager = (X509TrustManager) trustManagers[0];

            this.sslSocketFactory = new TLSSocketFactory(this.getVersions());
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return the trust manager argument
     */
    public X509TrustManager getTrustManager() {
        return this.trustManager;
    }

    /**
     * @return the ssl socket factory argument
     */
    public SSLSocketFactory getSslSocketFactory() {
        return this.sslSocketFactory;
    }

    /**
     * @return TLS versions
     */
    public String[] getVersions() {
        String[] acceptedVersions = new String[tlsVersions.size()];
        int i = 0;
        for (TLSVersion version : tlsVersions) {
            acceptedVersions[i] = version.toString();
            i++;
        }
        return acceptedVersions;
    }
}
