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

public class RuleSetProvider implements ConditionalContextProvider {

    private RuleSet ruleSet;
    private ArrayList<ContextPrimitive> contextPrimitives;

    public RuleSetProvider(RuleSet ruleSet, ArrayList<ContextPrimitive> contextPrimitives) {
        this.ruleSet = ruleSet;
        this.contextPrimitives = contextPrimitives;
    }

    public RuleSetProvider(RuleSet ruleSet, ContextPrimitive contextPrimitive) {
        this.ruleSet = ruleSet;

        ArrayList<ContextPrimitive> arr = new ArrayList<>();
        arr.add(contextPrimitive);
        this.contextPrimitives = arr;
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

    public void setContextPrimitives(ArrayList<ContextPrimitive> contextPrimitives) {
        this.contextPrimitives = contextPrimitives;
    }
}
