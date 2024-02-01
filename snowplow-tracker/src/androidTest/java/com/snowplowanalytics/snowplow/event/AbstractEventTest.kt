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
package com.snowplowanalytics.snowplow.event

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AbstractEventTest {
    
    @Test
    fun testAddsEntitiesUsingAllAPIs() {
        val event = ScreenView("screen")
        val entity1 = SelfDescribingJson("schema1", "data1")
        val entity2 = SelfDescribingJson("schema2", "data2")
        val entity3 = SelfDescribingJson("schema3", "data3")

        event.entities.add(entity1)
        Assert.assertEquals(1, event.entities.count())
        
        event.entities(listOf(entity2))
        Assert.assertEquals(2, event.entities.count())
        
        event.contexts(listOf(entity3))
        Assert.assertEquals(3, event.entities.count())
        Assert.assertEquals(3, event.contexts.count())
        Assert.assertTrue(event.entities.contains(entity1))
        Assert.assertTrue(event.entities.contains(entity2))
        Assert.assertTrue(event.entities.contains(entity3))
    }
}
