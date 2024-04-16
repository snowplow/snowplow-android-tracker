/*
 * Copyright (c) 2015-present Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.snowplow.internal.tracker

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.snowplowanalytics.snowplow.Snowplow.createTracker
import com.snowplowanalytics.snowplow.Snowplow.defaultTracker
import com.snowplowanalytics.snowplow.Snowplow.instancedTrackerNamespaces
import com.snowplowanalytics.snowplow.Snowplow.removeAllTrackers
import com.snowplowanalytics.snowplow.Snowplow.removeTracker
import com.snowplowanalytics.snowplow.Snowplow.setTrackerAsDefault
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MultipleInstancesTest {
    @Before
    fun setUp() {
        removeAllTrackers()
    }

    @After
    fun tearDown() {
        removeAllTrackers()
    }

    @Test
    fun testSingleInstanceIsReconfigurable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val t1 = createTracker(context, "t1", NetworkConfiguration("snowplowanalytics.fake"))
        Assert.assertEquals(
            "https://snowplowanalytics.fake/com.snowplowanalytics.snowplow/tp2",
            t1.network!!.endpoint
        )
        val t2 = createTracker(context, "t1", NetworkConfiguration("snowplowanalytics.fake2"))
        Assert.assertEquals(
            "https://snowplowanalytics.fake2/com.snowplowanalytics.snowplow/tp2",
            t2.network!!.endpoint
        )
        Assert.assertEquals(mutableSetOf("t1"), instancedTrackerNamespaces)
        Assert.assertEquals(t1, t2)
    }

    @Test
    fun testMultipleInstances() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val t1 = createTracker(context, "t1", NetworkConfiguration("snowplowanalytics.fake"))
        Assert.assertEquals(
            "https://snowplowanalytics.fake/com.snowplowanalytics.snowplow/tp2",
            t1.network!!.endpoint
        )
        val t2 = createTracker(context, "t2", NetworkConfiguration("snowplowanalytics.fake2"))
        Assert.assertEquals(
            "https://snowplowanalytics.fake2/com.snowplowanalytics.snowplow/tp2",
            t2.network!!.endpoint
        )
        Assert.assertEquals(mutableSetOf("t1", "t2"), instancedTrackerNamespaces)
        Assert.assertNotEquals(t1, t2)
    }

    @Test
    fun testDefaultTracker() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val t1 = createTracker(context, "t1", NetworkConfiguration("snowplowanalytics.fake"))
        createTracker(context, "t2", NetworkConfiguration("snowplowanalytics.fake2"))
        val td = defaultTracker
        Assert.assertEquals(t1, td)
    }

    @Test
    fun testUpdateDefaultTracker() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        createTracker(context, "t1", NetworkConfiguration("snowplowanalytics.fake"))
        val t2 = createTracker(context, "t2", NetworkConfiguration("snowplowanalytics.fake2"))
        setTrackerAsDefault(t2)
        val td = defaultTracker
        Assert.assertEquals(t2, td)
    }

    @Test
    fun testRemoveTracker() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val t1 = createTracker(context, "t1", NetworkConfiguration("snowplowanalytics.fake"))
        val t2 = createTracker(context, "t2", NetworkConfiguration("snowplowanalytics.fake2"))
        removeTracker(t1)
        Assert.assertNotNull(t2)
        Assert.assertEquals(mutableSetOf("t2"), instancedTrackerNamespaces)
    }

    @Test
    fun testRecreateTrackerWhichWasRemovedWithSameNamespace() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val t1 = createTracker(context, "t1", NetworkConfiguration("snowplowanalytics.fake"))
        removeTracker(t1)
        val t2 = createTracker(context, "t1", NetworkConfiguration("snowplowanalytics.fake2"))
        Assert.assertNotEquals(t1, t2)
        Assert.assertNotNull(t2)
        Assert.assertEquals(mutableSetOf("t1"), instancedTrackerNamespaces)
    }

    @Test
    fun testRemoveDefaultTracker() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val t1 = createTracker(context, "t1", NetworkConfiguration("snowplowanalytics.fake"))
        removeTracker(t1)
        val td = defaultTracker
        Assert.assertNull(td)
        Assert.assertEquals(mutableSetOf<Any>(), instancedTrackerNamespaces)
    }

    @Test
    fun testRemoveAllTrackers() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        createTracker(context, "t1", NetworkConfiguration("snowplowanalytics.fake"))
        createTracker(context, "t2", NetworkConfiguration("snowplowanalytics.fake2"))
        removeAllTrackers()
        Assert.assertEquals(mutableSetOf<Any>(), instancedTrackerNamespaces)
    }
}
