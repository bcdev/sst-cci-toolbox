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

import com.bc.ceres.core.Assert;
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.*;
import org.esa.cci.sst.Constants;
import org.esa.cci.sst.orm.PersistenceManager;
import org.postgis.*;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFileWriteable;

import javax.persistence.Query;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Abstract implementation of {@link org.esa.cci.sst.util.SubsceneGeneratorTool.SubsceneGenerator} responsible for
 * creating and applying subscenes on data products, which can be read using the BEAM API.
 * Sensor-specific subclasses of this class need to provide the dimension size that shall be used for the subscene.
 * <p/>
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
abstract class ProductSubsceneGenerator extends AbstractSubsceneGenerator {

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

    private final String sensorName;

    ProductSubsceneGenerator(PersistenceManager persistenceManager, String sensorName) {
        super(persistenceManager);
        this.sensorName = sensorName;
    }

    @Override
    public void createSubscene(SubsceneGeneratorTool.SubsceneIO subsceneIO) throws IOException {
        PersistenceManager persistenceManager = getPersistenceManager();
        String inputFilename = subsceneIO.getInputFilename();
        String outputFilename = subsceneIO.getOutputFilename();
        Product product = ProductIO.readProduct(inputFilename);
        Date time = getTimeStamp(product);
        org.esa.beam.framework.datamodel.GeoCoding geoCoding = product.getGeoCoding();
        org.esa.beam.framework.datamodel.GeoPos upperLeft = new org.esa.beam.framework.datamodel.GeoPos();
        org.esa.beam.framework.datamodel.GeoPos lowerRight = new org.esa.beam.framework.datamodel.GeoPos();
        geoCoding.getGeoPos(new org.esa.beam.framework.datamodel.PixelPos(0, 0), upperLeft);
        geoCoding.getGeoPos(new org.esa.beam.framework.datamodel.PixelPos(product.getSceneRasterWidth(), product.getSceneRasterHeight()), lowerRight);
        NetcdfFileWriteable ncFile = null;
        try {
            persistenceManager.transaction();
            List<Integer> matchupIds = getMatchupIds(sensorName, time, createRegion(upperLeft, lowerRight));
            ncFile = createNcFile(outputFilename, product, matchupIds);
            for (Band band : product.getBands()) {
                writeBand(ncFile, band, matchupIds);
            }
        } catch (InvalidRangeException e) {
            throw new IOException("Error writing to netcdf file.", e);
        } finally {
            persistenceManager.commit();
            if (ncFile != null) {
                ncFile.close();
            }
        }
    }

    PGgeometry createRegion(GeoPos upperLeft, GeoPos lowerRight) {
        Point[] points = new Point[4];
        points[0] = new Point(upperLeft.lat, upperLeft.lon);
        points[1] = new Point(upperLeft.lat, lowerRight.lon);
        points[2] = new Point(lowerRight.lat, lowerRight.lon);
        points[3] = new Point(lowerRight.lat, upperLeft.lon);
        return new PGgeometry(new MultiPoint(points));
    }

    void writeBand(NetcdfFileWriteable ncFile, Band band, List<Integer> matchupIds) throws IOException, InvalidRangeException {
        for (int matchupId : matchupIds) {
            int[] shape = createShape(matchupId, createBounds(matchupId, band));
            Array values = Array.factory(DataTypeUtils.getNetcdfDataType(band), shape);
            values.setInt(0, matchupId);
            ncFile.write(band.getName(), values);
        }
    }

    Rectangle createBounds(int matchupId, Band band) {
        // todo - ts - implement database query: get point of matchup
        // then get dimension size for sensor
        // then verschneide with band's bounds

        return null;  //To change body of created methods use File | Settings | File Templates.
    }

    int[] createShape(int matchupId, Rectangle bounds) {
        int[] shape = new int[3];
        shape[0] = matchupId;
        shape[1] = (int) bounds.getWidth();
        shape[2] = (int) bounds.getHeight();
        return shape;
    }

    @SuppressWarnings({"ConstantConditions"})
    Date getTimeStamp(Product product) {
        ProductData.UTC startTime = product.getStartTime();
        Assert.argument(startTime != null, "Product '" + product + "' has no start time.");
        ProductData.UTC endTime = product.getEndTime();
        if (endTime == null) {
            return startTime.getAsDate();
        }
        return new Date((startTime.getAsDate().getTime() + endTime.getAsDate().getTime()) / 2);
    }

    NetcdfFileWriteable createNcFile(String outputFilename, Product product, List<Integer> matchupIds) throws IOException {
        NetcdfFileWriteable ncFile = NetcdfFileWriteable.createNew(outputFilename);
        Group rootGroup = ncFile.getRootGroup();
        ncFile.addDimension(rootGroup, new Dimension(Constants.DIMENSION_NAME_MATCHUP, matchupIds.size(), true));
        ncFile.addDimension(rootGroup, new Dimension("ni", getSensorDimensionSize(), true));
        ncFile.addDimension(rootGroup, new Dimension("nj", getSensorDimensionSize(), true));
        for (Band band : product.getBands()) {
            String dimString = Constants.DIMENSION_NAME_MATCHUP + " ni nj";
            ncFile.addVariable(rootGroup, band.getName(), DataTypeUtils.getNetcdfDataType(band), dimString);
        }
        for (MetadataElement element : product.getMetadataRoot().getElements()) {
            for (MetadataAttribute attribute : element.getAttributes()) {
                ProductData data = attribute.getData();
                ncFile.addGlobalAttribute(element.getName(), data.getElemString());
            }
        }
        ncFile.create();
        return ncFile;
    }

    @SuppressWarnings({"unchecked"})
    List<Integer> getMatchupIds(String sensorName, Date time, PGgeometry bounds) {
        Query query = getPersistenceManager().createQuery(String.format(GET_MATCHUP_IDS, sensorName, time, time, bounds));
        return query.getResultList();
    }
}
