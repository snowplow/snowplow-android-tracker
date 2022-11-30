package com.snowplowanalytics.snowplow.network

import android.content.Context
import android.net.TrafficStats
import android.net.Uri
import android.os.Build

import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.core.emitter.Executor
import com.snowplowanalytics.core.emitter.TLSArguments
import com.snowplowanalytics.core.emitter.TLSVersion
import com.snowplowanalytics.core.tracker.Logger
import com.snowplowanalytics.snowplow.tracker.BuildConfig

import okhttp3.CookieJar
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody

import java.io.IOException
import java.util.*
import java.util.concurrent.*

/**
 * Components in charge to send events to the collector.
 * It uses OkHttp as Http client.
 */
class OkHttpNetworkConnection private constructor(builder: OkHttpNetworkConnectionBuilder) :
    NetworkConnection {
    private val TAG = OkHttpNetworkConnection::class.java.simpleName
    private val JSON = TrackerConstants.POST_CONTENT_TYPE.toMediaTypeOrNull()
    
    private val networkUri: String
    private val protocol: Protocol
    override val httpMethod: HttpMethod
    private val emitTimeout: Int
    private val customPostPath: String?
    private val serverAnonymisation: Boolean
    private var client: OkHttpClient? = null
    private val uriBuilder: Uri.Builder
    
    /**
     * Builder for the OkHttpNetworkConnection.
     * @param uri The uri of the collector
     */
    class OkHttpNetworkConnectionBuilder(
        val uri: String,
        val context: Context
    ) {
        var httpMethod = HttpMethod.POST // Optional
        var tlsVersions = EnumSet.of(TLSVersion.TLSv1_2) // Optional
        var emitTimeout = 5 // Optional
        var client: OkHttpClient? = null // Optional
        var cookieJar: CookieJar? = null // Optional
        var customPostPath: String? = null //Optional
        var serverAnonymisation = false // Optional

        /**
         * @param httpMethod The method by which requests are emitted
         * @return itself
         */
        fun method(httpMethod: HttpMethod): OkHttpNetworkConnectionBuilder {
            this.httpMethod = httpMethod
            return this
        }

        /**
         * @param version the TLS version allowed for requests
         * @return itself
         */
        fun tls(version: TLSVersion): OkHttpNetworkConnectionBuilder {
            tlsVersions = EnumSet.of(version)
            return this
        }

        /**
         * @param versions the TLS versions allowed for requests
         * @return itself
         */
        fun tls(versions: EnumSet<TLSVersion>): OkHttpNetworkConnectionBuilder {
            tlsVersions = versions
            return this
        }

        /**
         * @param emitTimeout The maximum timeout for emitting events. If emit time exceeds this value
         * TimeOutException will be thrown
         * @return itself
         */
        fun emitTimeout(emitTimeout: Int): OkHttpNetworkConnectionBuilder {
            this.emitTimeout = emitTimeout
            return this
        }

        /**
         * @param client An OkHttp client that will be used in the emitter, you can provide your
         *      own if you want to share your Singleton client's interceptors, connection pool etc.,
         *      otherwise a new one is created.
         * @return itself
         */
        fun client(client: OkHttpClient?): OkHttpNetworkConnectionBuilder {
            this.client = client
            return this
        }

        /**
         * @param cookieJar An OkHttp cookie jar to override the default cookie jar that stores
         * cookies in SharedPreferences. The cookie jar will be ignored in case
         * custom `client` is configured.
         * @return itself
         */
        fun cookieJar(cookieJar: CookieJar?): OkHttpNetworkConnectionBuilder {
            this.cookieJar = cookieJar
            return this
        }

        /**
         * @param customPostPath A custom path that is used on the endpoint to send requests.
         * @return itself
         */
        fun customPostPath(customPostPath: String?): OkHttpNetworkConnectionBuilder {
            this.customPostPath = customPostPath
            return this
        }

        /**
         * @param serverAnonymisation whether to anonymise server-side user identifiers including the `network_userid` and `user_ipaddress`
         * @return itself
         */
        fun serverAnonymisation(serverAnonymisation: Boolean): OkHttpNetworkConnectionBuilder {
            this.serverAnonymisation = serverAnonymisation
            return this
        }

        /**
         * Creates a new OkHttpNetworkConnection
         *
         * @return a new OkHttpNetworkConnection object
         */
        fun build(): OkHttpNetworkConnection {
            return OkHttpNetworkConnection(this)
        }
    }

    init {
        // Decode uri to extract protocol
        var tempUri = builder.uri
        val url = Uri.parse(builder.uri)
        var tempProtocol = Protocol.HTTPS
        if (url.scheme == null) {
            tempUri = "https://" + builder.uri
        } else {
            when (url.scheme) {
                "https" -> {}
                "http" -> tempProtocol = Protocol.HTTP
                else -> tempUri = "https://" + builder.uri
            }
        }

        // Configure
        networkUri = tempUri
        protocol = tempProtocol
        httpMethod = builder.httpMethod
        emitTimeout = builder.emitTimeout
        customPostPath = builder.customPostPath
        serverAnonymisation = builder.serverAnonymisation
        
        val tlsArguments = TLSArguments(builder.tlsVersions)
        uriBuilder = Uri.parse(networkUri).buildUpon()
        
        if (httpMethod == HttpMethod.GET) {
            uriBuilder.appendPath("i")
        } else if (customPostPath == null) {
            uriBuilder.appendEncodedPath(
                TrackerConstants.PROTOCOL_VENDOR + "/" +
                        TrackerConstants.PROTOCOL_VERSION
            )
        } else {
            uriBuilder.appendEncodedPath(customPostPath)
        }

        // Configure with external OkHttpClient
        client = if (builder.client == null) {
            OkHttpClient.Builder()
                .sslSocketFactory(tlsArguments.sslSocketFactory, tlsArguments.trustManager)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .cookieJar(builder.cookieJar ?: CollectorCookieJar(builder.context))
                .build()
        } else {
            builder.client
        }
    }

    override val uri: Uri
        get() = uriBuilder.clearQuery().build()

    override fun sendRequests(requests: List<Request>): List<RequestResult> {
        val futures: MutableList<Future<*>> = ArrayList()
        val results: MutableList<RequestResult> = ArrayList()

        // Start all requests in the ThreadPool
        for (request in requests) {
            val userAgent = request.customUserAgent ?: DEFAULT_USER_AGENT
            val okHttpRequest = if (httpMethod == HttpMethod.GET) buildGetRequest(
                request,
                userAgent
            ) else buildPostRequest(request, userAgent)
            futures.add(Executor.futureCallable(getRequestCallable(okHttpRequest)))
        }
        Logger.d(TAG, "Request Futures: %s", futures.size)

        // Get results of futures
        // - Wait up to emitTimeout seconds for the request
        for (i in futures.indices) {
            var code = -1
            
            try {
                code = futures[i][emitTimeout.toLong(), TimeUnit.SECONDS] as Int
            } catch (ie: InterruptedException) {
                Logger.e(TAG, "Request Future was interrupted: %s", ie.message)
            } catch (ee: ExecutionException) {
                Logger.e(TAG, "Request Future failed: %s", ee.message)
            } catch (te: TimeoutException) {
                Logger.e(TAG, "Request Future had a timeout: %s", te.message)
            }
            
            val request = requests[i]
            val eventIds = request.emitterEventIds
            results.add(RequestResult(code, request.oversize, eventIds))
            if (request.oversize) {
                Logger.track(
                    TAG,
                    "Request is oversized for emitter event IDs: %s",
                    eventIds.toString()
                )
            }
        }
        return results
    }

    /**
     * Builds an OkHttp GET request which is ready
     * to be executed.
     * @param request The request where to get the payload to be sent.
     * @param userAgent The user-agent used during the transmission to the collector.
     * @return An OkHttp request object.
     */
    private fun buildGetRequest(request: Request, userAgent: String): okhttp3.Request {
        // Clear the previous query
        uriBuilder.clearQuery()

        // Build the request query
        val hashMap = request.payload.map as HashMap<*, *>
        for (key in hashMap.keys as Iterable<String>) {
            val value = hashMap[key] as String?
            uriBuilder.appendQueryParameter(key, value)
        }

        // Build the request
        val reqUrl = uriBuilder.build().toString()
        val builder = okhttp3.Request.Builder()
            .url(reqUrl)
            .header("User-Agent", userAgent)
            .get()
        if (serverAnonymisation) {
            builder.header("SP-Anonymous", "*")
        }
        return builder.build()
    }

    /**
     * Builds an OkHttp POST request which is ready
     * to be executed.
     * @param request The request where to get the payload to be sent.
     * @param userAgent The user-agent used during the transmission to the collector.
     * @return An OkHttp request object.
     */
    private fun buildPostRequest(request: Request, userAgent: String): okhttp3.Request {
        val reqUrl = uriBuilder.build().toString()
        val reqBody = request.payload.toString().toRequestBody(JSON)
        
        val builder = okhttp3.Request.Builder()
            .url(reqUrl)
            .header("User-Agent", userAgent)
            .post(reqBody)
        if (serverAnonymisation) {
            builder.header("SP-Anonymous", "*")
        }
        return builder.build()
    }

    /**
     * Returns a Callable Request Send
     *
     * @param request the request to be sent
     * @return the new Callable object
     */
    private fun getRequestCallable(request: okhttp3.Request): Callable<Int> {
        return Callable { requestSender(request) }
    }

    /**
     * The function responsible for actually sending
     * the request to the collector.
     *
     * @param request The request to be sent
     * @return a RequestResult
     */
    private fun requestSender(request: okhttp3.Request): Int {
        return try {
            Logger.v(TAG, "Sending request: %s", request)
            TrafficStats.setThreadStatsTag(TRAFFIC_STATS_TAG)
            val resp = client!!.newCall(request).execute()
            val code = resp.code
            resp.body!!.close()
            code
        } catch (e: IOException) {
            Logger.e(TAG, "Request sending failed: %s", e.toString())
            -1
        }
    }

    companion object {
        private const val TRAFFIC_STATS_TAG = 1
        private val DEFAULT_USER_AGENT = String.format(
            "snowplow/%s android/%s",
            BuildConfig.TRACKER_LABEL,
            Build.VERSION.RELEASE
        )
    }
}
