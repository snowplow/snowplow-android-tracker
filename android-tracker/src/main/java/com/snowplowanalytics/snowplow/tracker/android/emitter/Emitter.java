/*
 * Copyright (c) 2014 Snowplow Analytics Ltd. All rights reserved.
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

package com.snowplowanalytics.snowplow.tracker.android.emitter;

import android.content.Context;
import android.net.Uri;

import com.snowplowanalytics.snowplow.tracker.android.Constants;
import com.snowplowanalytics.snowplow.tracker.android.EventStore;
import com.snowplowanalytics.snowplow.tracker.core.emitter.BufferOption;
import com.snowplowanalytics.snowplow.tracker.core.emitter.HttpMethod;
import com.snowplowanalytics.snowplow.tracker.core.emitter.RequestCallback;
import com.snowplowanalytics.snowplow.tracker.core.emitter.RequestMethod;
import com.snowplowanalytics.snowplow.tracker.core.payload.Payload;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;

public class Emitter extends com.snowplowanalytics.snowplow.tracker.core.emitter.Emitter {

    private Uri.Builder uriBuilder = new Uri.Builder();
    private final Logger logger = LoggerFactory.getLogger(Emitter.class);
    private final EventStore eventStore;

    /**
     * Create an Emitter instance with a collector URL.
     *
     * @param URI The collector URL. Don't include "http://" - this is done automatically.
     */
    public Emitter(String URI, Context context) {
        this(URI, HttpMethod.GET, null, context);
    }

    /**
     * Create an Emitter instance with a collector URL, and callback method.
     *
     * @param URI      The collector URL. Don't include "http://" - this is done automatically.
     * @param callback The callback method to handle success/failure cases when sending events.
     */
    public Emitter(String URI, Context context, RequestCallback callback) {
        this(URI, HttpMethod.GET, callback, context);
    }

    /**
     * Create an Emitter instance with a collector URL,
     *
     * @param URI        The collector URL. Don't include "http://" - this is done automatically.
     * @param httpMethod The HTTP request method. If GET, <code>BufferOption</code> is set to <code>Instant</code>.
     */
    public Emitter(String URI, Context context, HttpMethod httpMethod) {
        this(URI, httpMethod, null, context);
    }

    /**
     * Create an Emitter instance with a collector URL and HttpMethod to send requests.
     *
     * @param URI        The collector URL. Don't include "http://" - this is done automatically.
     * @param httpMethod The HTTP request method. If GET, <code>BufferOption</code> is set to <code>Instant</code>.
     * @param callback   The callback method to handle success/failure cases when sending events.
     */
    public Emitter(String URI, HttpMethod httpMethod, RequestCallback callback, Context context) {
        if(httpMethod == HttpMethod.GET) {
            uriBuilder.scheme("http")
                    .authority(URI)
                    .appendPath("i");
        } else {
            uriBuilder.scheme("http")
                    .authority(URI)
                    .appendPath(Constants.DEFAULT_VENDOR + "/tp2");
        }
        super.httpMethod = httpMethod;
        super.requestCallback = callback;
        this.eventStore = new EventStore(context);

        if (httpMethod == HttpMethod.GET) {
            super.setBufferOption(BufferOption.Instant);
        }
    }

    @SuppressWarnings("unchecked")
    protected HttpResponse sendGetData(Payload payload) {
        HashMap hashMap = (HashMap) payload.getMap();
        Iterator<String> iterator = hashMap.keySet().iterator();
        HttpResponse httpResponse = null;
        HttpClient httpClient = new DefaultHttpClient();

        while (iterator.hasNext()) {
            String key = iterator.next();
            String value = (String) hashMap.get(key);
            this.uriBuilder.appendQueryParameter(key, value);
        }

        try {
            HttpGet httpGet = new HttpGet(uriBuilder.build().toString());
            httpResponse = httpClient.execute(httpGet);
            logger.debug(httpResponse.getStatusLine().toString());
        } catch (IOException e) {
            logger.error("Error when sending HTTP GET error.");
            e.printStackTrace();
        }
        return httpResponse;
    }

    protected HttpResponse sendPostData(Payload payload) {
        HttpPost httpPost = new HttpPost(uriBuilder.build().toString());
        httpPost.addHeader("Content-Type", "application/json; charset=utf-8");
        HttpResponse httpResponse = null;
        HttpClient httpClient = new DefaultHttpClient();

        try {
            StringEntity params = new StringEntity(payload.toString());
            httpPost.setEntity(params);
            httpResponse = httpClient.execute(httpPost);
            logger.debug(httpResponse.getStatusLine().toString());
        } catch (UnsupportedEncodingException e) {
            logger.error("Encoding exception with the payload.");
            e.printStackTrace();
        } catch (IOException e) {
            logger.error("Error when sending HTTP POST.");
            e.printStackTrace();
        }
        return httpResponse;
    }

    @Deprecated
    @Override
    public void setRequestMethod(RequestMethod option) {
        logger.error("Cannot change RequestMethod: Asynchronous requests only available.");
    }
}
