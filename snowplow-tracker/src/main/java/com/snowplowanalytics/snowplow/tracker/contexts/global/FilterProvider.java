/*
 * Copyright (c) 2019 Snowplow Analytics Ltd. All rights reserved.
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

package com.snowplowanalytics.snowplow.tracker.contexts.global;

import java.util.ArrayList;

public class FilterProvider implements ConditionalContextProvider {

    private ContextFilter contextFilter;
    private ArrayList<ContextPrimitive> contextPrimitives;

    public FilterProvider(ContextFilter contextFilter, ArrayList<ContextPrimitive> contextPrimitives) {
        this.contextFilter = contextFilter;
        this.contextPrimitives = contextPrimitives;
    }

    public FilterProvider(ContextFilter contextFilter, ContextPrimitive contextPrimitive) {
        this.contextFilter = contextFilter;

        ArrayList<ContextPrimitive> arr = new ArrayList<>();
        arr.add(contextPrimitive);
        this.contextPrimitives = arr;
    }

    public ContextFilter getContextFilter() {
        return contextFilter;
    }

    public void setContextFilter(ContextFilter contextFilter) {
        this.contextFilter = contextFilter;
    }

    public ArrayList<ContextPrimitive> getContextPrimitives() {
        return contextPrimitives;
    }

    public void setContextPrimitives(ArrayList<ContextPrimitive> contextPrimitives) {
        this.contextPrimitives = contextPrimitives;
    }
}
