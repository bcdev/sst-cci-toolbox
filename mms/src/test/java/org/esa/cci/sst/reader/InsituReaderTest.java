/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.cci.sst.reader;

import org.esa.cci.sst.common.ExtractDefinition;
import org.esa.cci.sst.common.ExtractDefinitionBuilder;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.InsituObservation;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.TimeUtil;
import org.junit.Test;
import org.postgis.Geometry;
import org.postgis.LineString;
import org.postgis.PGgeometry;
import org.postgis.Point;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class InsituReaderTest {

    @Test
    public void testReadObservation_SST_CCI_V1_Data() throws Exception {
        final InsituObservation observation;

        try (InsituReader reader = createReader("insitu_WMOID_11851_20071123_20080111.nc")) {
            assertEquals(1, reader.getNumRecords());
            observation = reader.readObservation(0);

            assertCorrectDate(2007, 11, 18, observation.getTime().getTime());
            assertEquals(2125828.8, observation.getTimeRadius(), 0.0);

            final PGgeometry location = observation.getLocation();
            assertNotNull(location);

            final Geometry geometry = location.getGeometry();
            assertThat(geometry, is(instanceOf(LineString.class)));

            final Point startPoint = geometry.getFirstPoint();
            assertEquals(88.92, startPoint.getX(), 1e-8);
            assertEquals(9.750, startPoint.getY(), 1e-8);

            final Point endPoint = geometry.getLastPoint();
            assertEquals(84.82, endPoint.getX(), 1e-8);
            assertEquals(15.60, endPoint.getY(), 1e-8);
        }
    }

    @Test
    public void testRead_SST_CCI_V1_Data() throws Exception {
        final ExtractDefinitionBuilder builder = prepareExtractBuilder(2007, 11, 18);
        builder.shape(new int[]{1, 14});
        final ExtractDefinition extractDefinition = builder.build();

        try (InsituReader reader = createReader("insitu_WMOID_11851_20071123_20080111.nc")) {
            Array array = reader.read("sst", extractDefinition);
            assertNotNull(array);
            assertEquals(14, array.getSize());

            assertEquals(300.31, array.getDouble(0), 1e-5);
            assertEquals(300.31, array.getDouble(7), 1e-5);
            assertEquals(300.22998046875, array.getDouble(13), 1e-5);

            array = reader.read("lon", extractDefinition);
            assertNotNull(array);
            assertEquals(14, array.getSize());

            assertEquals(85.5140076, array.getDouble(0), 1e-5);
            assertEquals(85.4431534, array.getDouble(7), 1e-5);
            assertEquals(85.3783264, array.getDouble(13), 1e-5);

            array = reader.read("lat", extractDefinition);
            assertNotNull(array);
            assertEquals(14, array.getSize());

            assertEquals(14.2022161, array.getDouble(0), 1e-5);
            assertEquals(14.1622972, array.getDouble(7), 1e-5);
            assertEquals(14.107686, array.getDouble(13), 1e-5);

            array = reader.read("time", extractDefinition);
            assertNotNull(array);
            assertEquals(14, array.getSize());

            assertEquals(TimeUtil.julianDateToSecondsSinceEpoch(2454452.519), array.getDouble(0), 1e-3);
            assertEquals(TimeUtil.julianDateToSecondsSinceEpoch(2454453.060), array.getDouble(7), 1e-3);
            assertEquals(TimeUtil.julianDateToSecondsSinceEpoch(2454453.477), array.getDouble(13), 1e-3);
        }
    }

    @Test
    public void testReadSamplingPoints_SST_CCI_V1_Data() throws Exception {
        try (InsituReader reader = createReader("insitu_WMOID_11851_20071123_20080111.nc")) {
            final List<SamplingPoint> samplingPoints = reader.readSamplingPoints();
            assertEquals(1285, samplingPoints.size());

            SamplingPoint samplingPoint = samplingPoints.get(0);
            assertCorrectSamplingPoint(9.753368, 88.9182129, 1195835184000L, 0, samplingPoint);

            samplingPoint = samplingPoints.get(619);
            assertCorrectSamplingPoint(14.048774, 85.6864777, 1197883641600L, 619, samplingPoint);

            samplingPoint = samplingPoints.get(1284);
            assertCorrectSamplingPoint(15.607534, 84.8221817, 1200086841600L, 1284, samplingPoint);
        }
    }

    @Test
    public void testReadObservation_SST_CCI_V2_Drifter_Data() throws Exception {
        final InsituObservation observation;

        try (InsituReader reader = createReader("insitu_0_WMOID_71566_20020211_20120214.nc")) {
            assertEquals(1, reader.getNumRecords());
            observation = reader.readObservation(0);

            assertCorrectDate(2007, 1, 13, observation.getTime().getTime());
            assertEquals(1.57876344E8, observation.getTimeRadius(), 0.0);

            final PGgeometry location = observation.getLocation();
            assertNotNull(location);

            final Geometry geometry = location.getGeometry();
            assertThat(geometry, is(instanceOf(LineString.class)));

            final Point startPoint = geometry.getFirstPoint();
            assertEquals(0.61, startPoint.getX(), 1e-5);
            assertEquals(-62.33, startPoint.getY(), 1e-5);

            final Point endPoint = geometry.getLastPoint();
            assertEquals(-52.16, endPoint.getX(), 1e-5);
            assertEquals(-62.89, endPoint.getY(), 1e-5);
        }
    }

    @Test
    public void testRead_SST_CCI_V2_Drifter_Data() throws Exception {
        final ExtractDefinitionBuilder builder = prepareExtractBuilder(2010, 0, 25);
        builder.shape(new int[]{1, 2});
        final ExtractDefinition extractDefinition = builder.build();

        try (InsituReader reader = createReader("insitu_0_WMOID_71566_20020211_20120214.nc")) {
            Array array = reader.read("insitu.sea_surface_temperature", extractDefinition);
            assertEquals(2, array.getSize());
            assertEquals(1.6, array.getDouble(0), 1e-6);
            assertEquals(-32768.0, array.getDouble(1), 1e-6);

            array = reader.read("insitu.lon", extractDefinition);
            assertEquals(2, array.getSize());
            assertEquals(-58.89, array.getDouble(0), 1e-6);
            assertEquals(-32768.0, array.getDouble(1), 1e-6);

            array = reader.read("insitu.lat", extractDefinition);
            assertEquals(2, array.getSize());
            assertEquals(-60.62, array.getDouble(0), 1e-5);
            assertEquals(-32768.0, array.getDouble(1), 1e-6);

            array = reader.read("insitu.time", extractDefinition);
            assertEquals(2, array.getSize());
            assertEquals(1014480612, array.getDouble(0), 1e-6);
            assertEquals(-32768.0, array.getDouble(1), 1e-6);

            array = reader.read("insitu.sst_uncertainty", extractDefinition);
            assertEquals(2, array.getSize());
            assertEquals(0.389, array.getDouble(0), 1e-6);
            assertEquals(-32768.0, array.getDouble(1), 1e-6);

            array = reader.read("insitu.mohc_id", extractDefinition);
            assertEquals(2, array.getSize());
            assertEquals(1107312, array.getDouble(0), 1e-6);
            assertEquals(-32768.0, array.getDouble(1), 1e-6);
        }
    }

    @Test
    public void testReadSamplingPoints_SST_CCI_V2_Drifter_Data() throws Exception {
        try (InsituReader reader = createReader("insitu_0_WMOID_71566_20020211_20120214.nc")) {
            final List<SamplingPoint> samplingPoints = reader.readSamplingPoints();
            assertEquals(37221, samplingPoints.size());

            SamplingPoint samplingPoint = samplingPoints.get(0);
            assertCorrectSamplingPoint(-62.33, 0.61, 1013468400000L, 0, samplingPoint);

            samplingPoint = samplingPoints.get(15879);
            assertCorrectSamplingPoint(-47.78, 1.09, 1198240200000L, 15879, samplingPoint);

            samplingPoint = samplingPoints.get(37220);
            assertCorrectSamplingPoint(-62.89, -52.16, 1329221088000L, 37220, samplingPoint);
        }
    }

    @Test
    public void testReadObservation_SST_CCI_V2_Ship_Data() throws Exception {
        final InsituObservation observation;

        try (InsituReader reader = createReader("insitu_2_WMOID_ZNUB_20011201_20020211.nc")) {
            assertEquals(1, reader.getNumRecords());
            observation = reader.readObservation(0);

            assertCorrectDate(2002, 0, 6, observation.getTime().getTime());
            assertEquals(3078000.0, observation.getTimeRadius(), 0.0);

            final PGgeometry location = observation.getLocation();
            assertNotNull(location);

            final Geometry geometry = location.getGeometry();
            assertThat(geometry, is(instanceOf(LineString.class)));

            final Point startPoint = geometry.getFirstPoint();
            assertEquals(118.1, startPoint.getX(), 1e-5);
            assertEquals(-2.3, startPoint.getY(), 1e-5);

            final Point endPoint = geometry.getLastPoint();
            assertEquals(106.8, endPoint.getX(), 1e-5);
            assertEquals(-2.4, endPoint.getY(), 1e-5);
        }
    }

    @Test
    public void testRead_SST_CCI_V2_Ship_Data() throws Exception {
        final ExtractDefinitionBuilder builder = prepareExtractBuilder(2002, 0, 17);
        builder.shape(new int[]{1, 2});
        final ExtractDefinition extractDefinition = builder.build();

        try (InsituReader reader = createReader("insitu_2_WMOID_ZNUB_20011201_20020211.nc")) {
            Array array = reader.read("insitu.sea_surface_temperature", extractDefinition);
            assertEquals(2, array.getSize());
            assertEquals(26., array.getDouble(0), 1e-6);
            assertEquals(-32768.0, array.getDouble(1), 1e-6);

            array = reader.read("insitu.lon", extractDefinition);
            assertEquals(2, array.getSize());
            assertEquals(114.5, array.getDouble(0), 1e-6);
            assertEquals(-32768.0, array.getDouble(1), 1e-6);

            array = reader.read("insitu.lat", extractDefinition);
            assertEquals(2, array.getSize());
            assertEquals(-30.8, array.getDouble(0), 1e-5);
            assertEquals(-32768.0, array.getDouble(1), 1e-6);

            array = reader.read("insitu.time", extractDefinition);
            assertEquals(2, array.getSize());
            assertEquals(760449600, array.getDouble(0), 1e-6);
            assertEquals(-32768.0, array.getDouble(1), 1e-6);

            array = reader.read("insitu.sst_uncertainty", extractDefinition);
            assertEquals(2, array.getSize());
            assertEquals(1.026, array.getDouble(0), 1e-6);
            assertEquals(-32768.0, array.getDouble(1), 1e-6);

            array = reader.read("insitu.mohc_id", extractDefinition);
            assertEquals(2, array.getSize());
            assertEquals(63641, array.getDouble(0), 1e-6);
            assertEquals(-32768.0, array.getDouble(1), 1e-6);
        }
    }

    @Test
    public void testReadSamplingPoints_SST_CCI_V2_Ship_Data() throws Exception {
        try (InsituReader reader = createReader("insitu_2_WMOID_ZNUB_20011201_20020211.nc")) {
            final List<SamplingPoint> samplingPoints = reader.readSamplingPoints();
            assertEquals(25, samplingPoints.size());

            SamplingPoint samplingPoint = samplingPoints.get(0);
            assertCorrectSamplingPoint(-2.3, 118.1, 1007229600000L, 0, samplingPoint);

            samplingPoint = samplingPoints.get(12);
            assertCorrectSamplingPoint(-19.3, 117.6, 1010016000000L, 12, samplingPoint);

            samplingPoint = samplingPoints.get(24);
            assertCorrectSamplingPoint(-2.4, 106.8, 1013385600000L, 24, samplingPoint);
        }
    }

   // @todo 2 tb/tb add tests for GTMBA data once Gary has updated the files

    @Test
    public void testReadObservation_SST_CCI_V2_Argo_Data() throws Exception {
        final InsituObservation observation;

        try (InsituReader reader = createReader("insitu_5_WMOID_69036_20001006_20020906.nc")) {
            assertEquals(1, reader.getNumRecords());
            observation = reader.readObservation(0);

            assertCorrectDate(2001, 8, 21, observation.getTime().getTime());

            assertEquals(30234900.0, observation.getTimeRadius(), 0.0);

            final PGgeometry location = observation.getLocation();
            assertNotNull(location);

            final Geometry geometry = location.getGeometry();
            assertThat(geometry, is(instanceOf(LineString.class)));

            final Point startPoint = geometry.getFirstPoint();
            assertEquals(-20.252, startPoint.getX(), 1e-6);
            assertEquals(41.58, startPoint.getY(), 1e-5);

            final Point endPoint = geometry.getLastPoint();
            assertEquals(-22.798, endPoint.getX(), 1e-6);
            assertEquals(35.347, endPoint.getY(), 1e-6);
        }
    }

    @Test
     public void testRead_SST_CCI_V2_Argo_Data() throws Exception {
        final ExtractDefinitionBuilder builder = prepareExtractBuilder(2001, 7, 20);
        builder.shape(new int[]{1, 2});
        final ExtractDefinition extractDefinition = builder.build();

        try (InsituReader reader = createReader("insitu_5_WMOID_69036_20001006_20020906.nc")) {
            Array array = reader.read("insitu.sea_surface_temperature", extractDefinition);
            assertEquals(2, array.getSize());
            assertEquals(21.92, array.getDouble(0), 1e-6);
            assertEquals(-32768.0, array.getDouble(1), 1e-6);

            array = reader.read("insitu.lon", extractDefinition);
            assertEquals(2, array.getSize());
            assertEquals(-21.617, array.getDouble(0), 1e-6);
            assertEquals(-32768.0, array.getDouble(1), 1e-6);

            array = reader.read("insitu.lat", extractDefinition);
            assertEquals(2, array.getSize());
            assertEquals(39.47499847, array.getDouble(0), 1e-6);
            assertEquals(-32768.0, array.getDouble(1), 1e-6);

            array = reader.read("insitu.time", extractDefinition);
            assertEquals(2, array.getSize());
            assertEquals(749478179, array.getDouble(0), 1e-6);
            assertEquals(-32768.0, array.getDouble(1), 1e-6);

            array = reader.read("insitu.sst_uncertainty", extractDefinition);
            assertEquals(2, array.getSize());
            assertEquals(0.002, array.getDouble(0), 1e-6);
            assertEquals(-32768.0, array.getDouble(1), 1e-6);

            array = reader.read("insitu.mohc_id", extractDefinition);
            assertEquals(2, array.getSize());
            assertEquals(2519326, array.getDouble(0), 1e-6);
            assertEquals(-32768.0, array.getDouble(1), 1e-6);
        }
    }

    @Test
    public void testReadSamplingPoints_SST_CCI_V2_Argo_Data() throws Exception {
        try (InsituReader reader = createReader("insitu_5_WMOID_69036_20001006_20020906.nc")) {
            final List<SamplingPoint> samplingPoints = reader.readSamplingPoints();
            assertEquals(27, samplingPoints.size());

            SamplingPoint samplingPoint = samplingPoints.get(0);
            assertCorrectSamplingPoint(41.58, -20.252, 970839660000L, 0, samplingPoint);

            samplingPoint = samplingPoints.get(14);
            assertCorrectSamplingPoint(38.003, -22.754, 1003672140000L, 14, samplingPoint);

            samplingPoint = samplingPoints.get(26);
            assertCorrectSamplingPoint(35.347, -22.798, 1031309460000L, 26, samplingPoint);
        }
    }

    @Test
    public void testReadObservation_SST_CCI_V2_Xbt_Data() throws Exception {
        final InsituObservation observation;

        try (InsituReader reader = createReader("insitu_9_WMOID_9612_20060827_20060829.nc")) {
            assertEquals(1, reader.getNumRecords());
            observation = reader.readObservation(0);

            assertCorrectDate(2006, 7, 28, observation.getTime().getTime());
            assertEquals(77310, observation.getTimeRadius(), 0.0);

            final PGgeometry location = observation.getLocation();
            assertNotNull(location);

            final Geometry geometry = location.getGeometry();
            assertThat(geometry, is(instanceOf(LineString.class)));

            final Point startPoint = geometry.getFirstPoint();
            assertEquals(111.191703796, startPoint.getX(), 1e-6);
            assertEquals(-20.6517, startPoint.getY(), 1e-5);

            final Point endPoint = geometry.getLastPoint();
            assertEquals(105.75170134, endPoint.getX(), 1e-6);
            assertEquals(-8.7067, endPoint.getY(), 1e-6);
        }
    }

    @Test
    public void testRead_SST_CCI_V2_Xbt_Data() throws Exception {
        final ExtractDefinitionBuilder builder = prepareExtractBuilder(2006, 7, 28);
        builder.shape(new int[]{1, 3});
        final ExtractDefinition extractDefinition = builder.build();

        try (InsituReader reader = createReader("insitu_9_WMOID_9612_20060827_20060829.nc")) {
            Array array = reader.read("insitu.sea_surface_temperature", extractDefinition);
            assertEquals(3, array.getSize());
            assertEquals(25.57, array.getDouble(0), 1e-6);
            assertEquals(25.69, array.getDouble(1), 1e-6);
            assertEquals(25.59, array.getDouble(2), 1e-6);

            array = reader.read("insitu.lon", extractDefinition);
            assertEquals(3, array.getSize());
            assertEquals(108.5867, array.getDouble(0), 1e-6);
            assertEquals(107.2300034, array.getDouble(1), 1e-6);
            assertEquals(106.3700027, array.getDouble(2), 1e-6);

            array = reader.read("insitu.lat", extractDefinition);
            assertEquals(3, array.getSize());
            assertEquals(-15.0433, array.getDouble(0), 1e-6);
            assertEquals(-12.0917, array.getDouble(1), 1e-6);
            assertEquals(-10.0933, array.getDouble(2), 1e-6);

            array = reader.read("insitu.time", extractDefinition);
            assertEquals(3, array.getSize());
            assertEquals(904274351, array.getDouble(0), 1e-6);
            assertEquals(904312728, array.getDouble(1), 1e-6);
            assertEquals(904338467, array.getDouble(2), 1e-6);

            array = reader.read("insitu.sst_uncertainty", extractDefinition);
            assertEquals(3, array.getSize());
            assertEquals(0.15, array.getDouble(0), 1e-6);
            assertEquals(0.15, array.getDouble(1), 1e-6);
            assertEquals(0.15, array.getDouble(2), 1e-6);

            array = reader.read("insitu.mohc_id", extractDefinition);
            assertEquals(3, array.getSize());
            assertEquals(2581270, array.getDouble(0), 1e-6);
            assertEquals(2589864, array.getDouble(1), 1e-6);
            assertEquals(2597200, array.getDouble(2), 1e-6);
        }
    }

    @Test
    public void testReadSamplingPoints_SST_CCI_V2_Xbt_Data() throws Exception {
        try (InsituReader reader = createReader("insitu_9_WMOID_9612_20060827_20060829.nc")) {
            final List<SamplingPoint> samplingPoints = reader.readSamplingPoints();
            assertEquals(10, samplingPoints.size());

            SamplingPoint samplingPoint = samplingPoints.get(0);
            assertCorrectSamplingPoint(-20.6517, 111.19170379, 1156662360000L, 0, samplingPoint);

            samplingPoint = samplingPoints.get(6);
            assertCorrectSamplingPoint(-13.205, 107.7417, 1156759487000L, 6, samplingPoint);

            samplingPoint = samplingPoints.get(9);
            assertCorrectSamplingPoint(-8.7067, 105.7517014, 1156816980000L, 9, samplingPoint);
        }
    }

    @Test
    public void testReadObservation_SST_CCI_V2_Mbt_Data() throws Exception {
        final InsituObservation observation;

        try (InsituReader reader = createReader("insitu_10_WMOID_9733863_19840602_19840602.nc")) {
            assertEquals(1, reader.getNumRecords());
            observation = reader.readObservation(0);

            assertCorrectDate(1984, 5, 2, observation.getTime().getTime());
            assertEquals(21600, observation.getTimeRadius(), 0.0);

            final PGgeometry location = observation.getLocation();
            assertNotNull(location);

            final Geometry geometry = location.getGeometry();
            assertThat(geometry, is(instanceOf(LineString.class)));

            final Point startPoint = geometry.getFirstPoint();
            assertEquals(25.67, startPoint.getX(), 1e-6);
            assertEquals(39.82, startPoint.getY(), 1e-5);

            final Point endPoint = geometry.getLastPoint();
            assertEquals(25.67, endPoint.getX(), 1e-6);
            assertEquals(39.82, endPoint.getY(), 1e-6);
        }
    }

    @Test
    public void testRead_SST_CCI_V2_Mbt_Data() throws Exception {
        final ExtractDefinitionBuilder builder = prepareExtractBuilder(1984, 5, 2);
        builder.shape(new int[]{1, 2});
        final ExtractDefinition extractDefinition = builder.build();

        try (InsituReader reader = createReader("insitu_10_WMOID_9733863_19840602_19840602.nc")) {
            Array array = reader.read("insitu.sea_surface_temperature", extractDefinition);
            assertEquals(2, array.getSize());
            assertEquals(19.7, array.getDouble(0), 1e-6);
            assertEquals(19.0, array.getDouble(1), 1e-6);

            array = reader.read("insitu.lon", extractDefinition);
            assertEquals(2, array.getSize());
            assertEquals(25.67, array.getDouble(0), 1e-6);
            assertEquals(25.67, array.getDouble(1), 1e-6);

            array = reader.read("insitu.lat", extractDefinition);
            assertEquals(2, array.getSize());
            assertEquals(39.82, array.getDouble(0), 1e-6);
            assertEquals(39.82, array.getDouble(1), 1e-6);

            array = reader.read("insitu.time", extractDefinition);
            assertEquals(2, array.getSize());
            assertEquals(202543200, array.getDouble(0), 1e-6);
            assertEquals(202586400, array.getDouble(1), 1e-6);

            array = reader.read("insitu.sst_uncertainty", extractDefinition);
            assertEquals(2, array.getSize());
            assertEquals(0.3, array.getDouble(0), 1e-6);
            assertEquals(0.3, array.getDouble(1), 1e-6);

            array = reader.read("insitu.mohc_id", extractDefinition);
            assertEquals(2, array.getSize());
            assertEquals(688592, array.getDouble(0), 1e-6);
            assertEquals(689384, array.getDouble(1), 1e-6);
        }
    }

    @Test
    public void testReadSamplingPoints_SST_CCI_V2_Mbt_Data() throws Exception {
        try (InsituReader reader = createReader("insitu_10_WMOID_9733863_19840602_19840602.nc")) {
            final List<SamplingPoint> samplingPoints = reader.readSamplingPoints();
            assertEquals(2, samplingPoints.size());

            SamplingPoint samplingPoint = samplingPoints.get(0);
            assertCorrectSamplingPoint(39.82, 25.67, 455004000000L, 0, samplingPoint);

            samplingPoint = samplingPoints.get(1);
            assertCorrectSamplingPoint(39.82, 25.67, 455047200000L, 1, samplingPoint);
        }
    }

    @Test
    public void testReadObservation_SST_CCI_V2_Cdt_Data() throws Exception {
        final InsituObservation observation;

        try (InsituReader reader = createReader("insitu_11_WMOID_991446_19850619_19850703.nc")) {
            assertEquals(1, reader.getNumRecords());
            observation = reader.readObservation(0);

            assertCorrectDate(1985, 5, 26, observation.getTime().getTime());
            assertEquals(579564.5, observation.getTimeRadius(), 0.0);

            final PGgeometry location = observation.getLocation();
            assertNotNull(location);

            final Geometry geometry = location.getGeometry();
            assertThat(geometry, is(instanceOf(LineString.class)));

            final Point startPoint = geometry.getFirstPoint();
            assertEquals(-63.8303, startPoint.getX(), 1e-6);
            assertEquals(43.7332, startPoint.getY(), 1e-5);

            final Point endPoint = geometry.getLastPoint();
            assertEquals(-61.1702, endPoint.getX(), 1e-6);
            assertEquals(45.09500122, endPoint.getY(), 1e-6);
        }
    }

    @Test
    public void testRead_SST_CCI_V2_Cdt_Data() throws Exception {
        final ExtractDefinitionBuilder builder = prepareExtractBuilder(1985, 5, 30);
        builder.shape(new int[]{1, 3});
        final ExtractDefinition extractDefinition = builder.build();

        try (InsituReader reader = createReader("insitu_11_WMOID_991446_19850619_19850703.nc")) {
            Array array = reader.read("insitu.sea_surface_temperature", extractDefinition);
            assertEquals(3, array.getSize());
            assertEquals(11.894, array.getDouble(0), 1e-6);
            assertEquals(11.897, array.getDouble(1), 1e-6);
            assertEquals(11.89, array.getDouble(2), 1e-6);

            array = reader.read("insitu.lon", extractDefinition);
            assertEquals(3, array.getSize());
            assertEquals(-58.55149841, array.getDouble(0), 1e-6);
            assertEquals(-58.5573, array.getDouble(1), 1e-6);
            assertEquals(-58.5658, array.getDouble(2), 1e-6);

            array = reader.read("insitu.lat", extractDefinition);
            assertEquals(3, array.getSize());
            assertEquals(43.837002, array.getDouble(0), 1e-6);
            assertEquals(43.8385, array.getDouble(1), 1e-6);
            assertEquals(43.8452, array.getDouble(2), 1e-6);

            array = reader.read("insitu.time", extractDefinition);
            assertEquals(3, array.getSize());
            assertEquals(236546028, array.getDouble(0), 1e-6);
            assertEquals(236551212, array.getDouble(1), 1e-6);
            assertEquals(236556251, array.getDouble(2), 1e-6);

            array = reader.read("insitu.sst_uncertainty", extractDefinition);
            assertEquals(3, array.getSize());
            assertEquals(0.002, array.getDouble(0), 1e-6);
            assertEquals(0.002, array.getDouble(1), 1e-6);
            assertEquals(0.002, array.getDouble(2), 1e-6);

            array = reader.read("insitu.mohc_id", extractDefinition);
            assertEquals(3, array.getSize());
            assertEquals(737551, array.getDouble(0), 1e-6);
            assertEquals(739080, array.getDouble(1), 1e-6);
            assertEquals(740163, array.getDouble(2), 1e-6);
        }
    }

    @Test
    public void testReadSamplingPoints_SST_CCI_V2_Cdt_Data() throws Exception {
        try (InsituReader reader = createReader("insitu_11_WMOID_991446_19850619_19850703.nc")) {
            final List<SamplingPoint> samplingPoints = reader.readSamplingPoints();
            assertEquals(20, samplingPoints.size());

            SamplingPoint samplingPoint = samplingPoints.get(0);
            assertCorrectSamplingPoint(43.7332, -63.8303, 488062511000L, 0, samplingPoint);

            samplingPoint = samplingPoints.get(9);
            assertCorrectSamplingPoint(40.5975, -62.2245, 488347560000L, 9, samplingPoint);

            samplingPoint = samplingPoints.get(19);
            assertCorrectSamplingPoint(45.095, -61.1702, 489221640000L, 19, samplingPoint);
        }
    }

    @Test
    public void testCreateSubset_1D() throws InvalidRangeException {
        final Array historyTimes = InsituData.createHistoryTimeArray_MJD();
        final Range range = InsituReaderHelper.findRange(historyTimes, 2455090.56, 0.5);
        final List<Range> s = InsituReaderHelper.createSubsampling(historyTimes, range, 10);
        final Array subset = Array.factory(historyTimes.getElementType(), new int[]{1, 10});
        InsituReader.extractSubset(historyTimes, subset, s);

        assertEquals(2, subset.getRank());
        assertEquals(10, subset.getIndexPrivate().getShape(1));
        assertEquals(historyTimes.getDouble(s.get(0).first()), subset.getDouble(0), 0.0);
        assertEquals(historyTimes.getDouble(s.get(9).first()), subset.getDouble(9), 0.0);
    }

    @Test
    public void testCreateSubset_2D() throws InvalidRangeException {
        final Array historyTimes = InsituData.createHistoryTimeArray_MJD();
        final int historyLength = historyTimes.getIndexPrivate().getShape(0);
        final Range range = InsituReaderHelper.findRange(historyTimes, 2455090.56, 0.5);
        final List<Range> s = InsituReaderHelper.createSubsampling(historyTimes, range, 10);

        final Array array = Array.factory(DataType.INT, new int[]{historyLength, 2});
        final Array subset = Array.factory(array.getElementType(), new int[]{1, 10, 2});
        InsituReader.extractSubset(array, subset, s);

        assertEquals(3, subset.getRank());
        assertEquals(10, subset.getIndexPrivate().getShape(1));
        assertEquals(2, subset.getIndexPrivate().getShape(2));
    }

    @Test
    public void testNormalizeLon() {
        assertEquals(26.0, InsituReader.normalizeLon(26.0), 1e-8);
        assertEquals(-107.0, InsituReader.normalizeLon(-107.0), 1e-8);

        assertEquals(-0.1, InsituReader.normalizeLon(359.9), 1e-8);
        assertEquals(0.1, InsituReader.normalizeLon(-359.9), 1e-8);

        assertEquals(-19.0, InsituReader.normalizeLon(-379.0), 1e-8);
        assertEquals(3.0, InsituReader.normalizeLon(363.0), 1e-8);
    }

    @Test
    public void testGetNumRecords() {
        final InsituReader reader = new InsituReader("whatever");

        assertEquals(1, reader.getNumRecords());
    }

    @Test
    public void testGetGeoCoding() throws IOException {
        final InsituReader reader = new InsituReader("whatever");

        assertNull(reader.getGeoCoding(34));
    }

    @Test
    public void testGetDTime() throws IOException {
        final InsituReader reader = new InsituReader("whatever");

        try {
            reader.getDTime(23, 45);
            fail("IllegalStateException expected");
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void testGetTime() throws IOException {
        final InsituReader reader = new InsituReader("whatever");

        try {
            reader.getTime(23, 45);
            fail("IllegalStateException expected");
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void testGetInsituSource() throws IOException {
        final InsituReader reader = new InsituReader("whatever");

        assertNull(reader.getInsituSource());
    }

    @Test
    public void testGetScanLineCount() throws IOException {
        final InsituReader reader = new InsituReader("whatever");

        try {
            reader.getScanLineCount();
            fail("IllegalStateException expected");
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void testGetElement() throws IOException {
        final InsituReader reader = new InsituReader("whatever");

        try {
            reader.getElementCount();
            fail("IllegalStateException expected");
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void testIsNotOnPlanet() {
        assertFalse(InsituReader.isNotOnPlanet(0.0, 0.0));
        assertFalse(InsituReader.isNotOnPlanet(-17.0, 56.0));
        assertFalse(InsituReader.isNotOnPlanet(38.0, -36.0));

        assertTrue(InsituReader.isNotOnPlanet(90.1, -36.0));
        assertTrue(InsituReader.isNotOnPlanet(-90.1, -36.0));
        assertTrue(InsituReader.isNotOnPlanet(18.3, -180.1));
        assertTrue(InsituReader.isNotOnPlanet(18.3, 180.1));
    }

    private static InsituReader createReader(String resourceName) throws Exception {
        final DataFile dataFile = new DataFile();
        final String path = getResourceAsFile(resourceName).getPath();
        dataFile.setPath(path);

        final InsituReader reader = new InsituReader("history");
        reader.init(dataFile, null);

        return reader;
    }

    private static File getResourceAsFile(String name) throws URISyntaxException {
        final URL url = InsituReaderTest.class.getResource(name);
        final URI uri = url.toURI();

        return new File(uri);
    }

    private static Calendar createUtcCalendar() {
        return new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.ENGLISH);
    }

    private static ExtractDefinitionBuilder prepareExtractBuilder(int year, int month, int day) {
        final Calendar calendar = creatUtcCalendar(year, month, day);

        final ExtractDefinitionBuilder builder = new ExtractDefinitionBuilder();
        final ReferenceObservation refObs = new ReferenceObservation();
        refObs.setTime(calendar.getTime());
        builder.referenceObservation(refObs);
        return builder;
    }

    private static Calendar creatUtcCalendar(int year, int month, int day) {
        final Calendar calendar = createUtcCalendar();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar;
    }

    private static void assertCorrectSamplingPoint(double lat, double lon, long time, int index, SamplingPoint samplingPoint) {
        assertEquals(lat, samplingPoint.getLat(), 1e-5);
        assertEquals(lon, samplingPoint.getLon(), 1e-5);
        assertEquals(time, samplingPoint.getTime());
        assertEquals(index, samplingPoint.getIndex());
    }

    private static void assertCorrectDate(int year, int month, int day, long time) {
        final Calendar calendar = createUtcCalendar();
        calendar.setTimeInMillis(time);
        assertEquals(year, calendar.get(Calendar.YEAR));
        assertEquals(month, calendar.get(Calendar.MONTH));
        assertEquals(day, calendar.get(Calendar.DATE));
    }
}
