package edu.rpi.cogworks.retetris.struct;

import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An immutable sequence of elements ordered by the time they occur.
 * <p>
 * Time is represented as a <tt>long</tt>, although the units are not defined by this class. Clients are expected to provide a
 * method for converting elements to corresponding times.
 * <p>
 * Instances of this class provide quick searching for last element that occurs before a given time via the {@link #find(long)}
 * method. However, perhaps the most useful functionality is via the {@link #feedFrom(long)} method, which produces a
 * {@link Feed} that scrolls through the elements in chronological order.
 *
 * @param <T> The type of element that this Timeline contains.
 */
public class Timeline<T> {

    /**
     * A sorted array that contains the time of each element.
     * <p>
     * The <tt>long</tt> integral type is used to permit very fine-grained resolutions. For example, if the units are in
     * nanoseconds, the maximum time (relative to 0) is just under 1.5 centuries.
     * <p>
     * Using {@link Arrays#binarySearch(long[], long)}, the "insertion point" can be obtained, allowing the element preceding a
     * given time to be determined in logarithmic time.
     */
    private final long[] mTimes;

    /**
     * A mapping from times to the corresponding element.
     * <p>
     * This mapping facilitates element retrieval by providing a constant-time method to access an element when its time is
     * known by bypassing the binary search required to find times in {@link #mTimes}.
     */
    private final HashMap<Long, T> mTimeMap;

    /**
     * Constructs a new Timeline from the given elements, using the given {@link Function} to compute their timing.
     * <p>
     * If <tt>elements</tt> is not sorted, {@link #mTimes} will need to be sorted after creation, decreasing performance.
     *
     * @param elements The elements to be stored.
     * @param toLong   A {@link Function} to compute a time from each element.
     *
     * @throws IllegalArgumentException When <tt>elements</tt> is <tt>null</tt>, empty, or has any elements with equal times.
     */
    public Timeline(T[] elements, Function<T, Long> toLong) {
        // Ensure valid elements
        if (elements == null || elements.length == 0)
            throw new IllegalArgumentException("Empty or null element array");

        mTimes = new long[elements.length];
        mTimeMap = new HashMap<>();
        // Copy the data, checking if it needs to be sorted
        long last = Long.MIN_VALUE;
        boolean sort = false;
        for (int i = 0; i < elements.length; ++i) {
            sort |= last > (mTimes[i] = toLong.apply(elements[i]));
            if (mTimeMap.put(last = mTimes[i], elements[i]) != null)
                throw new IllegalArgumentException("Duplicate element times");
        }
        assert mTimeMap.size() == mTimes.length : "Struct size mismatch";

        // Sort if the argument wasn't already in order
        if (sort)
            Arrays.sort(mTimes);
    }

    /**
     * @return The number of elements on the timeline.
     */
    public int count() {
        return mTimes.length;
    }

    /**
     * @return The time at which the first element occurs.
     */
    public long begin() {
        // Constructor ensures we have at least one element
        return mTimes[0];
    }

    /**
     * @return The time at which the last element occurs.
     */
    public long end() {
        // Constructor ensures we have at least one element
        return mTimes[mTimes.length - 1];
    }

    /**
     * @return The amount of time spanning all of the contained elements.
     */
    public long duration() {
        return end() - begin();
    }

    /**
     * Finds the last element that has a timestamp less than or equal to the argument.
     *
     * @param time The time to find.
     *
     * @return The last element with a timestamp less than or equal to the query, or
     * <tt>null</tt> if the query is less than all elements.
     */
    public T find(long time) {
        T fetched = mTimeMap.get(time);
        if (fetched == null) {
            // No such time, so figure out the element before the requested time
            int index = indexOf(time);
            return index >= 0? mTimeMap.get(mTimes[index]) : null;
        } else return fetched;
    }

    /**
     * Constructs a {@link Feed} beginning at the given time.
     *
     * @param start The time at which to start.
     *
     * @return A new {@link Feed} at the specified time.
     * @see Feed#Feed(long)
     */
    public Feed feedFrom(long start) {
        return new Feed(start);
    }

    /**
     * Calculates the index of the last time of an element before or at the given time.
     * <p>
     * Uses a binary search to find <tt>time</tt> in {@link #mTimes}. If the time is found, its index is returned. Otherwise,
     * the index of the last element to occur before the query is returned.
     *
     * @param time A time to search for.
     *
     * @return The index of the last element's time before or at <tt>time</tt>.
     */
    private int indexOf(long time) {
        int search = Arrays.binarySearch(mTimes, time);
        if (search < 0)
            search = -(search + 1) - 1;
        return search;
    }

    /**
     * A cursor for navigating a {@link Timeline} in chronological order.
     * <p>
     * Instances provide methods for traveling forwards and backwards through the owner {@link Timeline} via
     * {@link #play(long)}, {@link #play(long, Consumer)}, {@link #back(long)}, and {@link #back(long, Consumer)}.
     *
     * @see #mCursor
     */
    public class Feed {

        /**
         * The current time position in the owner {@link Timeline}.
         * <p>
         * Any element with a lesser time has been passed, and any element with a time greater or equal has yet to occur.
         */
        private long mTime;
        /**
         * The position in {@link #mTimes} of the next element that has yet to occur.
         * <p>
         * This element's corresponding time is greater than or equal to {@link #mTime}.
         * <p>
         * The cursor is exclusive: the time at which the {@link Feed} is positioned is not considered to have occurred.
         * For example, consider a {@link Feed} that is positioned at time <tt>0</tt>, with an element which has a time of
         * <tt>10</tt>. Playing forward with a delta of <tt>10</tt> will not "pass" the element. However, if instead a delta of
         * <tt>11</tt> is used, then the element will be considered to have occurred.
         */
        private int mCursor;

        /**
         * Constructs a {@link Feed} positioned at the given time.
         * <p>
         * If an element occurs at <tt>time</tt>, it is not considered to have occurred, since {@link #mCursor} is exclusive.
         * Note that this method does use {@link #indexOf(long)} to find {@link #mCursor}, which incurs a binary search.
         *
         * @param time The time at which the {@link Feed} should be initially positioned.
         */
        private Feed(long time) {
            mTime = time;
            mCursor = indexOf(time);
            if (!mTimeMap.containsKey(mTime))
                // If there is no element at the time, then indexOf(long) returns the one before the time: we want the one after
                mCursor += 1;

            assert mCursor >= 0 && mCursor <= count();
        }

        /**
         * @return <tt>true</tt> if there are no further elements, <tt>false</tt> if elements have yet to occur.
         */
        public boolean atEnd() {
            return mCursor >= count();
        }

        /**
         * @return <tt>true</tt> if no elements have yet occurred, <tt>false</tt> if at least one has.
         */
        public boolean atStart() {
            return mCursor == 0;
        }

        /**
         * @return The time that the {@link Feed} is currently positioned at.
         */
        public long getNow() {
            return mTime;
        }

        /**
         * @return The amount of time that has passed since the previous element, or <tt>-1</tt> if no elements have passed.
         */
        public long getLastDelta() {
            if (atStart())
                return -1;
            else return mTime - mTimes[mCursor - 1];
        }

        /**
         * @return The amount of time before the next element occurs, or <tt>-1</tt> if there are no more elements.
         */
        public long getNextDelta() {
            if (atEnd())
                return -1;
            else return mTimes[mCursor] - mTime;
        }

        /**
         * Scrolls {@link #mCursor} forward along the elements, submitting each one as it is passed to <tt>callback</tt>.
         *
         * @param delta    The amount of time to move forward along the {@link Timeline}.
         * @param callback A {@link Consumer} that, if non-null, accepts each element as it occurs.
         *
         * @throws IllegalArgumentException if <tt>delta</tt> is negative.
         */
        public void play(long delta, Consumer<T> callback) {
            if (delta < 0)
                throw new IllegalArgumentException("Negative delta: " + delta);

            final long end = mTime + delta;

            long t;
            while (mCursor < count() && (t = mTimes[mCursor]) < end) {
                ++mCursor;
                if (callback != null)
                    callback.accept(mTimeMap.get(t));
            }

            mTime = end;
            assert atEnd() || mTimes[mCursor] >= mTime;
        }

        /**
         * Scrolls {@link #mCursor} forward along the elements, returning the last one to occur.
         * <p>
         * Note that even if no elements are passed during the delta, the last element to have occurred previously is returned.
         *
         * @param delta The amount of time to move forward along the {@link Timeline}.
         *
         * @return The last element to have occurred before the {@link Feed}'s resulting position, or <tt>null</tt> if the
         * position is before the first element.
         */
        public T play(long delta) {
            play(delta, null);
            if (!atStart())
                return mTimeMap.get(mTimes[mCursor - 1]);
            else return null;
        }

        /**
         * Scrolls {@link #mCursor} backwards along the elements, submitting each one as it is passed to <tt>callback</tt>.
         * <p>
         * Since {@link #mCursor} is exclusive, elements are not submitted until {@link #mCursor} is strictly less than its time.
         *
         * @param delta    The amount of time to move backward along the {@link Timeline}.
         * @param callback A {@link Consumer} that, if non-null, accepts each element as it occurs.
         */
        public void back(long delta, Consumer<T> callback) {
            if (delta < 0)
                throw new IllegalArgumentException("Negative delta: " + delta);

            final long end = mTime - delta;
            long t;
            while (mCursor > 0 && (t = mTimes[mCursor - 1]) >= end) {
                if (callback != null)
                    callback.accept(mTimeMap.get(t));
                --mCursor;
            }

            mTime = end;
            assert atStart() || mTimes[mCursor - 1] < mTime;
        }

        /**
         * Scrolls {@link #mCursor} backward along the elements, returning the last element to have occurred after changing the
         * {@link Feed}'s position.
         * <p>
         * Note that even if no elements are passed during the delta, the last element to have occurred previously is returned.
         *
         * @param delta The amount of time to move backward along the {@link Timeline}.
         *
         * @return The last element to have occurred before the {@link Feed}'s resulting position, or <tt>null</tt> if the
         * position is before the first element.
         */
        public T back(long delta) {
            back(delta, null);
            if (!atStart())
                return mTimeMap.get(mTimes[mCursor - 1]);
            else return null;
        }
    }
}
