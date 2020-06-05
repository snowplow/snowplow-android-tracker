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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RuleSet {

    private ArrayList<String> accept = new ArrayList<>();;
    private ArrayList<String> reject = new ArrayList<>();;

    public RuleSet(List<String> accept, List<String> reject) {
        Collections.addAll(accept, accept.toArray(new String[0]));
        Collections.addAll(reject, reject.toArray(new String[0]));
    }

    public RuleSet(String accept, String reject) {
        this.accept.add(accept);
        this.reject.add(reject);
    }

    public ArrayList<String> getAccept() {
        return accept;
    }

    public void setAccept(ArrayList<String> accept) {
        this.accept = accept;
    }

    public ArrayList<String> getReject() {
        return reject;
    }

    public void setReject(ArrayList<String> reject) {
        this.reject = reject;
    }

    public boolean isValid() {
        for (String rule: accept) {
            if (!GlobalContextUtils.isValidRule(rule)) {
                return false;
            }
        }

        for (String rule: reject) {
            if (!GlobalContextUtils.isValidRule(rule)) {
                return false;
            }
        }

        return true;
    }
}
