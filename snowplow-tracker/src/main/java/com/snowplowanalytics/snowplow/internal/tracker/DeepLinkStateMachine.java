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

package com.snowplowanalytics.snowplow.internal.tracker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.entity.DeepLink;
import com.snowplowanalytics.snowplow.event.DeepLinkReceived;
import com.snowplowanalytics.snowplow.event.Event;
import com.snowplowanalytics.snowplow.internal.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.InspectableEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DeepLinkStateMachine implements StateMachineInterface {

    /*
     States: Init, DeepLinkReceived, ReadyForOutput
     Events: DL (DeepLinkReceived), SV (ScreenView)
     Transitions:
      - Init (DL) DeepLinkReceived
      - DeepLinkReceived (SV) ReadyForOutput
      - ReadyForOutput (DL) DeepLinkReceived
      - ReadyForOutput (SV) Init
     Entity Generation:
      - ReadyForOutput
      */

    @NonNull
    @Override
    public List<String> subscribedEventSchemasForTransitions() {
        return Arrays.asList(DeepLinkReceived.SCHEMA, TrackerConstants.SCHEMA_SCREEN_VIEW);
    }

    @NonNull
    @Override
    public List<String> subscribedEventSchemasForEntitiesGeneration() {
        return Arrays.asList(TrackerConstants.SCHEMA_SCREEN_VIEW);
    }

    @NonNull
    @Override
    public List<String> subscribedEventSchemasForPayloadUpdating() {
        return new ArrayList<>();
    }

    @Nullable
    @Override
    public State transition(@NonNull Event event, @Nullable State state) {
        // - Init (DL) DeepLinkReceived
        // - ReadyForOutput (DL) DeepLinkReceived
        if (event instanceof DeepLinkReceived) {
            DeepLinkReceived dlEvent = (DeepLinkReceived) event;
            return new DeepLinkState(dlEvent.url, dlEvent.referrer);
        } else {
            // - Init (SV) Init
            if (state == null) {
                return null;
            }
            // - ReadyForOutput (SV) Init
            DeepLinkState dlState = (DeepLinkState) state;
            if (dlState.readyForOutput) {
                return null;
            }
            // - DeepLinkReceived (SV) ReadyForOutput
            DeepLinkState currentState = new DeepLinkState(dlState.url, dlState.referrer);
            currentState.readyForOutput = true;
            return currentState;
        }
    }

    @Nullable
    @Override
    public List<SelfDescribingJson> entities(@NonNull InspectableEvent event, @Nullable State state) {
        if (state == null) {
            return null;
        }
        DeepLinkState deepLinkState = (DeepLinkState) state;
        if (!deepLinkState.readyForOutput) {
            return null;
        }
        DeepLink entity = new DeepLink(deepLinkState.url)
                .referrer(deepLinkState.referrer);
        return Collections.singletonList(entity);
    }

    @Nullable
    @Override
    public Map<String, Object> payloadValues(@NonNull InspectableEvent event, @Nullable State state) {
        return null;
    }
}
