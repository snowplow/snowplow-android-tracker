package com.snowplowanalytics.snowplow.internal.tracker;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.snowplowanalytics.snowplow.Snowplow;
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration;
import com.snowplowanalytics.snowplow.controller.TrackerController;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class MultipleInstancesTest {

    @Before
    public void setUp() {
        Snowplow.removeAllTrackers();
    }

    @After
    public void tearDown() {
        Snowplow.removeAllTrackers();
    }

    @Test
    public void testSingleInstanceIsReconfigurable() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        TrackerController t1 = Snowplow.createTracker(context,"t1", new NetworkConfiguration("snowplowanalytics.fake"));
        assertEquals("https://snowplowanalytics.fake/com.snowplowanalytics.snowplow/tp2", t1.getNetwork().getEndpoint());
        TrackerController t2 = Snowplow.createTracker(context,"t1", new NetworkConfiguration("snowplowanalytics.fake2"));
        assertEquals("https://snowplowanalytics.fake2/com.snowplowanalytics.snowplow/tp2", t2.getNetwork().getEndpoint());
        assertEquals(Set.of("t1"), Snowplow.getInstancedTrackerNamespaces());
        assertEquals(t1, t2);
    }

    @Test
    public void testMultipleInstances() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        TrackerController t1 = Snowplow.createTracker(context,"t1", new NetworkConfiguration("snowplowanalytics.fake"));
        assertEquals("https://snowplowanalytics.fake/com.snowplowanalytics.snowplow/tp2", t1.getNetwork().getEndpoint());
        TrackerController t2 = Snowplow.createTracker(context,"t2", new NetworkConfiguration("snowplowanalytics.fake2"));
        assertEquals("https://snowplowanalytics.fake2/com.snowplowanalytics.snowplow/tp2", t2.getNetwork().getEndpoint());
        assertEquals(Set.of("t1", "t2"), Snowplow.getInstancedTrackerNamespaces());
        assertNotEquals(t1, t2);
    }

    @Test
    public void testDefaultTracker() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        TrackerController t1 = Snowplow.createTracker(context,"t1", new NetworkConfiguration("snowplowanalytics.fake"));
        TrackerController t2 = Snowplow.createTracker(context,"t2", new NetworkConfiguration("snowplowanalytics.fake2"));
        TrackerController td = Snowplow.getDefaultTracker();
        assertEquals(t1, td);
    }

    @Test
    public void testUpdateDefaultTracker() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        TrackerController t1 = Snowplow.createTracker(context,"t1", new NetworkConfiguration("snowplowanalytics.fake"));
        TrackerController t2 = Snowplow.createTracker(context,"t2", new NetworkConfiguration("snowplowanalytics.fake2"));
        Snowplow.setTrackerAsDefault(t2);
        TrackerController td = Snowplow.getDefaultTracker();
        assertEquals(t2, td);
    }

    @Test
    public void testRemoveTracker() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        TrackerController t1 = Snowplow.createTracker(context,"t1", new NetworkConfiguration("snowplowanalytics.fake"));
        TrackerController t2 = Snowplow.createTracker(context,"t2", new NetworkConfiguration("snowplowanalytics.fake2"));
        Snowplow.removeTracker(t1);
        assertNotNull(t2);
        assertEquals(Set.of("t2"), Snowplow.getInstancedTrackerNamespaces());
    }

    @Test
    public void testRecreateTrackerWhichWasRemovedWithSameNamespace() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        TrackerController t1 = Snowplow.createTracker(context,"t1", new NetworkConfiguration("snowplowanalytics.fake"));
        Snowplow.removeTracker(t1);
        TrackerController t2 = Snowplow.createTracker(context,"t1", new NetworkConfiguration("snowplowanalytics.fake2"));
        assertNotEquals(t1, t2);
        assertNotNull(t2);
        assertEquals(Set.of("t1"), Snowplow.getInstancedTrackerNamespaces());
    }

    @Test
    public void testRemoveDefaultTracker() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        TrackerController t1 = Snowplow.createTracker(context,"t1", new NetworkConfiguration("snowplowanalytics.fake"));
        Snowplow.removeTracker(t1);
        TrackerController td = Snowplow.getDefaultTracker();
        assertNull(td);
        assertEquals(Set.of(), Snowplow.getInstancedTrackerNamespaces());
    }

    @Test
    public void testRemoveAllTrackers() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        TrackerController t1 = Snowplow.createTracker(context,"t1", new NetworkConfiguration("snowplowanalytics.fake"));
        TrackerController t2 = Snowplow.createTracker(context,"t2", new NetworkConfiguration("snowplowanalytics.fake2"));
        Snowplow.removeAllTrackers();
        assertEquals(Set.of(), Snowplow.getInstancedTrackerNamespaces());
    }
}
