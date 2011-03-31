package org.esa.cci.sst.reader;

import org.esa.beam.framework.datamodel.Product;
import org.postgis.Point;

import java.awt.Rectangle;

/**
 * Strategies for creating pixel and geo boundaries for a given product. The strategies
 * are needed because AMSR-E and TMI products contain leading and trailing rows (or columns)
 * of pixels where the geo-location is invalid.
 *
 * @author Ralf Quast
 */
public interface BoundaryCalculator {

    /**
     * Returns the pixel boundary of a product. The pixel boundary shall enclose only
     * pixels where the geo-location is valid.
     *
     * @param product The product.
     *
     * @return the pixel boundary of the product supplied as argument.
     *
     * @throws Exception when the pixel boundary cannot not be calculated.
     */
    public Rectangle getPixelBoundary(Product product) throws Exception;

    /**
     * Returns the geo-boundary of a product. The geo-boundary shall enclose only
     * pixels where the geo-location is valid.
     *
     * @param product The product.
     *
     * @return the geo-boundary of the product supplied as argument.
     *
     * @throws Exception when the geo-boundary cannot not be calculated.
     */
    Point[] getGeoBoundary(Product product) throws Exception;
}
