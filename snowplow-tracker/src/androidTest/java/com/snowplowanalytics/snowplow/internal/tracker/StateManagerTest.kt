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
import com.snowplowanalytics.core.emitter.Emitter
import com.snowplowanalytics.core.statemachine.State
import com.snowplowanalytics.core.statemachine.StateMachineInterface
import com.snowplowanalytics.core.statemachine.StateManager
import com.snowplowanalytics.core.statemachine.TrackerState
import com.snowplowanalytics.core.tracker.*
import com.snowplowanalytics.snowplow.event.*
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow.tracker.InspectableEvent
import com.snowplowanalytics.snowplow.tracker.LogLevel
import com.snowplowanalytics.snowplow.tracker.MockEventStore
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import kotlin.collections.ArrayList

@RunWith(AndroidJUnit4::class)
class StateManagerTest {
    @Test
    fun testStateManager() {
        val stateManager = StateManager()
        stateManager.addOrReplaceStateMachine(MockStateMachine())
        val eventInc = SelfDescribing("inc", object : HashMap<String, Any?>() {
            init {
                put("value", 1)
            }
        })
        val eventDec = SelfDescribing("dec", object : HashMap<String, Any?>() {
            init {
                put("value", 2)
            }
        })
        val event = SelfDescribing("event", object : HashMap<String, Any?>() {
            init {
                put("value", 3)
            }
        })
        var trackerState = stateManager.trackerStateForProcessedEvent(eventInc)
        var mockState = trackerState.getState("identifier") as MockState?
        Assert.assertEquals(1, mockState!!.value.toLong())
        
        var e = TrackerEvent(eventInc, trackerState)
        var entities = stateManager.entitiesForProcessedEvent(e)
        var data = entities[0].map["data"] as Map<String?, Int>?
        Assert.assertEquals(1, data!!["value"]!!.toInt().toLong())
        Assert.assertTrue(stateManager.addPayloadValuesToEvent(e))
        Assert.assertNull(e.payload["newParam"])
        
        trackerState = stateManager.trackerStateForProcessedEvent(eventInc)
        mockState = trackerState.getState("identifier") as MockState?
        Assert.assertEquals(2, mockState!!.value.toLong())
        
        e = TrackerEvent(eventInc, trackerState)
        entities = stateManager.entitiesForProcessedEvent(e)
        data = entities[0].map["data"] as Map<String?, Int>?
        Assert.assertEquals(2, data!!["value"]!!.toInt().toLong())
        Assert.assertTrue(stateManager.addPayloadValuesToEvent(e))
        Assert.assertNull(e.payload["newParam"])
        
        trackerState = stateManager.trackerStateForProcessedEvent(eventDec)
        mockState = trackerState.getState("identifier") as MockState?
        Assert.assertEquals(1, mockState!!.value.toLong())
        
        e = TrackerEvent(eventDec, trackerState)
        entities = stateManager.entitiesForProcessedEvent(e)
        data = entities[0].map["data"] as Map<String?, Int>?
        Assert.assertEquals(1, data!!["value"]!!.toInt().toLong())
        Assert.assertTrue(stateManager.addPayloadValuesToEvent(e))
        Assert.assertNull(e.payload["newParam"])
        
        trackerState = stateManager.trackerStateForProcessedEvent(event)
        mockState = trackerState.getState("identifier") as MockState?
        Assert.assertEquals(1, mockState!!.value.toLong())
        
        e = TrackerEvent(event, trackerState)
        entities = stateManager.entitiesForProcessedEvent(e)
        data = entities[0].map["data"] as Map<String?, Int>?
        Assert.assertEquals(1, data!!["value"]!!.toInt().toLong())
        Assert.assertTrue(stateManager.addPayloadValuesToEvent(e))
        Assert.assertEquals("value", e.payload["newParam"])
    }

