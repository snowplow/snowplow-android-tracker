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

package com.snowplowanalytics.snowplow.tracker.contexts.global;

import com.snowplowanalytics.snowplow.internal.utils.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RuleSetProvider implements ConditionalContextProvider {

    private String tag;
    private RuleSet ruleSet;
    private ArrayList<ContextPrimitive> contextPrimitives = new ArrayList<>();

    public RuleSetProvider(String tag, RuleSet ruleSet, List<ContextPrimitive> contextPrimitives) {
        setTag(tag);
        this.ruleSet = ruleSet;
        Collections.addAll(this.contextPrimitives, contextPrimitives.toArray(new ContextPrimitive[0]));
    }

    public RuleSetProvider(String tag, RuleSet ruleSet, ContextPrimitive contextPrimitive) {
        setTag(tag);
        this.ruleSet = ruleSet;
        this.contextPrimitives.add(contextPrimitive);
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        Preconditions.checkNotNull(tag, "tag cannot be null");
        this.tag = tag;
    }

    public RuleSet getRuleSet() {
        return ruleSet;
    }

    public void setRuleSet(RuleSet ruleSet) {
        this.ruleSet = ruleSet;
    }

    public ArrayList<ContextPrimitive> getContextPrimitives() {
        return contextPrimitives;
    }

    public void setContextPrimitives(List<ContextPrimitive> contextPrimitives) {
        this.contextPrimitives.clear();
        Collections.addAll(this.contextPrimitives, contextPrimitives.toArray(new ContextPrimitive[0]));
    }

    @Override
    public String tag() {
        return tag;
    }
}
