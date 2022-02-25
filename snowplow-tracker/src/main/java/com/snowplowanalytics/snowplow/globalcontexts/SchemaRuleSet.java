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

package com.snowplowanalytics.snowplow.globalcontexts;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.tracker.InspectableEvent;
import com.snowplowanalytics.snowplow.internal.tracker.SchemaRule;

import java.util.ArrayList;
import java.util.List;

public class SchemaRuleSet {

    @NonNull
    private final List<SchemaRule> rulesAllowed = new ArrayList<>();
    @NonNull
    private final List<SchemaRule> rulesDenied = new ArrayList<>();

    private SchemaRuleSet(@Nullable List<String> allowed, @Nullable List<String> denied) {
        for (String rule : allowed) {
            SchemaRule schemaRule = SchemaRule.build(rule);
            if (schemaRule != null) {
                rulesAllowed.add(schemaRule);
            }
        }
        for (String rule : denied) {
            SchemaRule schemaRule = SchemaRule.build(rule);
            if (schemaRule != null) {
                rulesDenied.add(schemaRule);
            }
        }
    }

    @NonNull
    public static SchemaRuleSet buildRuleSet(@NonNull List<String> allowed, @NonNull List<String> denied) {
        return new SchemaRuleSet(allowed, denied);
    }

    @NonNull
    public static SchemaRuleSet buildRuleSetWithAllowedList(@NonNull List<String> allowed) {
        return buildRuleSet(allowed, new ArrayList<>());
    }

    @NonNull
    public static SchemaRuleSet buildRuleSetWithDeniedList(@NonNull List<String> denied) {
        return buildRuleSet(new ArrayList<>(), denied);
    }

    @NonNull
    public List<String> getAllowed() {
        List<String> result = new ArrayList<>(rulesAllowed.size());
        for (SchemaRule schemaRule : rulesAllowed) {
            result.add(schemaRule.getRule());
        }
        return result;
    }

    @NonNull
    public List<String> getDenied() {
        List<String> result = new ArrayList<>(rulesDenied.size());
        for (SchemaRule schemaRule : rulesDenied) {
            result.add(schemaRule.getRule());
        }
        return result;
    }

    @NonNull
    public FunctionalFilter getFilter() {
        return new FunctionalFilter() {
            @Override
            public boolean apply(@NonNull InspectableEvent event) {
                return matchWithSchema(event.getSchema());
            }
        };
    }

    public boolean matchWithSchema(@Nullable String schema) {
        if (schema == null) {
            return false;
        }
        for (SchemaRule rule : rulesDenied) {
            if (rule.matchWithSchema(schema)) {
                return false;
            }
        }
        if (rulesAllowed.size() == 0) {
            return true;
        }
        for (SchemaRule rule : rulesAllowed) {
            if (rule.matchWithSchema(schema)) {
                return true;
            }
        }
        return false;
    }
}
