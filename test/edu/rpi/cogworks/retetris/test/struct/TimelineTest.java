package edu.rpi.cogworks.retetris.test.struct;

import edu.rpi.cogworks.retetris.struct.Timeline;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TimelineTest {

    @Test
    public void testInit() {
        Long[] longs = new Long[] {0L, 1L, 2L, 3L, 4L, 5L};
        Timeline<Long> line = new Timeline<>(longs, Long::longValue);
        assertEquals("Count should be source length", longs.length, line.count());
        assertEquals("Should start with 0", (long) longs[0], line.begin());
        assertEquals("Should end with 5", (long) longs[5], line.end());
        assertEquals("Should have duration 5", 5, line.duration());

        longs = new Long[] {5L, 2L, 3L, 1L, 4L};
        line = new Timeline<>(longs, Long::longValue);
        assertEquals("Timeline length should be equal to source length", longs.length, line.count());
        assertEquals("Should start with 1", (long) longs[3], line.begin());
        assertEquals("Should end with 5", (long) longs[0], line.end());
        assertEquals("Should have duration 4", longs[0] - longs[3], line.duration());

        double delta = 0.00001;
        Double[] doubles = new Double[] {0.124, 1.1321, 123.123, 4124.124};
        Timeline<Double> doubleLine = new Timeline<>(doubles, d -> (long) (d * 1000));
        assertEquals("Timeline length should be equal to source length", doubles.length, doubleLine.count());
        assertEquals("Should start with 124", doubles[0] * 1000, doubleLine.begin(), delta);
        assertEquals("Should end with 4124124", doubles[3] * 1000, doubleLine.end(), delta);
        assertEquals("Should have duration 4124000", (doubles[3] - doubles[0]) * 1000, doubleLine.duration(), delta);
    }

    @Test
    public void testInitInvalid() {
        boolean thrown = false;
        try {
            new Timeline<>(null, Long::longValue);
        } catch (Exception e) {
            thrown = true;
        }
        assertTrue("Should not permit null element array", thrown);

        Long[] longs = new Long[] {0L, 1L, 2L, 3L, 3L, 4L, 5L};
        thrown = false;
        try {
            new Timeline<>(longs, Long::longValue);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue("Should not permit duplicate elements", thrown);

        longs = new Long[0];
        thrown = false;
        try {
            new Timeline<>(longs, Long::longValue);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue("Should not permit empty element array", thrown);
    }

    @Test
    public void testFind() {
        Long[] longs = new Long[10];
        for (int i = 0; i < longs.length; ++i)
            longs[i] = i * 1000L;

        Timeline<Long> line = new Timeline<>(longs, Long::longValue);
        for (int i = 0; i < longs.length; ++i) {
            assertEquals(i > 0? longs[i - 1] : null, line.find(i * 1000 - 1));
            assertEquals(longs[i], line.find(i * 1000));
            assertEquals(longs[i], line.find(i * 1000 + 1));
        }
    }

    @Test
    public void testFeedPlay() {
        Long[] longs = new Long[] {0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L};
        Timeline<Long> line = new Timeline<>(longs, l -> l * 1000);
        Timeline<Long>.Feed feed = line.feedFrom(0);
        assertTrue("Feed should be at start", feed.atStart());
        assertEquals("Now at 0", 0, feed.getNow());
        List<Long> empty = new ArrayList<>(0);
        List<Long> events = new LinkedList<>();
        feed.play(0, events::add);
        assertEquals("No change in time should not pass any events", empty, events);
        assertEquals("Still at 0", 0, feed.getNow());

        events.clear();
        feed.play(1000, events::add);
        assertEquals("Should pass events [0, 1000)", Arrays.asList(0L), events);
        assertEquals("Now at 1000", 1000, feed.getNow());

        events.clear();
        feed.play(0, events::add);
        assertEquals("Even though event is now, 0 advance should not pass it", empty, events);
        assertEquals("Still at 1000", 1000, feed.getNow());

        events.clear();
        feed.play(4000, events::add);
        assertEquals("Should pass events from [1000, 5000)", Arrays.asList(1L, 2L, 3L, 4L), events);
        assertEquals("Now at 5000", 5000, feed.getNow());

        events.clear();
        feed.play(3000, events::add);
        assertEquals("Should pass events from [5000, 8000)", Arrays.asList(5L, 6L, 7L), events);
        assertEquals("Now at 8000", 8000, feed.getNow());

        events.clear();
        feed.play(2000, events::add);
        assertEquals("Should pass events from [8000, 10000)", Arrays.asList(8L, 9L), events);
        assertEquals("Now at 10000", 10000, feed.getNow());

        events.clear();
        feed.play(1000, events::add);
        assertEquals("Should pass events from [10000, 11000)", Arrays.asList(10L), events);
        assertEquals("Now at 11000", 11000, feed.getNow());
        assertTrue("Should be no remaining events", feed.atEnd());
    }

    @Test
    public void testFeedBack() {
        Long[] longs = new Long[] {0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L};
        Timeline<Long> line = new Timeline<>(longs, l -> l * 1000);
        Timeline<Long>.Feed feed = line.feedFrom(11000);
        assertTrue("Feed should be at end", feed.atEnd());
        assertEquals("Now at 11000", 11000, feed.getNow());
        // Use object to permit null return
        Object last = feed.back(0);
        assertEquals("Still at 11000", 11000, feed.getNow());
        assertEquals("Last event was 10", 10L, last);

        last = feed.back(1000);
        assertEquals("Now at 10000", 10000, feed.getNow());
        assertEquals("Last event was 9", 9L, last);

        last = feed.back(999);
        assertEquals("Now at 9001", 9001, feed.getNow());
        assertEquals("Last event was 9", 9L, last);

        last = feed.back(0);
        assertEquals("Still at 9000", 9001, feed.getNow());
        assertEquals("Last event was 9", 9L, last);

        last = feed.back(4001);
        assertEquals("Now at 5000", 5000, feed.getNow());
        assertEquals("Last event was 4", 4L, last);

        last = feed.back(3500);
        assertEquals("Now at 1500", 1500, feed.getNow());
        assertEquals("Last event was 1", 1L, last);

        last = feed.back(1500);
        assertEquals("Now at 0", 0L, feed.getNow());
        assertTrue("Feed should be at start", feed.atStart());
        assertNull("There is no last event", last);
    }

    @Test
    public void testFeedDeltas() {
        Long[] longs = new Long[] {0L, 1L, 2L};
        Timeline<Long> line = new Timeline<>(longs, l -> l * 1000);
        Timeline<Long>.Feed feed = line.feedFrom(0);
        assertEquals("Should have negative last delta when no events passed", -1, feed.getLastDelta());
        assertEquals("Should have zero next delta when event is now", 0, feed.getNextDelta());

        feed.play(500, null);
        assertEquals("Now at 500", 500, feed.getNow());
        assertEquals("Last event was 500 ago", 500, feed.getLastDelta());
        assertEquals("Next event is in 500", 500, feed.getNextDelta());

        feed.play(500, null);
        assertEquals("Now at 1000", 1000, feed.getNow());
        assertEquals("Last event was 1000 ago", 1000, feed.getLastDelta());
        assertEquals("Next event is now", 0, feed.getNextDelta());

        feed.play(1, null);
        assertEquals("Now at 1001", 1001, feed.getNow());
        assertEquals("Last event was 1 ago", 1, feed.getLastDelta());
        assertEquals("Next event is in 999", 999, feed.getNextDelta());

        feed.play(1001, null);
        assertEquals("Now at 2002", 2002, feed.getNow());
        assertEquals("Last event was 2 ago", 2, feed.getLastDelta());
        assertEquals("No more events", -1, feed.getNextDelta());
    }

    @Test
    public void testFeedNegativeDeltas() {
        Timeline<Long>.Feed feed = new Timeline<>(new Long[] {0L, 1L, 2L, 3L, 4L}, Long::longValue).feedFrom(0);

        boolean thrown = false;
        try {
            assertNull("Should have not passed elements", feed.play(-1));
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue("Should throw IllegalArgumentException", thrown);
        assertEquals("Should be at 0", 0, feed.getNow());

        thrown = false;
        try {
            feed.play(-1, l -> fail("Should not scroll"));
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue("Should throw IllegalArgumentException", thrown);
        assertEquals("Should be at 0", 0, feed.getNow());

        thrown = false;
        try {
            assertNull("Should have not passed elements", feed.back(-1));
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue("Should throw IllegalArgumentException", thrown);
        assertEquals("Should be at 0", 0, feed.getNow());

        thrown = false;
        try {
            feed.play(-1, l -> fail("Should not scroll"));
        } catch (IllegalArgumentException e) {
            thrown = true;
        }

        assertTrue("Should throw IllegalArgumentException", thrown);
        assertEquals("Should be at 0", 0, feed.getNow());
    }

    @Test
    public void testFeedComprehensive() {
        Double[] times = new Double[] {.01, .012, .013, .015, .05, .1, .11, .13, .2,};
        Timeline<Double> line = new Timeline<>(times, d -> (long) (d * 1000));
        Stack<Double> stack = new Stack<>();

        Timeline<Double>.Feed feed = line.feedFrom(0);
        assertNull("Should be nothing from [0,.01]", feed.play(10));
        long time = 0;
        while (!feed.atEnd()) {
            long next = feed.getNextDelta();
            if (next < 0)
                break;
            feed.play(next, d -> fail("Should not pass any events: " + d + " @ " + (long) (d * 1000)));
            if (feed.getNow() > time)
                time = feed.getNow();
            else fail("Time should advance: " + time);
            // Need to advance one tick to pass current event
            Double passed = feed.play(1);
            assertNotNull("Event should have occurred", passed);
            stack.push(passed);
        }
        assertEquals("Should pass events in order", Arrays.asList(times), stack);

        while (!feed.atStart())
            feed.back(feed.getLastDelta(), d -> assertEquals("Should pass events in reverse", stack.pop(), d));
        assertTrue("Should be at start", feed.atStart());
        assertTrue("Should have emptied stack", stack.isEmpty());
    }
}
