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

import android.util.Base64;

import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.payload.TrackerPayload;
import com.snowplowanalytics.snowplow.tracker.utils.Logger;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class GlobalContextUtils {

    static final String TAG = GlobalContextUtils.class.getSimpleName();

    /**
     * Re-evaluates global contexts for the provided payload
     * @return An ArrayList of SDJs representing global contexts ready to be appended to the event's contexts
     */
    public static synchronized ArrayList<SelfDescribingJson>
    evalGlobalContexts(TrackerPayload payload, ArrayList<GlobalContext> globalContexts) {

        ArrayList<SelfDescribingJson> computedGlobalContexts = new ArrayList<>();

        String eventType = (String) payload.getMap().get(Parameters.EVENT);
        String eventSchema = getSchema(payload);
        for (GlobalContext context : globalContexts) {
            if (context instanceof ContextPrimitive) {
                computeContextPrimitive(payload, computedGlobalContexts,
                        (ContextPrimitive) context, eventType, eventSchema);
            } else if (context instanceof ConditionalContextProvider) {
                if (context instanceof FilterProvider) {
                    if (((FilterProvider) context).getContextFilter().filter(payload)) {
                        computeContextPrimitives(payload, computedGlobalContexts,
                                ((FilterProvider) context).getContextPrimitives(), eventType, eventSchema);
                    }
                } else if (context instanceof RuleSetProvider) {
                    boolean ruleSetCheck =
                            matchSchemaAgainstRuleSet((((RuleSetProvider) context).getRuleSet()), eventSchema);
                    if (ruleSetCheck) {
                        computeContextPrimitives(payload, computedGlobalContexts,
                                ((RuleSetProvider) context).getContextPrimitives(), eventType, eventSchema);

                    }
                }
            }
        }
        return computedGlobalContexts;
    }

    static boolean matchSchemaAgainstRuleSet(RuleSet ruleSet, String eventSchema) {
        ArrayList<String> acceptRules = ruleSet.getAccept();
        ArrayList<String> rejectRules = ruleSet.getReject();
        int acceptCount = 0;
        int rejectCount = 0;

        for (String rule : acceptRules) {
            if (matchSchemaAgainstRule(rule, eventSchema)) {
                ++acceptCount;
            }
        }

        for (String rule : rejectRules) {
            if (matchSchemaAgainstRule(rule, eventSchema)) {
                ++rejectCount;
            }
        }

        return (acceptCount > 0 && rejectCount == 0);
    }

    static boolean matchSchemaAgainstRule(String rule, String eventSchema) {
        if (!isValidRule(rule)) {
            return false;
        }
        String[] ruleParts = getUriSubparts(rule);
        String[] schemaParts = getUriSubparts(eventSchema);

        if (ruleParts.length > 0 && schemaParts.length > 0) {
            if (!matchVendor(ruleParts[0], schemaParts[0])) {
                return false;
            }
            for (int i=1; i<=4; i++) {
                if (!matchPart(ruleParts[i], schemaParts[i])) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    static boolean isValidRule(String rule) {
        if (rule != null) {
            String[] ruleParts = getUriSubparts(rule);
            if (ruleParts.length == 5) {
                String vendor = ruleParts[0];
                String[] versionParts = Arrays.copyOfRange(ruleParts, 2, ruleParts.length);
                return validateVendor(vendor) && validateVersion(versionParts);
            }
            return false;
        }
        return false;
    }

    /**
     * Splits an Iglu URI or a Global Context Rule to extract vendor, name and version parts (model, revision, addition)
     * @param uri an Iglu URI or a Global Context Rule
     * @return Vendor, name and version parts (model, revision, addition)
     */
    static String[] getUriSubparts(String uri) {
        // split after excluding protocol
        String[] parts = uri.substring(5).split("/");
        if (parts.length == 4) {
            String[] versionParts = parts[3].split("-");
            if (versionParts.length == 3) {
                // parts[2] corresponds to the format of the uri, always same, skipped intentionally
                return new String[]{ parts[0], parts[1], versionParts[0], versionParts[1], versionParts[2] };
            }
            return new String[]{};
        } else {
            return new String[]{};
        }
    }

    static boolean validateVersion(String[] versionParts) {
        if (versionParts.length == 3) {
            boolean asterisk = false;
            for (String part : versionParts) {
                if (part.equals("*")) {
                    asterisk = true;
                } else if (asterisk) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    static boolean validateVendor(String vendor) {
        String[] parts = vendor.split("\\.");
        return (parts.length > 1) && validateVendorParts(parts);
    }

    static boolean validateVendorParts(String[] parts) {
        if (parts[0].equals("*") || parts[1].equals("*")) {
            return false;
        }
        if (parts.length == 2) {
            return true;
        } else {
            String[] wildcardAllowedParts = Arrays.copyOfRange(parts, 2, parts.length);
            boolean asterisk = false;
            for (String part: wildcardAllowedParts) {
                if (part.equals("*")) {
                    asterisk = true;
                } else if (asterisk) {
                    return false;
                }
            }
            return true;
        }
    }

    static boolean matchVendor(String ruleVendor, String schemaVendor) {
        String[] ruleVendorParts = ruleVendor.split("\\.");
        String[] schemaVendorParts = schemaVendor.split("\\.");

        if ((ruleVendorParts.length > 1) && (schemaVendorParts.length > 1)) {
            if ((ruleVendorParts.length == schemaVendorParts.length) && validateVendorParts(ruleVendorParts))  {
                for (int i=0; i < ruleVendorParts.length; ++i) {
                    if (!matchPart(ruleVendorParts[i], schemaVendorParts[i])) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
        return false;
    }

    static boolean matchPart(String rulePart, String schemaPart) {
        return (rulePart != null) && (schemaPart != null) && (rulePart.equals("*") || (rulePart.equals(schemaPart)));
    }

    static void computeContextPrimitive(TrackerPayload payload,
                                        ArrayList<SelfDescribingJson> computedGlobalContexts,
                                        ContextPrimitive primitive,
                                        String eventType, String eventSchema) {
        if (primitive instanceof ContextGenerator) {
            computedGlobalContexts.add(((ContextGenerator) primitive).generate(payload, eventType, eventSchema));
        } else if (primitive instanceof SelfDescribingJson) {
            computedGlobalContexts.add((SelfDescribingJson) primitive);
        }
    }

    static void computeContextPrimitives(TrackerPayload payload,
                                         ArrayList<SelfDescribingJson> computedGlobalContexts,
                                         ArrayList<ContextPrimitive> primitives,
                                         String eventType, String eventSchema) {
        for (ContextPrimitive primitive : primitives) {
            computeContextPrimitive(payload, computedGlobalContexts, primitive, eventType, eventSchema);
        }
    }

    static String getSchema(TrackerPayload payload) {
        HashMap<String,Object> trackerLoad = payload.getMap();
        try {
            if (trackerLoad.containsKey(Parameters.UNSTRUCTURED)) {
                String unstructPayload = (String) trackerLoad.get(Parameters.UNSTRUCTURED);
                return new JSONObject(unstructPayload).getJSONObject("data").getString("schema");
            } else if (trackerLoad.containsKey(Parameters.UNSTRUCTURED_ENCODED)) {
                String encodedSchema = (String) (trackerLoad.get(Parameters.UNSTRUCTURED_ENCODED));
                JSONObject decodedSchema = new JSONObject(new String(Base64.decode(encodedSchema, Base64.DEFAULT)));
                return decodedSchema.getJSONObject("data").getString("schema");
            } else { // the case of first class Snowplow events
                return TrackerConstants.SCHEMA_PAYLOAD_DATA;
            }
        } catch (Exception e) {
            return "";
        }
    }
}
