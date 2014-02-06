package org.esa.cci.sst.reader;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class InsituReaderHelper {

    static List<Range> createSubsampling(Array historyTimes, Range range, int maxLength) {
        try {
            final List<Range> subsampling = new ArrayList<>();
            if (range.length() > maxLength) {
                subsampling.add(new Range(range.first(), range.first()));
                // get maxLength-2 entries from the history
                final double startTime = historyTimes.getDouble(range.first());
                final double endTime = historyTimes.getDouble(range.last());
                final double timeStep = (endTime - startTime) / (maxLength - 1);
                for (int i = range.first() + 1; i < range.last(); i++) {
                    if (historyTimes.getDouble(i) >= startTime + subsampling.size() * timeStep) {
                        if (subsampling.size() < maxLength - 1) {
                            subsampling.add(new Range(i, i));
                        }
                    }
                }
                subsampling.add(new Range(range.last(), range.last()));
            } else { // no subset needed
                subsampling.add(range);
            }
            return subsampling;
        } catch (InvalidRangeException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Returns the range of the in-situ history time data that falls within ±12 hours of a
     * given reference time.
     *
     * @param historyTimes  The in-situ history time data (JD). The array must be of rank 1 and
     *                      its elements must be sorted in ascending order.
     * @param referenceTime The reference time (JD).
     * @return the range of the in-situ history time data that falls within ±12 hours of the
     * given reference time.
     * @throws IllegalArgumentException when {@code historyTimes.getRank() != 1}.
     */
    static Range findRange(Array historyTimes, double referenceTime, double timeDelta) {
        if (historyTimes.getRank() != 1) {
            throw new IllegalArgumentException("history.getRank() != 1");
        }
        if (referenceTime + timeDelta < historyTimes.getDouble(0)) {
            return Range.EMPTY;
        }
        final int historyLength = historyTimes.getIndexPrivate().getShape(0);
        if (referenceTime - timeDelta > historyTimes.getDouble(historyLength - 1)) {
            return Range.EMPTY;
        }
        int startIndex = -1;
        int endIndex = -1;
        for (int i = 0; i < historyLength; i++) {
            final double time = historyTimes.getDouble(i);
            if (startIndex == -1) {
                if (time >= referenceTime - timeDelta) {
                    startIndex = i;
                    endIndex = startIndex;
                }
            } else {
                if (time <= referenceTime + timeDelta) {
                    endIndex = i;
                } else {
                    break;
                }
            }
        }
        try {
            return new Range(startIndex, endIndex);
        } catch (InvalidRangeException e) {
            return Range.EMPTY;
        }
    }
}