    @Test
    fun testAddRemoveStateMachine() {
        val stateManager = StateManager()
        stateManager.addOrReplaceStateMachine(MockStateMachine())
        stateManager.removeStateMachine("identifier")
        val eventInc = SelfDescribing("inc", object : HashMap<String, Any?>() {
            init {
                put("value", 1)
            }
        })
        val trackerState = stateManager.trackerStateForProcessedEvent(eventInc)
        val state = trackerState.getState("identifier")
        Assert.assertNull(state)
        val e = TrackerEvent(eventInc, trackerState)
        val entities = stateManager.entitiesForProcessedEvent(e)
        Assert.assertEquals(0, entities.size.toLong())
    }

    @Test
    @Throws(InterruptedException::class)
    fun testScreenStateMachine() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val eventStore = MockEventStore()
        val emitter = Emitter("namespace", eventStore, context, "http://snowplow-fake-url.com")
        val trackerBuilder = { tracker: Tracker ->
            tracker.screenContext = true
            tracker.sessionContext = false
            tracker.platformContextEnabled = false
            tracker.lifecycleAutotracking = false
            tracker.base64Encoded = false
            tracker.logLevel = LogLevel.VERBOSE
        }
        val tracker = Tracker(emitter, "namespace", "appId", context = context, builder = trackerBuilder)

        // Send events
        tracker.track(Timing("category", "variable", 123))
        Thread.sleep(1000)
        if (eventStore.lastInsertedRow == -1L) Assert.fail()
        var payload = eventStore.db[eventStore.lastInsertedRow]
        eventStore.removeAllEvents()
        var entities = payload!!.map["co"] as String?
        Assert.assertNull(entities)
        
        tracker.track(ScreenView("screen1"))
        Thread.sleep(1000)
        if (eventStore.lastInsertedRow == -1L) Assert.fail()
        payload = eventStore.db[eventStore.lastInsertedRow]
        eventStore.removeAllEvents()
        entities = payload!!.map["co"] as String?
        Assert.assertNotNull(entities)
        Assert.assertTrue(entities!!.contains("screen1"))
        Assert.assertEquals(
            1,
            (entities.split("screen1").dropLastWhile { it.isEmpty() }
                .toTypedArray().size - 1).toLong()
        )
        
        tracker.track(Timing("category", "variable", 123))
        Thread.sleep(1000)
        if (eventStore.lastInsertedRow == -1L) Assert.fail()
        payload = eventStore.db[eventStore.lastInsertedRow]
        eventStore.removeAllEvents()
        entities = payload!!.map["co"] as String?
        Assert.assertTrue(entities!!.contains("screen1"))
        Assert.assertEquals(
            1,
            (entities.split("screen1").dropLastWhile { it.isEmpty() }
                .toTypedArray().size - 1).toLong()
        )
        
        tracker.track(ScreenView("screen2"))
        Thread.sleep(1000)
        if (eventStore.lastInsertedRow == -1L) Assert.fail()
        payload = eventStore.db[eventStore.lastInsertedRow]
        eventStore.removeAllEvents()
        entities = payload!!.map["co"] as String?
        Assert.assertTrue(entities!!.contains("screen2"))
        Assert.assertEquals(
            1,
            (entities.split("screen2").dropLastWhile { it.isEmpty() }
                .toTypedArray().size - 1).toLong()
        )
        
        val eventPayload = payload.map["ue_pr"] as String?
        Assert.assertTrue(eventPayload!!.contains("screen1"))
        Assert.assertTrue(eventPayload.contains("screen2"))
        
        tracker.track(Timing("category", "variable", 123))
        Thread.sleep(1000)
        if (eventStore.lastInsertedRow == -1L) Assert.fail()
        payload = eventStore.db[eventStore.lastInsertedRow]
        eventStore.removeAllEvents()
        entities = payload!!.map["co"] as String?
        Assert.assertTrue(entities!!.contains("screen2"))
        Assert.assertEquals(
            1,
            (entities.split("screen2").dropLastWhile { it.isEmpty() }
                .toTypedArray().size - 1).toLong()
        )
        
