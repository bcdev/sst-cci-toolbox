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

import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.*;
import org.esa.cci.sst.Constants;
import org.esa.cci.sst.orm.PersistenceManager;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFileWriteable;

import javax.persistence.Query;
import java.awt.*;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.List;

/**
 * Implementation of {@link org.esa.cci.sst.util.SubsceneGeneratorTool.SubsceneGenerator} responsible for creating
 * and applying subscenes on data products, which can be read using the BEAM API.
 * <p/>
 * <ol>
 * <li>Get geographic boundaries from<code>f</code>
 * <li>Get time stamp from <code>f</code>
 * <li>Perform database query for matchup files with fitting geo boundaries and time
 * <li>Get matchup id from query
 * <li>Create new Netcdf file, add all variables from input product
 * <li>Set dimensions to shape given by subscene definition
 * <li>Add matchup dimension
 * <li>Copy values from input product to netcdf
 * </ol>
 *
 * @author Thomas Storm
 */
class ProductSubsceneGenerator extends AbstractSubsceneGenerator {

    /**
     * all matchup-ids for matchups with a reference observation o, which has
     * - a location within the product bounds
     * - a time point within 24 hours of the product's time point
     */
    private static final String GET_MATCHUP_IDS =
            "select m.id"
                    + " from Matchup m"
                    + " where m.oref.sensor = %s"
                    + " and m.oref.time >= %s - '12:00:00' and m.oref.time < %s + '12:00:00'"
                    + " and st_intersects(%s, oref.point)";

    ProductSubsceneGenerator(PersistenceManager persistenceManager) {
        super(persistenceManager);
    }

    @Override
    public void createSubscene(SubsceneGeneratorTool.SubsceneIO subsceneIO) throws IOException {
        PersistenceManager persistenceManager = getPersistenceManager();
        try {
            String inputFilename = subsceneIO.getInputFilename();
            String outputFilename = subsceneIO.getOutputFilename();
            Product product = ProductIO.readProduct(inputFilename);
            GeoCoding geoCoding = product.getGeoCoding();
            GeoPos upperLeft = new GeoPos();
            GeoPos lowerRight = new GeoPos();
            geoCoding.getGeoPos(new PixelPos(0, 0), upperLeft);
            geoCoding.getGeoPos(new PixelPos(product.getSceneRasterWidth(), product.getSceneRasterHeight()), lowerRight);
            persistenceManager.transaction();
            List<Integer> matchupIds = getMatchupIds("sensorname", "timeString", "boundsString");
            NetcdfFileWriteable ncFile = createNcFile(outputFilename, product);


            Product productSubset = product.createSubset(createSubsetDef(null), outputFilename, "Subscene of product '" + product.getName() + "'");
        } finally {
            persistenceManager.commit();
        }
    }

    NetcdfFileWriteable createNcFile(String outputFilename, Product product) throws IOException {
        NetcdfFileWriteable ncFile = NetcdfFileWriteable.createNew(outputFilename);
        Group rootGroup = ncFile.getRootGroup();
        ncFile.addDimension(rootGroup, new Dimension(Constants.DIMENSION_NAME_MATCHUP, 1, true));
        ncFile.addDimension(rootGroup, new Dimension("ni", product.getSceneRasterWidth(), true));
        ncFile.addDimension(rootGroup, new Dimension("nj", product.getSceneRasterHeight(), true));
        for (Band band : product.getBands()) {
            String dimString = Constants.DIMENSION_NAME_MATCHUP + " ni nj";
            ncFile.addVariable(rootGroup, band.getName(), DataTypeUtils.getNetcdfDataType(band), dimString);
        }
        for (MetadataElement element : product.getMetadataRoot().getElements()) {
            for (MetadataAttribute attribute : element.getAttributes()) {
                ProductData data = attribute.getData();
                int type = data.getType();
//            if(type == ProductData.TYPE_ASCII || type == ProductData.TYPE_UNDEFINED) {
                ncFile.addGlobalAttribute(element.getName(), data.getElemString());
//            }
            }
        }
        ncFile.create();
        return ncFile;
    }

    @SuppressWarnings({"unchecked"})
    List<Integer> getMatchupIds(String sensorName, String time, String bounds) {
        Query query = getPersistenceManager().createQuery(String.format(GET_MATCHUP_IDS, sensorName, time, time, bounds));
        return query.getResultList();
    }

    private ProductSubsetDef createSubsetDef(Point2D point) {
        ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.setRegion(createRegion(point));
        return subsetDef;
    }

    private Rectangle createRegion(Point2D subsetDef) {
        return null;
    }
}
