/*
 * SST_cci Tools
 *
 * Copyright (C) 2011-2013 by Brockmann Consult GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.esa.cci.sst.common.file;

import org.esa.beam.framework.datamodel.PixelGeoCoding2;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.cci.sst.common.cellgrid.GridDef;

import javax.media.jai.JAI;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;

import static org.esa.beam.gpf.operators.standard.MosaicOp.Variable;

/**
 * For doing a re-projection of full-orbit AVHRR L2P products.
 *
 * @author Bettina Scholze
 * @author Sabine Embacher
 * @author Ralf Quast
 */
class Projector {

    private static final String MASK_EXPRESSION = "lat >= -90.0 && lat <= 90.0 && lon >= -180.0 && lon <= 180.0";

    private final GridDef gridDef;

    static {
        JAI.getDefaultInstance().getTileCache().setMemoryCapacity(536870912);
        GPF.getDefaultInstance().getOperatorSpiRegistry().loadOperatorSpis();
    }

    Projector(GridDef gridDef) {
        this.gridDef = gridDef;
    }

    Product createProjection(Product sourceProduct, String... bandNames) {
        sourceProduct.getGeoCoding().dispose();
        sourceProduct.setGeoCoding(null);
        sourceProduct.setGeoCoding(new PixelGeoCoding2(sourceProduct.getBand("lat"),
                                                       sourceProduct.getBand("lon"),
                                                       MASK_EXPRESSION));

        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("crs", "EPSG:4326");
        parameters.put("resampling", "Nearest");
        parameters.put("referencePixelX", 0.0);
        parameters.put("referencePixelY", 0.0);
        parameters.put("northing", 90.0);
        parameters.put("easting", -180.0);
        parameters.put("pixelSizeX", gridDef.getResolutionX());
        parameters.put("pixelSizeY", gridDef.getResolutionY());
        parameters.put("width", gridDef.getWidth());
        parameters.put("height", gridDef.getHeight());

        return GPF.createProduct("Reproject", parameters, sourceProduct);
    }

    // AVHRR L2P products are self-overlapping, so there may be the need to create a mosaic of subsets
    private Product createMosaic(Product[] sourceProducts, String... bandNames) {
        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("crs", "EPSG:4326");
        parameters.put("resampling", "Nearest");
        parameters.put("northBound", 90.0);
        parameters.put("southBound", -90.0);
        parameters.put("westBound", -180.0);
        parameters.put("eastBound", 180.0);
        parameters.put("pixelSizeX", gridDef.getResolutionX());
        parameters.put("pixelSizeY", gridDef.getResolutionY());
        final Variable[] variables = new Variable[bandNames.length];
        for (int i = 0; i < bandNames.length; i++) {
            variables[i] = new Variable(bandNames[i], bandNames[i]);
        }
        parameters.put("variables", variables);

        return GPF.createProduct("Mosaic", parameters, sourceProducts);
    }

    // AVHRR L2P products are self-overlapping, so there may be the need to create a mosaic of subsets
    private static Product createSubset(Product sourceProduct, Rectangle rectangle) {
        final Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("region", rectangle);

        return GPF.createProduct("Subset", parameters, sourceProduct);
    }
}
