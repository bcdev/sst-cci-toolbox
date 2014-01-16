package org.esa.cci.sst.common;

import java.util.Date;

/**
 * Defines an extract from a data set, e.g. a subscene of a full orbit image.
 *
 * @author Ralf Quast
 */
public interface ExtractDefinition {

    double getLat();

    double getLon();

    int getRecordNo();

    int[] getShape();

    Date getDate();

    Number getFillValue();
}
