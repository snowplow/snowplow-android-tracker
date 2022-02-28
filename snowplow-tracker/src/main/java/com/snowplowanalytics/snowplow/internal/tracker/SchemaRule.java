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
import androidx.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SchemaRule {

    private final static String RULE_PATTERN = "^iglu:((?:(?:[a-zA-Z0-9-_]+|\\*)\\.)+(?:[a-zA-Z0-9-_]+|\\*))\\/([a-zA-Z0-9-_\\.]+|\\*)\\/([a-zA-Z0-9-_\\.]+|\\*)\\/([1-9][0-9]*|\\*)-(0|[1-9][0-9]*|\\*)-(0|[1-9][0-9]*|\\*)$";
    private final static String URI_PATTERN = "^iglu:((?:(?:[a-zA-Z0-9-_]+)\\.)+(?:[a-zA-Z0-9-_]+))\\/([a-zA-Z0-9-_]+)\\/([a-zA-Z0-9-_]+)\\/([1-9][0-9]*)\\-(0|[1-9][0-9]*)\\-(0|[1-9][0-9]*)$";

    private final String rule;
    private final List<String> ruleParts;

    private SchemaRule(@NonNull String rule, @NonNull List<String> ruleParts) {
        this.rule = rule;
        this.ruleParts = ruleParts;
    }

    @Nullable
    public static SchemaRule build(@NonNull String rule) {
        if (rule == null || rule.isEmpty()) {
            return null;
        }
        List<String> parts = getParts(rule, RULE_PATTERN);
        if (parts == null || parts.isEmpty() || !validateVendor(parts.get(0))) {
            return null;
        }
        return new SchemaRule(rule, parts);
    }

    @NonNull
    public String getRule() {
        return rule;
    }

    public boolean matchWithSchema(@NonNull String schema) {
        if (schema == null) {
            return false;
        }
        List<String> uriParts = getParts(schema, URI_PATTERN);
        if (uriParts == null || uriParts.size() < ruleParts.size()) {
            return false;
        }
        // Check vendor part
        String[] ruleVendor = ruleParts.get(0).split("\\.");
        String[] uriVendor = uriParts.get(0).split("\\.");
        if (uriVendor.length != ruleVendor.length) {
            return false;
        }
        int index = 0;
        for (String ruleVendorPart : ruleVendor) {
            if (!"*".equals(ruleVendorPart) && !uriVendor[index].equals(ruleVendorPart)) {
                return false;
            }
            index++;
        }
        // Check the rest of the rule
        index = 1;
        for (String rulePart : ruleParts.subList(1, ruleParts.size())) {
            if (!"*".equals(rulePart) && !uriParts.get(index).equals(rulePart)) {
                return false;
            }
            index++;
        }
        return true;
    }

    // Private methods

    @Nullable
    private static List<String> getParts(@NonNull String uri, @NonNull String regex) {
        List<String> result = new ArrayList<>(6);
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(uri);
        if (!matcher.find()) {
            return null;
        }
        for (int i=1; i < matcher.groupCount(); i++) {
            if (i > 6) return null;
            String part = matcher.group(i);
            result.add(part);
        }
        return result;
    }

    private static boolean validateVendor(@NonNull String vendor) {
        // the components array will be generated like this from vendor:
        // "com.acme.marketing" => ["com", "acme", "marketing"]
        String[] components = vendor.split("\\.");
        // check that vendor doesn't begin or end with period
        // e.g. ".snowplowanalytics.snowplow." => ["", "snowplowanalytics", "snowplow", ""]
        if (components.length > 1 && (components[0].isEmpty() || components[components.length-1].isEmpty())) {
            return false;
        }
        // reject vendors with criteria that are too broad & don't make sense, i.e. "*.*.marketing"
        if ("*".equals(components[0]) || "*".equals(components[1])) {
            return false;
        }
        // now validate the remaining parts, vendors should follow matching that never breaks trailing specificity
        // in other words, once we use an asterisk, we must continue using asterisks for parts or stop
        // e.g. "com.acme.marketing.*.*" is allowed, but "com.acme.*.marketing.*" or "com.acme.*.marketing" is forbidden
        if (components.length <= 2) return true;
        // trailingComponents are the remaining parts after the first two
        String[] trailingComponents = Arrays.copyOfRange(components, 2, components.length);
        boolean asterisk = false;
        for (String part : trailingComponents) {
            if ("*".equals(part)) { // mark when we've found a wildcard
                asterisk = true;
            } else if (asterisk) { // invalid when alpha parts come after wildcard
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SchemaRule that = (SchemaRule) o;
        return rule.equals(that.rule);
    }

    @Override
    public int hashCode() {
        return rule.hashCode();
    }
}
