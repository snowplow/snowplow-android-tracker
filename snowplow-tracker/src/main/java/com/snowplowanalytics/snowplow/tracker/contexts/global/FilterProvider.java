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

import com.snowplowanalytics.snowplow.tracker.utils.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FilterProvider implements ConditionalContextProvider {

    private String tag;
    private ContextFilter contextFilter;
    private ArrayList<ContextPrimitive> contextPrimitives = new ArrayList<>();

    public FilterProvider(String tag, ContextFilter contextFilter, List<ContextPrimitive> contextPrimitives) {
        setTag(tag);
        this.contextFilter = contextFilter;
        Collections.addAll(this.contextPrimitives, contextPrimitives.toArray(new ContextPrimitive[0]));
    }

    public FilterProvider(String tag, ContextFilter contextFilter, ContextPrimitive contextPrimitive) {
        setTag(tag);
        this.contextFilter = contextFilter;
        this.contextPrimitives.add(contextPrimitive);
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        Preconditions.checkNotNull(tag, "tag cannot be null");
        this.tag = tag;
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

    public void setContextPrimitives(List<ContextPrimitive> primitives) {
        this.contextPrimitives.clear();
        Collections.addAll(this.contextPrimitives, primitives.toArray(new ContextPrimitive[0]));
    }

    @Override
    public String tag() {
        return tag;
    }
}