        tracker.track(Timing("category", "variable", 123))
        Thread.sleep(1000)
        if (eventStore.lastInsertedRow == -1L) Assert.fail()
        payload = eventStore.db[eventStore.lastInsertedRow]
        eventStore.removeAllEvents()
        entities = payload!!.map["co"] as String?
        Assert.assertTrue(entities!!.contains("screen2"))
        Assert.assertEquals(
            1,
            (entities.split("screen2").dropLastWhile { it.isEmpty() }
                .toTypedArray().size - 1).toLong()
        )
    }

    @Test
    @Throws(InterruptedException::class)
    fun testLifecycleStateMachine() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val eventStore = MockEventStore()
        val emitter = Emitter("namespace", eventStore, context, "http://snowplow-fake-url.com")
        val trackerBuilder = { tracker: Tracker ->
            tracker.lifecycleAutotracking = true
            tracker.base64Encoded = false
            tracker.logLevel = LogLevel.VERBOSE
        }
        val tracker = Tracker(emitter, "namespace", "appId", context = context, builder = trackerBuilder)

        // Send events
        tracker.track(Timing("category", "variable", 123))
        Thread.sleep(1000)
        if (eventStore.lastInsertedRow == -1L) Assert.fail()
        var payload = eventStore.db[eventStore.lastInsertedRow]
        eventStore.removeAllEvents()
        var entities = payload!!.map["co"] as String?
        Assert.assertNotNull(entities)
        Assert.assertTrue(entities!!.contains("\"isVisible\":true"))
        Assert.assertEquals(1, (entities.split("isVisible").dropLastWhile { it.isEmpty() }
            .toTypedArray().size - 1).toLong())
        
        tracker.track(Background())
        Thread.sleep(1000)
        if (eventStore.lastInsertedRow == -1L) Assert.fail()
        payload = eventStore.db[eventStore.lastInsertedRow]
        eventStore.removeAllEvents()
        entities = payload!!.map["co"] as String?
        Assert.assertNotNull(entities)
        Assert.assertTrue(entities!!.contains("\"isVisible\":false"))
        Assert.assertEquals(1, (entities.split("isVisible").dropLastWhile { it.isEmpty() }
            .toTypedArray().size - 1).toLong())
        
        tracker.track(ScreenView("screen1"))
        Thread.sleep(1000)
        if (eventStore.lastInsertedRow == -1L) Assert.fail()
        payload = eventStore.db[eventStore.lastInsertedRow]
        eventStore.removeAllEvents()
        entities = payload!!.map["co"] as String?
        Assert.assertNotNull(entities)
        Assert.assertTrue(entities!!.contains("\"isVisible\":false"))
        Assert.assertEquals(1, (entities.split("isVisible").dropLastWhile { it.isEmpty() }
            .toTypedArray().size - 1).toLong())
        
        tracker.track(Foreground().foregroundIndex(9))
        Thread.sleep(1000)
        if (eventStore.lastInsertedRow == -1L) Assert.fail()
        payload = eventStore.db[eventStore.lastInsertedRow]
        eventStore.removeAllEvents()
        entities = payload!!.map["co"] as String?
        Assert.assertNotNull(entities)
        println(entities)
        Assert.assertTrue(entities!!.contains("\"isVisible\":true"))
        Assert.assertTrue(entities.contains("\"index\":9"))
        Assert.assertEquals(1, (entities.split("isVisible").dropLastWhile { it.isEmpty() }
            .toTypedArray().size - 1).toLong())
        
        tracker.track(ScreenView("screen1"))
        Thread.sleep(1000)
        if (eventStore.lastInsertedRow == -1L) Assert.fail()
        payload = eventStore.db[eventStore.lastInsertedRow]
        eventStore.removeAllEvents()
        entities = payload!!.map["co"] as String?
        Assert.assertNotNull(entities)
        Assert.assertTrue(entities!!.contains("\"isVisible\":true"))
        Assert.assertTrue(entities.contains("\"index\":9"))
        Assert.assertEquals(1, (entities.split("isVisible").dropLastWhile { it.isEmpty() }
            .toTypedArray().size - 1).toLong())
    }

    @Test
    @Throws(InterruptedException::class)
    fun testDeepLinkStateMachine() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val eventStore = MockEventStore()
        val emitter = Emitter("namespace", eventStore, context, "http://snowplow-fake-url.com")
        val trackerBuilder = { tracker: Tracker ->
            tracker.deepLinkContext = true
            tracker.base64Encoded = false
            tracker.sessionContext = false
            tracker.platformContextEnabled = false
            tracker.lifecycleAutotracking = false
        }
        val tracker = Tracker(emitter, "namespace", "appId", context = context, builder = trackerBuilder)

        // Send events
        tracker.track(Timing("category", "variable", 123))
        Thread.sleep(1000)
        if (eventStore.lastInsertedRow == -1L) Assert.fail()
        var payload = eventStore.db[eventStore.lastInsertedRow]
        eventStore.removeAllEvents()
        var entities = payload!!.map["co"] as String?
        Assert.assertNull(entities)
        tracker.track(DeepLinkReceived("http://www.homepage.com"))
        Thread.sleep(1000)
        if (eventStore.lastInsertedRow == -1L) Assert.fail()
        payload = eventStore.db[eventStore.lastInsertedRow]
        eventStore.removeAllEvents()
        entities = payload!!.map["co"] as String?
        Assert.assertNull(entities)
        tracker.track(ScreenView("screen1"))
        Thread.sleep(1000)
        if (eventStore.lastInsertedRow == -1L) Assert.fail()
        payload = eventStore.db[eventStore.lastInsertedRow]
        eventStore.removeAllEvents()
        entities = payload!!.map["co"] as String?
        Assert.assertNotNull(entities)
        Assert.assertTrue(entities!!.contains("www.homepage.com"))
        Assert.assertEquals(
            1,
            (entities.split("url").dropLastWhile { it.isEmpty() }.toTypedArray().size - 1).toLong()
        )
        tracker.track(Timing("category", "variable", 123))
        Thread.sleep(1000)
        if (eventStore.lastInsertedRow == -1L) Assert.fail()
        payload = eventStore.db[eventStore.lastInsertedRow]
        eventStore.removeAllEvents()
        entities = payload!!.map["co"] as String?
        Assert.assertNull(entities)
        tracker.track(ScreenView("screen2"))
        Thread.sleep(1000)
        if (eventStore.lastInsertedRow == -1L) Assert.fail()
        payload = eventStore.db[eventStore.lastInsertedRow]
        eventStore.removeAllEvents()
        entities = payload!!.map["co"] as String?
        Assert.assertNull(entities)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testAllowsMultipleStateMachines() {
        val stateManager = StateManager()
        stateManager.addOrReplaceStateMachine(MockStateMachine("identifier1"))
        stateManager.addOrReplaceStateMachine(MockStateMachine("identifier2"))
        val eventInc = SelfDescribing("inc", object : HashMap<String, Any?>() {
            init {
                put("value", 1)
            }
        })
        val trackerState = stateManager.trackerStateForProcessedEvent(eventInc)
        val e = TrackerEvent(eventInc, trackerState)
        val entities = stateManager.entitiesForProcessedEvent(e)
        Assert.assertEquals(2, entities.size.toLong())
    }

    @Test
    @Throws(InterruptedException::class)
    fun testDoesntDuplicateStateFromStateMachinesWithSameId() {
        val stateManager = StateManager()
        stateManager.addOrReplaceStateMachine(MockStateMachine())
        stateManager.addOrReplaceStateMachine(MockStateMachine())
        val eventInc = SelfDescribing("inc", object : HashMap<String, Any?>() {
            init {
                put("value", 1)
            }
        })
        val trackerState = stateManager.trackerStateForProcessedEvent(eventInc)
        val e = TrackerEvent(eventInc, trackerState)
        val entities = stateManager.entitiesForProcessedEvent(e)
        Assert.assertEquals(1, entities.size.toLong())
    }

    @Test
    fun testReplacingStateMachineDoesntResetTrackerState() {
        val stateManager = StateManager()
        stateManager.addOrReplaceStateMachine(MockStateMachine())
        stateManager.trackerStateForProcessedEvent(
            SelfDescribing(
                "inc",
                object : HashMap<String, Any?>() {
                    init {
                        put("value", 1)
                    }
                })
        )
        val state1 = stateManager.trackerState.getState("identifier")
        stateManager.addOrReplaceStateMachine(MockStateMachine())
        val state2 = stateManager.trackerState.getState("identifier")
        Assert.assertNotNull(state1)
        Assert.assertSame(state1, state2)
    }

    @Test
    fun testReplacingStateMachineWithDifferentOneResetsTrackerState() {
        class MockStateMachine1 : MockStateMachine()
        class MockStateMachine2 : MockStateMachine()

        val stateManager = StateManager()
        stateManager.addOrReplaceStateMachine(MockStateMachine1())
        stateManager.trackerStateForProcessedEvent(
            SelfDescribing(
                "inc",
                object : HashMap<String, Any?>() {
                    init {
                        put("value", 1)
                    }
                })
        )
        val state1 = stateManager.trackerState.getState("identifier")
        stateManager.addOrReplaceStateMachine(MockStateMachine2())
        val state2 = stateManager.trackerState.getState("identifier")
        Assert.assertNotNull(state1)
        Assert.assertNotSame(state1, state2)
    }

    @Test
    fun testAfterTrackCallback() {
        val stateManager = StateManager()
        val stateMachine = MockStateMachine()
        stateManager.addOrReplaceStateMachine(stateMachine)

        stateManager.afterTrack(
            TrackerEvent(
                Structured("cat", "act"),
                TrackerState()
            )
        )

        Thread.sleep(100)
        Assert.assertEquals(1, stateMachine.afterTrackEvents.size)
        Assert.assertEquals("cat", stateMachine.afterTrackEvents.first().payload["se_ca"])
    }

    @Test
    fun testFilterReturnsSettingOfStateMachine() {
        val stateManager = StateManager()
        stateManager.addOrReplaceStateMachine(MockStateMachine())

        Assert.assertFalse(stateManager.filter(TrackerEvent(SelfDescribing("s1", emptyMap()), TrackerState())))
        Assert.assertTrue(stateManager.filter(TrackerEvent(SelfDescribing("s2", emptyMap()), TrackerState())))
    }
} // Mock classes

