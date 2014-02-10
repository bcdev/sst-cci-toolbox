package org.esa.cci.sst.reader;

import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.Range;

import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assert.assertSame;

public class InSituReaderHelperTest {

    private static final double HALF_DAY_MJD = 0.5;
    private static final int HALF_DAY_SECS = 43200;

    @Test
    public void testCreateSubsampling_MJD() {
        final Array historyTimes = InsituData.createHistoryTimeArray_MJD();
        final Range r = InsituReaderHelper.findRange(historyTimes, 2455090.56, HALF_DAY_MJD);
        final List<Range> s = InsituReaderHelper.createSubsampling(historyTimes, r, 10);

        assertEquals(10, s.size());
        assertEquals(r.first(), s.get(0).first());
        assertEquals(r.first(), s.get(0).last());
        assertEquals(r.last(), s.get(9).first());
        assertEquals(r.last(), s.get(9).last());
    }

    @Test
    public void testCreateSubsampling_SECS_1978() {
        final Array historyTimes = InsituData.createHistoryTimeArray_SECS_1978();
        final Range r = InsituReaderHelper.findRange(historyTimes, 790918847, HALF_DAY_SECS);
        final List<Range> s = InsituReaderHelper.createSubsampling(historyTimes, r, 10);

        assertEquals(10, s.size());
        assertEquals(r.first(), s.get(0).first());
        assertEquals(r.first(), s.get(0).last());
        assertEquals(r.last(), s.get(9).first());
        assertEquals(r.last(), s.get(9).last());
    }

