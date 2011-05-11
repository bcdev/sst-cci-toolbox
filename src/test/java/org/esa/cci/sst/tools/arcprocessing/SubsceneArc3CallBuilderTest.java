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

package org.esa.cci.sst.tools.arcprocessing;

import org.esa.cci.sst.Queries;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.tools.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.postgis.PGgeometry;
import org.postgis.Point;
import ucar.ma2.Array;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import javax.persistence.Query;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Thomas Storm
 */
@SuppressWarnings({"InstanceMethodNamingConvention", "OverlyLongMethod"})
public class SubsceneArc3CallBuilderTest {

    private NetcdfFileWriteable target;
    private String netcdfResource;

    @Before
    public void setUp() throws Exception {
        final String someNetcdfResource = getClass().getResource("empty_test.nc").getFile();
        netcdfResource = getClass().getResource("test_with_mixed_variables.nc").getFile();
        final File file = new File(new File(someNetcdfResource).getParent(), "test_writable.nc");
        target = NetcdfFileWriteable.createNew(file.getAbsolutePath());
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    @After
    public void tearDown() throws Exception {
        new File(target.getLocation()).delete();
    }

    @Test
    public void testCreateSubsceneMmdFilename() throws Exception {
        final Properties config = new Properties();
        config.setProperty(Constants.PROPERTY_MMS_ARC3_SOURCEFILE, "mmd.nc");
        final SubsceneArc3CallBuilder subsceneArc3CallBuilder = new SubsceneArc3CallBuilder(config, null);
        final String subsceneMmdFilename = subsceneArc3CallBuilder.createSubsceneMmdFilename();
        assertEquals("mmd_subscenes.nc", subsceneMmdFilename);
    }

    @Test
    public void testAddDimensionsAndVariables() throws Exception {
        final SubsceneArc3CallBuilder subsceneArc3CallBuilder = new SubsceneArc3CallBuilder(null, null);
        final NetcdfFile source = NetcdfFile.open(netcdfResource);

        final Variable someSubsceneVariable = source.findVariable(NetcdfFile.escapeName("atsr_orb.someVariable"));
        subsceneArc3CallBuilder.addSubsceneDimensions(target, someSubsceneVariable);

        assertEquals(3, target.getRootGroup().getDimensions().size());
        assertNotNull(target.getRootGroup().findDimension("record"));
        assertNotNull(target.getRootGroup().findDimension("ni"));
        assertNotNull(target.getRootGroup().findDimension("nj"));
        assertEquals(2, target.getRootGroup().findDimension("record").getLength());
        assertEquals(101, target.getRootGroup().findDimension("ni").getLength());
        assertEquals(101, target.getRootGroup().findDimension("nj").getLength());

        subsceneArc3CallBuilder.addNonSubsceneDimensions(source, target);

        assertEquals(5, target.getRootGroup().getDimensions().size());
        assertNotNull(target.getRootGroup().findDimension("someOther"));
        assertNotNull(target.getRootGroup().findDimension("someOtherAgain"));
        assertEquals(5, target.getRootGroup().findDimension("someOther").getLength());
        assertEquals(5, target.getRootGroup().findDimension("someOtherAgain").getLength());

        assertTrue(target.getVariables() == null || target.getVariables().isEmpty());

        subsceneArc3CallBuilder.addVariables(source, target);

        assertEquals(5, target.getRootGroup().getVariables().size());
        final Variable matchupVar = target.getRootGroup().findVariable("matchup.id");
        assertNotNull(matchupVar);
        assertEquals("record", matchupVar.getDimensionsString());
        final Variable latitudeVar = target.getRootGroup().findVariable("atsr_orb.latitude");
        assertNotNull(latitudeVar);
        assertEquals("record ni nj", latitudeVar.getDimensionsString());
        final Variable longitudeVar = target.getRootGroup().findVariable("atsr_orb.longitude");
        assertNotNull(longitudeVar);
        assertEquals("record ni nj", longitudeVar.getDimensionsString());
        final Variable someAtsrVariable = target.getRootGroup().findVariable("atsr_orb.someVariable");
        assertNotNull(someAtsrVariable);
        assertEquals("record ni nj", someAtsrVariable.getDimensionsString());
        final Variable someVariable = target.getRootGroup().findVariable("someVariable");
        assertNotNull(someVariable);
        assertEquals("record someOther someOtherAgain", someVariable.getDimensionsString());
    }

    @Test(expected = IllegalStateException.class)
    public void testGetMatchupVariable_Failing() throws Exception {
        final SubsceneArc3CallBuilder subsceneArc3CallBuilder = new SubsceneArc3CallBuilder(null, null);
        subsceneArc3CallBuilder.getMatchupVariable(NetcdfFile.open(getClass().getResource("empty_test.nc").getFile()));
    }

    @Test
    public void testGetMatchupVariable() throws Exception {
        final SubsceneArc3CallBuilder subsceneArc3CallBuilder = new SubsceneArc3CallBuilder(null, null);
        final Variable matchupVariable = subsceneArc3CallBuilder.getMatchupVariable(
                NetcdfFile.open(getClass().getResource("test.nc").getFile()));

        assertNotNull(matchupVariable);
        assertEquals("matchup_id", matchupVariable.getName());
        assertEquals(1, matchupVariable.getDimensions().size());
        assertEquals("record", matchupVariable.getDimensions().get(0).getName());
        assertEquals(10, matchupVariable.getDimensions().get(0).getLength());
    }

    @Test
    public void testGetMatchupVariableWithAlternativeName() throws Exception {
        final SubsceneArc3CallBuilder subsceneArc3CallBuilder = new SubsceneArc3CallBuilder(null, null);
        final Variable matchupVariable = subsceneArc3CallBuilder.getMatchupVariable(
                NetcdfFile.open(getClass().getResource("test2.nc").getFile()));

        assertNotNull(matchupVariable);
        assertEquals("matchup.id", matchupVariable.getName());
        assertEquals(1, matchupVariable.getDimensions().size());
        assertEquals("record", matchupVariable.getDimensions().get(0).getName());
        assertEquals(10, matchupVariable.getDimensions().get(0).getLength());
    }

    @Test
    public void testGetMatchupLocations() throws Exception {
        final PersistenceManager persistenceManager = mock(PersistenceManager.class);
        final Query query = mock(Query.class);
        when(persistenceManager.createQuery(Queries.QUERY_STRING_SELECT_REFERENCE_OBSERVATION_FOR_MATCHUP)).thenReturn(
                query);
        final ReferenceObservation singleResult = new ReferenceObservation();
        singleResult.setPoint(new PGgeometry(new Point(10.0, 20.0)));
        when(query.getSingleResult()).thenReturn(singleResult);
        final SubsceneArc3CallBuilder subsceneArc3CallBuilder = new SubsceneArc3CallBuilder(null, persistenceManager);
        final NetcdfFile file = NetcdfFile.open(netcdfResource);
        final Map<Integer, Point> matchupLocations = subsceneArc3CallBuilder.getMatchupLocations(file);

        assertEquals(2, matchupLocations.size());
        for (Point point : matchupLocations.values()) {
            assertEquals(10.0, point.getX(), 0.0001);
            assertEquals(20.0, point.getY(), 0.0001);
        }
    }

    @Test
    public void testFindCentralPoint() throws Exception {
        final NetcdfFile file = NetcdfFile.open(netcdfResource);
        final Variable latitude = file.findVariable(NetcdfFile.escapeName("atsr_orb.latitude"));
        final Variable longitude = file.findVariable(NetcdfFile.escapeName("atsr_orb.longitude"));

        final SubsceneArc3CallBuilder subsceneArc3CallBuilder = new SubsceneArc3CallBuilder(null, null);
        final int[] centralPoint1 = subsceneArc3CallBuilder.findCentralNetcdfCoords(latitude, longitude, 0,
                                                                                    new Point(87, 20));
        final int[] centralPoint2 = subsceneArc3CallBuilder.findCentralNetcdfCoords(latitude, longitude, 0,
                                                                                    new Point(91, 20));
        final int[] centralPoint3 = subsceneArc3CallBuilder.findCentralNetcdfCoords(latitude, longitude, 0,
                                                                                    new Point(81, 15));
        final int[] centralPoint4 = subsceneArc3CallBuilder.findCentralNetcdfCoords(latitude, longitude, 0,
                                                                                    new Point(90, 40));
        final int[] centralPoint5 = subsceneArc3CallBuilder.findCentralNetcdfCoords(latitude, longitude, 1,
                                                                                    new Point(25, 120));
        final int[] centralPoint6 = subsceneArc3CallBuilder.findCentralNetcdfCoords(latitude, longitude, 1,
                                                                                    new Point(25, 119));
        final int[] centralPoint7 = subsceneArc3CallBuilder.findCentralNetcdfCoords(latitude, longitude, 1,
                                                                                    new Point(21, 118));
        final int[] centralPoint8 = subsceneArc3CallBuilder.findCentralNetcdfCoords(latitude, longitude, 1,
                                                                                    new Point(21, 117));

        assertEquals(0, centralPoint1[0]);
        assertEquals(1, centralPoint1[1]);

        assertEquals(0, centralPoint2[0]);
        assertEquals(1, centralPoint2[1]);

        assertEquals(0, centralPoint3[0]);
        assertEquals(0, centralPoint3[1]);

        assertEquals(1, centralPoint4[0]);
        assertEquals(1, centralPoint4[1]);

        assertEquals(0, centralPoint5[0]);
        assertEquals(0, centralPoint5[1]);

        assertEquals(0, centralPoint6[0]);
        assertEquals(1, centralPoint6[1]);

        assertEquals(1, centralPoint7[0]);
        assertEquals(0, centralPoint7[1]);

        assertEquals(1, centralPoint8[0]);
        assertEquals(1, centralPoint8[1]);
    }

    @Test
    public void testReadSubscene() throws Exception {
        final SubsceneArc3CallBuilder subsceneArc3CallBuilder = new SubsceneArc3CallBuilder(null, null);
        final NetcdfFile file = NetcdfFile.open(netcdfResource);
        final Variable latitude = file.findVariable(NetcdfFile.escapeName("atsr_orb.latitude"));
        final Variable longitude = file.findVariable(NetcdfFile.escapeName("atsr_orb.longitude"));

        final Array array1 = subsceneArc3CallBuilder.readSubscene(0, new int[]{1, 1}, latitude, 1);
        final Array array2 = subsceneArc3CallBuilder.readSubscene(0, new int[]{1, 1}, latitude, 2);
        final Array array3 = subsceneArc3CallBuilder.readSubscene(0, new int[]{1, 1}, latitude, 3);

        final Array array4 = subsceneArc3CallBuilder.readSubscene(0, new int[]{1, 1}, longitude, 1);
        final Array array5 = subsceneArc3CallBuilder.readSubscene(0, new int[]{1, 1}, longitude, 2);
        final Array array6 = subsceneArc3CallBuilder.readSubscene(0, new int[]{1, 1}, longitude, 3);

        assertEquals(1, array1.getSize());
        assertEquals(40, array1.getDouble(0), 0.001);

        assertEquals(4, array2.getSize());
        assertEquals(10, array2.getDouble(0), 0.001);
        assertEquals(20, array2.getDouble(1), 0.001);
        assertEquals(30, array2.getDouble(2), 0.001);
        assertEquals(40, array2.getDouble(3), 0.001);

        assertEquals(9, array3.getSize());
        assertEquals(10, array3.getDouble(0), 0.001);
        assertEquals(20, array3.getDouble(1), 0.001);
        assertEquals(1001, array3.getDouble(2), 0.001);
        assertEquals(30, array3.getDouble(3), 0.001);
        assertEquals(40, array3.getDouble(4), 0.001);
        assertEquals(1002, array3.getDouble(5), 0.001);
        assertEquals(50, array3.getDouble(6), 0.001);
        assertEquals(60, array3.getDouble(7), 0.001);
        assertEquals(1003, array3.getDouble(8), 0.001);

        assertEquals(1, array4.getSize());
        assertEquals(95, array4.getDouble(0), 0.001);

        assertEquals(4, array5.getSize());
        assertEquals(80, array5.getDouble(0), 0.001);
        assertEquals(85, array5.getDouble(1), 0.001);
        assertEquals(90, array5.getDouble(2), 0.001);
        assertEquals(95, array5.getDouble(3), 0.001);

        assertEquals(9, array6.getSize());
        assertEquals(80, array6.getDouble(0), 0.001);
        assertEquals(85, array6.getDouble(1), 0.001);
        assertEquals(1001, array6.getDouble(2), 0.001);
        assertEquals(90, array6.getDouble(3), 0.001);
        assertEquals(95, array6.getDouble(4), 0.001);
        assertEquals(1002, array6.getDouble(5), 0.001);
        assertEquals(100, array6.getDouble(6), 0.001);
        assertEquals(105, array6.getDouble(7), 0.001);
        assertEquals(1003, array6.getDouble(8), 0.001);
    }

    @Test
    public void testWriteSubscene() throws Exception {
        final NetcdfFile source = NetcdfFile.open(netcdfResource);
        final List<Variable> variables = source.getVariables();
        final SubsceneArc3CallBuilder subsceneArc3CallBuilderMock = setupMock(source);

        subsceneArc3CallBuilderMock.addSubsceneDimensions(target, source.findVariable(NetcdfFile.escapeName("atsr_orb.latitude")));
        subsceneArc3CallBuilderMock.addNonSubsceneDimensions(source, target);
        subsceneArc3CallBuilderMock.addVariables(source, target);
        target.create();
        variables.remove(0);
        subsceneArc3CallBuilderMock.writeSubscene(source, target, variables);
        final List<Variable> targetVariables = target.getVariables();

        assertEquals(5, targetVariables.size());
        assertEquals("record", targetVariables.get(0).getDimensionsString());
        assertEquals("record ni nj", targetVariables.get(1).getDimensionsString());
        assertEquals("record ni nj", targetVariables.get(2).getDimensionsString());
    }

    private SubsceneArc3CallBuilder setupMock(NetcdfFile source) throws Exception {
        final SubsceneArc3CallBuilder subsceneArc3CallBuilderMock = mock(SubsceneArc3CallBuilder.class);

        final Map<Integer, Point> map = new HashMap<Integer, Point>();
        map.put(0, new Point(80.0, 10.0));
        map.put(1, new Point(105.0, 60.0));
        when(subsceneArc3CallBuilderMock.getMatchupLocations(source)).thenReturn(map);
        when(subsceneArc3CallBuilderMock.getSubsceneWidth()).thenReturn(2);
        doCallRealMethod().when(subsceneArc3CallBuilderMock).addSubsceneDimensions(Matchers.<NetcdfFileWriteable>any(),
                                                                                   Matchers.<Variable>any());
        doCallRealMethod().when(subsceneArc3CallBuilderMock).addNonSubsceneDimensions(Matchers.<NetcdfFileWriteable>any(),
                                                                                      Matchers.<NetcdfFileWriteable>any());
        doCallRealMethod().when(subsceneArc3CallBuilderMock).addVariables(Matchers.<NetcdfFile>any(),
                                                                          Matchers.<NetcdfFileWriteable>any());
        doCallRealMethod().when(subsceneArc3CallBuilderMock).writeSubscene(Matchers.<NetcdfFile>any(),
                                                                           Matchers.<NetcdfFileWriteable>any(),
                                                                           Matchers.<List<Variable>>any());
        doCallRealMethod().when(subsceneArc3CallBuilderMock).readSubscene(anyInt(),
                                                                          Matchers.<int[]>anyObject(),
                                                                          Matchers.<Variable>anyObject(),
                                                                          anyInt());
        doCallRealMethod().when(subsceneArc3CallBuilderMock).findCentralNetcdfCoords(Matchers.<Variable>any(),
                                                                                     Matchers.<Variable>any(),
                                                                                     anyInt(),
                                                                                     Matchers.<Point>any());
        doCallRealMethod().when(subsceneArc3CallBuilderMock).getAtsrSourceVar(Matchers.<List<Variable>>any(),
                                                                              Matchers.<String>any());
        doCallRealMethod().when(subsceneArc3CallBuilderMock).createSection(anyInt(),
                                                                           Matchers.<int[]>any(),
                                                                           anyInt());
        doCallRealMethod().when(subsceneArc3CallBuilderMock).isSubsceneDimension(Matchers.<NetcdfFileWriteable>any(),
                                                                                 Matchers.<Dimension>any());

        return subsceneArc3CallBuilderMock;
    }

    @Test
    public void testCreateSection() throws Exception {
        final SubsceneArc3CallBuilder subsceneArc3CallBuilder = new SubsceneArc3CallBuilder(null, null);
        final SubsceneArc3CallBuilder.Section section = subsceneArc3CallBuilder.createSection(3, new int[]{30, 28}, 5);

        assertArrayEquals(new int[]{3, 28, 26}, section.origin);
        assertArrayEquals(new int[]{1, 5, 5}, section.shape);
    }

    @Test
    public void testCopyNonSubsceneValues() throws Exception {
        final SubsceneArc3CallBuilder subsceneArc3CallBuilder = new SubsceneArc3CallBuilder(null, null);
        final NetcdfFile source = NetcdfFile.open(netcdfResource);

        subsceneArc3CallBuilder.addSubsceneDimensions(target, source.findVariable(NetcdfFile.escapeName("atsr_orb.someVariable")));
        subsceneArc3CallBuilder.addNonSubsceneDimensions(source, target);
        subsceneArc3CallBuilder.addVariables(source, target);

        final List<String> subsceneNames = new ArrayList<String>();
        subsceneNames.add("atsr_orb.someVariable");
        subsceneNames.add("atsr_orb.latitude");
        subsceneNames.add("atsr_orb.longitude");
        subsceneArc3CallBuilder.copyNonSubsceneValues(source, target, subsceneNames);

        final Array values = target.findVariable("someVariable").read();
        assertEquals(1, values.getFloat(0), 0.00001);
        assertEquals(25, values.getFloat(24), 0.00001);
        assertEquals(101, values.getFloat(25), 0.00001);
        assertEquals(111, values.getFloat(35), 0.00001);
        assertEquals(120, values.getFloat(44), 0.00001);
        assertEquals(125, values.getFloat(49), 0.00001);
    }
}
