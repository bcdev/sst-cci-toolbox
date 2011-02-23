package org.esa.cci.sst.reader;

import org.esa.beam.framework.datamodel.Product;
import org.postgis.Point;

import java.io.IOException;

public interface GeoBoundaryCalculator {

    Point[] getGeoBoundary(Product product) throws IOException;
}
