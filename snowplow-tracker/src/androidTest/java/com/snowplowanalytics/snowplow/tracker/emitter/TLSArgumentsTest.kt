/*
 * Copyright (c) 2015-2023 Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.snowplow.tracker.emitter

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.snowplowanalytics.core.emitter.TLSArguments
import com.snowplowanalytics.core.emitter.TLSVersion
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class TLSArgumentsTest {
    @Test
    fun testEnumStringConversion() {
        val versions = EnumSet.of(TLSVersion.TLSv1_2, TLSVersion.TLSv1_1)
        val arguments = TLSArguments(versions)
        val stringVersions = arguments.versions
        Assert.assertTrue(listOf(*stringVersions).contains("TLSv1.2"))
        Assert.assertTrue(listOf(*stringVersions).contains("TLSv1.1"))
    }
}