    @Test
    public void testFindRange_illegalRank() {
        final double[][] twoDims = {{1.0, 2.0}, {3.0, 4.0}};
        final Array historyTimes = Array.factory(twoDims);

        try {
            InsituReaderHelper.findRange(historyTimes, 12, HALF_DAY_MJD);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testFindRange_ForReferenceTimeInHistory_MJD() {
        final Array historyTimes = InsituData.createHistoryTimeArray_MJD();
        final double referenceTime = 2455090.56;
        final Range range = InsituReaderHelper.findRange(historyTimes, referenceTime, HALF_DAY_MJD);

        assertTrue(historyTimes.getDouble(range.first() - 1) < referenceTime - HALF_DAY_MJD);
        assertTrue(historyTimes.getDouble(range.first()) >= referenceTime - HALF_DAY_MJD);
        assertTrue(historyTimes.getDouble(range.last()) <= referenceTime + HALF_DAY_MJD);
        assertTrue(historyTimes.getDouble(range.last() + 1) > referenceTime + HALF_DAY_MJD);
    }

    @Test
    public void testFindRange_ForReferenceTimeInHistory_SECS_1978() {
        final Array historyTimes = InsituData.createHistoryTimeArray_SECS_1978();
        final double referenceTime = 790904339;
        final Range range = InsituReaderHelper.findRange(historyTimes, referenceTime, HALF_DAY_SECS);

        assertTrue(historyTimes.getDouble(range.first() - 1) < referenceTime - HALF_DAY_SECS);
        assertTrue(historyTimes.getDouble(range.first()) >= referenceTime - HALF_DAY_SECS);
        assertTrue(historyTimes.getDouble(range.last()) <= referenceTime + HALF_DAY_SECS);
        assertTrue(historyTimes.getDouble(range.last() + 1) > referenceTime + HALF_DAY_SECS);
    }

    @Test
    public void testFindRange_ForReferenceTimeAtStartOfHistory_MJD() {
        final Array historyTimes = InsituData.createHistoryTimeArray_MJD();
        final double referenceTime = 2454939.446;
        final Range range = InsituReaderHelper.findRange(historyTimes, referenceTime, HALF_DAY_MJD);

        assertTrue(historyTimes.getDouble(range.first()) == referenceTime);
        assertTrue(historyTimes.getDouble(range.last()) <= referenceTime + HALF_DAY_MJD);
        assertTrue(historyTimes.getDouble(range.last() + 1) > referenceTime + HALF_DAY_MJD);
    }

    @Test
    public void testFindRange_ForReferenceTimeAtStartOfHistory_SECS_1978() {
        final Array historyTimes = InsituData.createHistoryTimeArray_SECS_1978();
        final double referenceTime = 790323660;
        final Range range = InsituReaderHelper.findRange(historyTimes, referenceTime, HALF_DAY_SECS);

        assertTrue(historyTimes.getDouble(range.first()) == referenceTime);
        assertTrue(historyTimes.getDouble(range.last()) <= referenceTime + HALF_DAY_SECS);
        assertTrue(historyTimes.getDouble(range.last() + 1) > referenceTime + HALF_DAY_SECS);
    }

    @Test
    public void testFindRange_ForReferenceTimeAtEndOfHistory_MJD() {
        final Array historyTimes = InsituData.createHistoryTimeArray_MJD();
        final double referenceTime = 2455097.774;
        final Range range = InsituReaderHelper.findRange(historyTimes, referenceTime, HALF_DAY_MJD);

        assertTrue(historyTimes.getDouble(range.first() - 1) < referenceTime - HALF_DAY_MJD);
        assertTrue(historyTimes.getDouble(range.first()) >= referenceTime - HALF_DAY_MJD);
        assertTrue(historyTimes.getDouble(range.last()) == referenceTime);
    }

    @Test
    public void testFindRange_ForReferenceTimeAtEndOfHistory_SECS_1978() {
        final Array historyTimes = InsituData.createHistoryTimeArray_SECS_1978();
        final double referenceTime = 791596007;
        final Range range = InsituReaderHelper.findRange(historyTimes, referenceTime, HALF_DAY_SECS);

        assertTrue(historyTimes.getDouble(range.first() - 1) < referenceTime - HALF_DAY_SECS);
        assertTrue(historyTimes.getDouble(range.first()) >= referenceTime - HALF_DAY_SECS);
        assertEquals(referenceTime, historyTimes.getDouble(range.last()), 1e-8);
    }

    @Test
    public void testFindRange_ForReferenceTimeAtLowerLimit_MJD() {
        final Array historyTimes = InsituData.createHistoryTimeArray_MJD();
        final double referenceTime = 2454939.446 - HALF_DAY_MJD;
        final Range range = InsituReaderHelper.findRange(historyTimes, referenceTime, HALF_DAY_MJD);

        assertNotSame(Range.EMPTY, range);
        assertTrue(range.first() == range.last());
        assertTrue(historyTimes.getDouble(range.first()) == referenceTime + HALF_DAY_MJD);
    }

    @Test
    public void testFindRange_ForReferenceTimeAtLowerLimit_SECS_1978() {
        final Array historyTimes = InsituData.createHistoryTimeArray_SECS_1978();
        final double referenceTime = 790323660 - HALF_DAY_SECS;
        final Range range = InsituReaderHelper.findRange(historyTimes, referenceTime, HALF_DAY_SECS);

        assertNotSame(Range.EMPTY, range);
        assertTrue(range.first() == range.last());
        assertTrue(historyTimes.getDouble(range.first()) == referenceTime + HALF_DAY_SECS);
    }

    @Test
    public void testFindRange_ForReferenceTimeAtUpperLimit_MJD() {
        final Array historyTimes = InsituData.createHistoryTimeArray_MJD();
        final double referenceTime = 2455097.774 + HALF_DAY_MJD;
        final Range range = InsituReaderHelper.findRange(historyTimes, referenceTime, HALF_DAY_MJD);

        assertNotSame(Range.EMPTY, range);
        assertTrue(range.first() == range.last());
        assertTrue(historyTimes.getDouble(range.first()) == referenceTime - HALF_DAY_MJD);
    }

    @Test
    public void testFindRange_ForReferenceTimeAtUpperLimit_SECS_1978() {
        final Array historyTimes = InsituData.createHistoryTimeArray_SECS_1978();
        final double referenceTime = 791596007 + HALF_DAY_SECS;
        final Range range = InsituReaderHelper.findRange(historyTimes, referenceTime, HALF_DAY_SECS);

        assertNotSame(Range.EMPTY, range);
        assertTrue(range.first() == range.last());
        assertTrue(historyTimes.getDouble(range.first()) == referenceTime - HALF_DAY_SECS);
    }

    @Test
    public void testFindRange_ForReferenceTimeBeforeHistory_MJD() {
        final Array historyTimes = InsituData.createHistoryTimeArray_MJD();
        final double referenceTime = 2454939.446 - 2 * HALF_DAY_MJD;
        final Range range = InsituReaderHelper.findRange(historyTimes, referenceTime, HALF_DAY_MJD);

        assertSame(Range.EMPTY, range);
    }

    @Test
    public void testFindRange_ForReferenceTimeBeforeHistory_SECS_1978() {
        final Array historyTimes = InsituData.createHistoryTimeArray_SECS_1978();
        final double referenceTime = 790323660 - 2 * HALF_DAY_SECS;
        final Range range = InsituReaderHelper.findRange(historyTimes, referenceTime, HALF_DAY_MJD);

        assertSame(Range.EMPTY, range);
    }

    @Test
    public void testFindRange_ForReferenceTimeAfterHistory_MJD() {
        final Array historyTimes = InsituData.createHistoryTimeArray_MJD();
        final double referenceTime = 2455097.774 + 2 * HALF_DAY_MJD;
        final Range range = InsituReaderHelper.findRange(historyTimes, referenceTime, HALF_DAY_MJD);

        assertSame(Range.EMPTY, range);
    }

    @Test
    public void testFindRange_ForReferenceTimeAfterHistory_SECS_1978() {
        final Array historyTimes = InsituData.createHistoryTimeArray_MJD();
        final double referenceTime = 791596007 + 2 * HALF_DAY_SECS;
        final Range range = InsituReaderHelper.findRange(historyTimes, referenceTime, HALF_DAY_SECS);

        assertSame(Range.EMPTY, range);
    }
}
