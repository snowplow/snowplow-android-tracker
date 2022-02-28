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

package com.snowplowanalytics.snowplow.tracker.emitter;

import android.test.AndroidTestCase;

import com.snowplowanalytics.snowplow.internal.emitter.TLSArguments;
import com.snowplowanalytics.snowplow.internal.emitter.TLSVersion;

import java.util.Arrays;
import java.util.EnumSet;

public class TLSArgumentsTest extends AndroidTestCase {
    public void testEnumStringConversion() {
        EnumSet<TLSVersion> versions = EnumSet.of(TLSVersion.TLSv1_2, TLSVersion.TLSv1_1);
        TLSArguments arguments = new TLSArguments(versions);
        String[] stringVersions = arguments.getVersions();
        assertTrue(Arrays.asList(stringVersions).contains("TLSv1.2"));
        assertTrue(Arrays.asList(stringVersions).contains("TLSv1.1"));
    }
}
