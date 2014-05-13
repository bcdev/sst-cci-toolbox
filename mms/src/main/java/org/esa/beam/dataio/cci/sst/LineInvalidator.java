package org.esa.beam.dataio.cci.sst;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Variable;

import java.io.IOException;

/**
 * @author Ralf Quast
 */
class LineInvalidator {

    private final int leadLineSkip;
    private final int tailLineSkip;

    LineInvalidator(Variable variable, NumberInvalidator invalidator, int[] shape) throws IOException {
        int leadLineSkip = 0;
        int tailLineSkip = 0;

        try {
            final Array array = variable.read(new int[shape.length], shape);
            final int lineCount = lineCount(shape);
            for (int i = 0; i < lineCount; i++) {
                if (invalidator.isInvalid(array.getDouble(i))) {
                    leadLineSkip++;
                } else {
                    break;
                }
            }
            for (int i = lineCount; i-- > 0; ) {
                if (invalidator.isInvalid(array.getDouble(i))) {
                    tailLineSkip++;
                } else {
                    break;
                }
            }
        } catch (InvalidRangeException ignored) {
            // cannot happen
        }

        this.leadLineSkip = leadLineSkip;
        this.tailLineSkip = tailLineSkip;
    }

    private int lineCount(int[] shape) {
        for (final int dimension : shape) {
            if (dimension > 1) {
                return dimension;
            }
        }
        return 1;
    }

    int getLeadLineSkip() {
        return leadLineSkip;
    }

    int getTailLineSkip() {
        return tailLineSkip;
    }
}
