/*
 * Copyright (c) 2015-2020 Snowplow Analytics Ltd. All rights reserved.
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

package com.snowplowanalytics.snowplow.tracker;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.snowplowanalytics.snowplow.tracker.emitter.HttpMethod;
import com.snowplowanalytics.snowplow.tracker.emitter.RequestResult;
import com.snowplowanalytics.snowplow.tracker.networkconnection.Request;

import java.util.List;

/**
 * Interface for the component that
 * sends events to the collector.
 */
public interface NetworkConnection {

    /**
     * Send requests to the collector.
     * @param requests to send.
     * @return results of the sending operation.
     */
    @NonNull List<RequestResult> sendRequests(@NonNull List<Request> requests);

    /**
     * @return http method used to send requests to the collector.
     */
    @NonNull HttpMethod getHttpMethod();

    /**
     * @return URI of the collector.
     */
    @NonNull Uri getUri();
}
