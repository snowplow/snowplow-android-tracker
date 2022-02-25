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
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GlobalContext {

    @NonNull
    private FunctionalGenerator generator;
    @Nullable
    private FunctionalFilter filter;


    public GlobalContext(@NonNull ContextGenerator contextGenerator) {
        Objects.requireNonNull(contextGenerator);
        this.generator = new FunctionalGenerator() {
            @Override
            @Nullable
            public List<SelfDescribingJson> apply(@NonNull InspectableEvent event) {
                return contextGenerator.generateContexts(event);
            }
        };
        this.filter = new FunctionalFilter() {
            @Override
            public boolean apply(@NonNull InspectableEvent event) {
                return contextGenerator.filterEvent(event);
            }
        };
    }

    public GlobalContext(@NonNull List<SelfDescribingJson> staticContexts) {
        Objects.requireNonNull(staticContexts);
        this.generator = new FunctionalGenerator() {
            @Override
            @Nullable
            public List<SelfDescribingJson> apply(@NonNull InspectableEvent event) {
                return staticContexts;
            }
        };
        this.filter = null;
    }

    public GlobalContext(@NonNull FunctionalGenerator generator) {
        this(generator, (FunctionalFilter) null);
    }

    public GlobalContext(@NonNull List<SelfDescribingJson> staticContexts, @Nullable SchemaRuleSet ruleset) {
        Objects.requireNonNull(staticContexts);
        this.generator = new FunctionalGenerator() {
            @Override
            @Nullable
            public List<SelfDescribingJson> apply(@NonNull InspectableEvent event) {
                return staticContexts;
            }
        };
        this.filter = ruleset.getFilter();
    }

    public GlobalContext(@NonNull FunctionalGenerator generator, @Nullable SchemaRuleSet ruleset) {
        this(generator, ruleset.getFilter());
    }

    public GlobalContext(@NonNull List<SelfDescribingJson> staticContexts, @Nullable FunctionalFilter filter) {
        Objects.requireNonNull(staticContexts);
        this.generator = new FunctionalGenerator() {
            @Override
            @Nullable
            public List<SelfDescribingJson> apply(@NonNull InspectableEvent event) {
                return staticContexts;
            }
        };
        this.filter = filter;
    }

    public GlobalContext(@NonNull FunctionalGenerator generator, @Nullable FunctionalFilter filter) {
        Objects.requireNonNull(generator);
        this.generator = generator;
        this.filter = filter;
    }

    @NonNull
    public  List<SelfDescribingJson> generateContexts(@NonNull InspectableEvent event) {
        if (filter != null && !filter.apply(event)) {
            return new ArrayList<>();
        }
        return generator.apply(event);
    }
}