internal class MockState(var value: Int) : State
internal open class MockStateMachine(
    override val identifier: String = "identifier"
) : StateMachineInterface {
    var afterTrackEvents: MutableList<InspectableEvent> = ArrayList()

    override val subscribedEventSchemasForEventsBefore: List<String>
        get() = emptyList()

    override val subscribedEventSchemasForTransitions: List<String>
        get() = LinkedList(listOf("inc", "dec"))

    override val subscribedEventSchemasForEntitiesGeneration: List<String>
        get() = LinkedList(listOf("*"))

    override val subscribedEventSchemasForPayloadUpdating: List<String>
        get() = LinkedList(listOf("event"))

    override val subscribedEventSchemasForAfterTrackCallback: List<String>
        get() = Collections.singletonList("*")

    override val subscribedEventSchemasForFiltering: List<String>
        get() = Collections.singletonList("s1")

    override fun eventsBefore(event: Event): List<Event>? {
        return null
    }

    override fun transition(event: Event, state: State?): State? {
        val e = event as SelfDescribing
        var currentState = state as MockState?
        if (currentState == null) {
            currentState = MockState(0)
        }
        return when (e.schema) {
            "inc" -> {
                MockState(currentState.value + 1)
            }
            "dec" -> {
                MockState(currentState.value - 1)
            }
            else -> {
                MockState(0)
            }
        }
    }

    override fun entities(event: InspectableEvent, state: State?): List<SelfDescribingJson> {
        val mockState = state as MockState?
        val sdj = SelfDescribingJson("enitity", object : HashMap<String?, Int?>() {
            init {
                put("value", mockState!!.value)
            }
        })
        return LinkedList(listOf(sdj))
    }

    override fun payloadValues(event: InspectableEvent, state: State?): Map<String, Any>? {
        return object : HashMap<String, Any>() {
            init {
                put("newParam", "value")
            }
        }
    }

    override fun afterTrack(event: InspectableEvent) {
        afterTrackEvents.add(event)
    }

    override fun filter(event: InspectableEvent, state: State?): Boolean? {
        return false
    }
}
