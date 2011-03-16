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
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.cci.sst.Constants;
import org.esa.cci.sst.orm.PersistenceManager;
import org.postgis.LinearRing;
import org.postgis.PGgeometry;
import org.postgis.Point;
import org.postgis.Polygon;
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
     * all matchup-ids for matchups with a reference observation oref, which has
     * - a time point within 24 hours of the product's time point
     * - a location within the product bounds
     */
    private static final String GET_MATCHUP_IDS =
            "select m.id"
            + " from mm_matchup m, mm_observation oref"
            + " where m.refobs_id = oref.id"
            + " and oref.sensor = ?"
            + " and oref.time >= ? and oref.time < ?"
            + " and ST_intersects(geometry(?), oref.point)";

    /**
     * the reference point for a given matchup id
     */
    private static final String GET_POINT_FOR_MATCHUP =
            "select oref.point"
            + " from mm_matchup m, mm_observation oref"
            + " where m.id = ?"
            + " and m.refobs_id = oref.id";


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
        Date productsTime = getTimeStamp(product);
        GeoCoding geoCoding = product.getGeoCoding();
        GeoPos upperLeft = new GeoPos();
        GeoPos lowerRight = new GeoPos();
        geoCoding.getGeoPos(new PixelPos(0, 0), upperLeft);
        geoCoding.getGeoPos(new PixelPos(product.getSceneRasterWidth(), product.getSceneRasterHeight()), lowerRight);
        NetcdfFileWriteable ncFile = null;
        try {
            List<Integer> matchupIds = getMatchupIds(sensorName, productsTime, createRegion(upperLeft, lowerRight));
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

    String createRegion(GeoPos upperLeft, GeoPos lowerRight) {
        final LinearRing[] rings = new LinearRing[]{
                new LinearRing(new Point[]{
                        new Point(upperLeft.lat, upperLeft.lon),
                        new Point(upperLeft.lat, lowerRight.lon),
                        new Point(lowerRight.lat, lowerRight.lon),
                        new Point(lowerRight.lat, upperLeft.lon),
                        new Point(upperLeft.lat, upperLeft.lon)
                })
        };
        return new Polygon(rings).toString();
    }

    void writeBand(NetcdfFileWriteable ncFile, Band band, List<Integer> matchupIds) throws IOException,
                                                                                           InvalidRangeException {
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

        final Point result = getPoint(matchupId);

        return null;
    }

    Point getPoint(final int matchupId) {
        final Query query = getPersistenceManager().createNativeQuery(GET_POINT_FOR_MATCHUP);
        query.setParameter(1, matchupId);
        try {
            String queryResult = query.getSingleResult().toString();
            return (Point) PGgeometry.geomFromString(queryResult);
        } catch (Exception e) {
            throw new IllegalStateException("No point for matchup-id '" + matchupId + "'.", e);
        }
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

    NetcdfFileWriteable createNcFile(String outputFilename, Product product, List<Integer> matchupIds) throws
                                                                                                       IOException {
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
    List<Integer> getMatchupIds(String sensorName, Date productsTime, String bounds) {
        getPersistenceManager().transaction();
        Query query = createQuery(sensorName, productsTime, bounds);
        final List<Integer> resultList = query.getResultList();
        getPersistenceManager().commit();
        return resultList;
    }

    Query createQuery(final String sensorName, final Date productsTime, final String bounds) {
        final long time = productsTime.getTime();
        final long twelveHours = 12 * 60 * 60 * 1000;
        final Date minDate = new Date(time - twelveHours);
        final Date maxDate = new Date(time + twelveHours);
        final String queryString = String.format(GET_MATCHUP_IDS);
        Query query = getPersistenceManager().createNativeQuery(queryString);
        query.setParameter(1, sensorName);
        query.setParameter(2, minDate);
        query.setParameter(3, maxDate);
        query.setParameter(4, bounds);
        return query;
    }

}
