/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.cci.sst.util;

import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.Product;

/**
 * Implementation of {@link org.esa.cci.sst.util.SubsceneGeneratorTool.SubsceneGenerator} responsible for creating
 * and applying subscenes on data products, which can be read using the BEAM API.
 *
 * @author Thomas Storm
 */
class ProductSubsceneGenerator implements SubsceneGeneratorTool.SubsceneGenerator {

    @Override
    public ProductSubsetDef createSubsetDef() {
        return null;
    }

    @Override
    public Product createSubscene(final ProductSubsetDef subsetDef) {
        return null;
    }
}
