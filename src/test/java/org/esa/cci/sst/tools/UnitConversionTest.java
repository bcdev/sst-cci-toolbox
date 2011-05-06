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

package org.esa.cci.sst.tools;

import org.esa.cci.sst.ColumnRegistry;
import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.ColumnI;
import org.esa.cci.sst.rules.Converter;
import org.esa.cci.sst.rules.Rule;
import org.esa.cci.sst.rules.RuleException;
import org.esa.cci.sst.rules.RuleFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ucar.ma2.DataType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Illustrates how a unit conversion can be carried out.
 *
 * @author Ralf Quast
 */
public class UnitConversionTest {

    private static final String TIME_VARIABLE_NAME = "time";
    private static final DataType TIME_VARIABLE_TYPE = DataType.DOUBLE;
    private static final double JULIAN_DATE_OF_EPOCH_1978 = 2443509.5;

    private ColumnRegistry registry = ColumnRegistry.getInstance();

    @Test
    public void testColumnConversion() {
        final ColumnI targetColumn = registry.getColumn(TIME_VARIABLE_NAME);

        assertEquals(TIME_VARIABLE_NAME, targetColumn.getName());
        assertEquals(TIME_VARIABLE_TYPE.name(), targetColumn.getType());
        assertEquals("seconds since 1978-01-01 00:00:00", targetColumn.getUnit());

        final ColumnI sourceColumn = registry.getSourceColumn(targetColumn);

        assertNotNull(sourceColumn);
        assertEquals(TIME_VARIABLE_NAME, sourceColumn.getName());
        assertEquals(TIME_VARIABLE_TYPE.name(), sourceColumn.getType());
        assertEquals("Julian Date", sourceColumn.getUnit());
    }

    @Test
    public void testNumericConversion() throws RuleException {
        final ColumnI targetColumn = registry.getColumn(TIME_VARIABLE_NAME);

        assertNotNull(targetColumn);

        final Converter converter = registry.getConverter(targetColumn);

        assertNotNull(converter);
        assertEquals(0.0, converter.apply(JULIAN_DATE_OF_EPOCH_1978));
        assertEquals(86400.0, converter.apply(JULIAN_DATE_OF_EPOCH_1978 + 1.0));
    }

    @Before
    public void initRegistry() throws Exception {
        final Rule rule = RuleFactory.getInstance().getRule("JulianDateToSeconds");
        final ColumnBuilder builder = new ColumnBuilder();
        builder.setName(TIME_VARIABLE_NAME);
        builder.setType(TIME_VARIABLE_TYPE);
        builder.setUnit("Julian Date");

        registry.register(rule, builder.build());
    }

    @After
    public void clearRegistry() {
        registry.clear();
    }
}
