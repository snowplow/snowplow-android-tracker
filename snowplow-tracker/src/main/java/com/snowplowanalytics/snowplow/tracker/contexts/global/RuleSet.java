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

public class RuleSet {

    private ArrayList<String> accept;
    private ArrayList<String> reject;

    public RuleSet(ArrayList<String> accept, ArrayList<String> reject) {
        this.accept = accept;
        this.reject = reject;
    }

    public RuleSet(String accept, String reject) {
        ArrayList<String> acc = new ArrayList<>();
        acc.add(accept);
        this.accept = acc;

        ArrayList<String> rej = new ArrayList<>();
        rej.add(reject);
        this.reject = rej;
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
}
