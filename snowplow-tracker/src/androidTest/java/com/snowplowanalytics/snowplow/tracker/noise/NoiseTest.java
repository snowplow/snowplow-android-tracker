/*
 * Copyright (c) 2015 Snowplow Analytics Ltd. All rights reserved.
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

package com.snowplowanalytics.snowplow.tracker.noise;

import android.test.AndroidTestCase;

import com.snowplowanalytics.snowplow.tracker.DevicePlatforms;
import com.snowplowanalytics.snowplow.tracker.Executor;
import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.emitter.BufferOption;
import com.snowplowanalytics.snowplow.tracker.emitter.HttpMethod;
import com.snowplowanalytics.snowplow.tracker.emitter.RequestSecurity;
import com.snowplowanalytics.snowplow.tracker.utils.FileStore;
import com.snowplowanalytics.snowplow.tracker.utils.LogLevel;
import com.snowplowanalytics.snowplow.tracker.utils.Logger;
import com.snowplowanalytics.snowplow.tracker.utils.Preconditions;
import com.snowplowanalytics.snowplow.tracker.utils.Util;

import java.lang.reflect.Constructor;

public class NoiseTest extends AndroidTestCase {

    public void testEnum() {
        NoiseTest.superficialEnumCodeCoverage(BufferOption.class);
        NoiseTest.superficialEnumCodeCoverage(HttpMethod.class);
        NoiseTest.superficialEnumCodeCoverage(RequestSecurity.class);
        NoiseTest.superficialEnumCodeCoverage(LogLevel.class);
        NoiseTest.superficialEnumCodeCoverage(DevicePlatforms.class);
    }

    public void testPrivateConstructor() {
        NoiseTest.superficialPrivateConstructorCodeCoverage(Logger.class);
        NoiseTest.superficialPrivateConstructorCodeCoverage(Preconditions.class);
        NoiseTest.superficialPrivateConstructorCodeCoverage(FileStore.class);
        NoiseTest.superficialPrivateConstructorCodeCoverage(Util.class);
        NoiseTest.superficialPrivateConstructorCodeCoverage(Executor.class);
        NoiseTest.superficialPrivateConstructorCodeCoverage(Parameters.class);
        NoiseTest.superficialPrivateConstructorCodeCoverage(TrackerConstants.class);
    }

    private static void superficialEnumCodeCoverage(Class<? extends Enum<?>> enumClass) {
        try {
            for (Object o : (Object[]) enumClass.getMethod("values").invoke(null)) {
                enumClass.getMethod("valueOf", String.class).invoke(null, o.toString());
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static void superficialPrivateConstructorCodeCoverage(Class<?> privateClass) {
        try {
            Constructor<?> constructor = privateClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
