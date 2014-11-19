package org.esa.cci.sst.util;

import org.junit.Before;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.*;

public class CacheTest {

    private static final int CAPACITY = 6;
    private Cache<Integer, Double> cache;

    @Before
    public void setUp() {
        cache = new Cache<>(CAPACITY);
    }

    @Test
    public void testCreate_isEmpty() {
        for (int i = 0; i < CAPACITY; i++) {
            assertFalse(cache.contains(i));
        }
    }

    @Test
    public void testAddAndContains() {
        cache.add(18, 22.089);

        assertTrue(cache.contains(18));

        assertFalse(cache.contains(105));
        assertFalse(cache.contains(-22));
    }

    @Test
    public void testAddAndGet() {
        cache.add(19, 22.089);
        cache.add(107, 11.113);

        Double cached = cache.get(19);
        assertEquals(22.089, cached, 1e-8);

        cached = cache.get(107);
        assertEquals(11.113, cached, 1e-8);
    }

    @Test
    public void testAdd_returnsNullWhenWithinCapacity() {
        assertNull(cache.add(19, 22.089));
        assertNull(cache.add(-6, 107.543));
    }

    @Test
    public void testAddAndRemove() {
        cache.add(20, 22.089);
        cache.add(11, 11.113);

        assertEquals(22.089, cache.get(20), 1e-8);
        assertEquals(11.113, cache.get(11), 1e-8);

        cache.remove(11);
        assertFalse(cache.contains(11));
        assertEquals(22.089, cache.get(20), 1e-8);

        cache.remove(20);
        assertFalse(cache.contains(11));
        assertFalse(cache.contains(20));
    }

    @Test
    public void testGetWithInvalidKeyThrows() {
        try {
            cache.get(24);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testAddBeyondCapacityRemovesOldestEntry() {
        cache.add(0, 0.0);
        cache.add(1, 0.0);
        cache.add(2, 0.0);
        cache.add(3, 0.0);
        cache.add(4, 0.0);
        cache.add(5, 0.0);

        assertTrue(cache.contains(0));

        cache.add(6, 0.0);

        assertFalse(cache.contains(0));
        assertTrue(cache.contains(1));
        assertTrue(cache.contains(6));

        cache.add(7, 0.0);

        assertFalse(cache.contains(0));
        assertFalse(cache.contains(1));
        assertTrue(cache.contains(7));
    }

    @Test
    public void testAddBeyondCapacity_returnsRemovedEntry() {
        cache.add(0, 0.0);
        cache.add(1, 1.0);
        cache.add(2, 2.0);
        cache.add(3, 3.0);
        cache.add(4, 4.0);
        cache.add(5, 5.0);

        final Double removed = cache.add(6, 6.0);
        assertNotNull(removed);
        assertEquals(0.0, removed, 1e-8);
    }

    @Test
    public void testClear() {
        cache.add(25, 25.0);
        cache.add(26, 26.0);

        assertTrue(cache.contains(25));
        assertTrue(cache.contains(26));

        cache.clear();

        assertFalse(cache.contains(25));
        assertFalse(cache.contains(26));
    }

    @Test
    public void testClear_returnsCollectionWithRemovedItems() {
        cache.add(25, 25.0);
        cache.add(26, 26.0);

        final Collection<Double> clearedItems = cache.clear();
        assertNotNull(clearedItems);
        assertEquals(2, clearedItems.size());
    }
}
