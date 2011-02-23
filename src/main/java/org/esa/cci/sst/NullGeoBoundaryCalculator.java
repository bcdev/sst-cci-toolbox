package org.esa.cci.sst;

import org.esa.beam.framework.datamodel.Product;
import org.esa.cci.sst.reader.GeoBoundaryCalculator;
import org.postgis.Point;

import java.io.IOException;

class NullGeoBoundaryCalculator implements GeoBoundaryCalculator {

    @Override
    public Point[] getGeoBoundary(Product product) throws IOException {
        return null;
    }
}
