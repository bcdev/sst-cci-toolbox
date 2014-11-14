/*
 * Copyright (c) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.cci.sst.netcdf;

import org.esa.cci.sst.grid.GridDef;
import org.junit.Before;
import org.junit.Test;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.awt.*;
import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class NcToolsTest {

    private NetcdfFile netcdfFile;

    @Before
    public void setUp() {
        netcdfFile = mock(NetcdfFile.class);
    }

    @Test
    public void testGetVariable() throws IOException {
        final Variable mockVariable = mock(Variable.class);
        final String variableName = "blablupp";

        when(netcdfFile.findVariable(variableName)).thenReturn(mockVariable);

        final Variable variable = NcTools.getVariable(netcdfFile, variableName);
        assertNotNull(variable);

        verify(netcdfFile, times(1)).findVariable(variableName);
        verifyNoMoreInteractions(netcdfFile);
    }

    @Test
    public void testGetVariable_invalidVariableName() throws IOException {
        final String variableName = "schnickschnack";

        when(netcdfFile.findVariable(variableName)).thenReturn(null);

        try {
            NcTools.getVariable(netcdfFile, variableName);
            fail("IOException expected");
        } catch (IOException expected) {
        }

        verify(netcdfFile, times(1)).findVariable(variableName);
        verify(netcdfFile, times(1)).getLocation();
        verifyNoMoreInteractions(netcdfFile);
    }

    @Test
    public void testGetGridRectangle_variableRankIncorrect() {
        final Variable mockVariable = mock(Variable.class);
        when(mockVariable.getRank()).thenReturn(1);

        try {
            NcTools.getGridRectangle(netcdfFile, mockVariable, GridDef.createGlobal(2.9));
            fail("IOException expected");
        } catch (IOException expected) {
        }

        verify(mockVariable, times(1)).getRank();
        verify(netcdfFile, times(1)).getLocation();
        verifyNoMoreInteractions(netcdfFile);
    }

    @Test
    public void testGetGridRectangle_incorrectDimensions() {
        final Variable mockVariable = mock(Variable.class);
        when(mockVariable.getRank()).thenReturn(2);

        final Dimension y_dim = mock(Dimension.class);
        when(y_dim.getLength()).thenReturn(11);

        final Dimension x_dim = mock(Dimension.class);
        when(x_dim.getLength()).thenReturn(19);

        when(mockVariable.getDimension(0)).thenReturn(y_dim);
        when(mockVariable.getDimension(1)).thenReturn(x_dim);

        final GridDef gridDef = GridDef.createGlobal(20, 10);

        try {
            NcTools.getGridRectangle(netcdfFile, mockVariable, gridDef);
            fail("IOException expected");
        } catch (IOException expected) {
        }

        verify(mockVariable, times(1)).getRank();
        verify(mockVariable, times(1)).getDimension(0);
        verify(mockVariable, times(1)).getDimension(1);
        verify(netcdfFile, times(1)).getLocation();
        verifyNoMoreInteractions(netcdfFile);
    }

    @Test
    public void testGetGridRectangle() throws IOException {
        final Variable variable = mock(Variable.class);
        when(variable.getRank()).thenReturn(2);

        final Dimension y_dim = mock(Dimension.class);
        when(y_dim.getLength()).thenReturn(10);

        final Dimension x_dim = mock(Dimension.class);
        when(x_dim.getLength()).thenReturn(20);

        when(variable.getDimension(0)).thenReturn(y_dim);
        when(variable.getDimension(1)).thenReturn(x_dim);

        final GridDef gridDef = GridDef.createGlobal(20, 10);


        final Rectangle gridRectangle = NcTools.getGridRectangle(netcdfFile, variable, gridDef);
        assertNotNull(gridRectangle);
        assertEquals(0, gridRectangle.getX(), 1e-8);
        assertEquals(0, gridRectangle.getY(), 1e-8);
        assertEquals(20, gridRectangle.getWidth(), 1e-8);
        assertEquals(10, gridRectangle.getHeight(), 1e-8);

        verify(variable, times(1)).getRank();
        verify(variable, times(1)).getDimension(0);
        verify(variable, times(1)).getDimension(1);
        verifyNoMoreInteractions(netcdfFile);
    }

    @Test
    public void testGetNumericAttributeValue() {
        final String attributeName = "an_attribute";
        final Variable variable = mock(Variable.class);
        final Attribute attribute = mock(Attribute.class);

        when(attribute.getNumericValue()).thenReturn(108);
        when(variable.findAttribute(attributeName)).thenReturn(attribute);

        final Number value = NcTools.getNumericAttributeValue(variable, attributeName);
        assertEquals(108, value.longValue());

        verify(variable, times(1)).findAttribute(attributeName);
        verify(attribute, times(1)).getNumericValue();
        verifyNoMoreInteractions(variable);
        verifyNoMoreInteractions(attribute);
    }

    @Test
    public void testGetNumericAttributeValue_attributeNotPresent() {
        final Variable variable = mock(Variable.class);

        when(variable.findAttribute(anyString())).thenReturn(null);

        final Number value = NcTools.getNumericAttributeValue(variable, "is_not_there");
        assertNull(value);

        verify(variable, times(1)).findAttribute(anyString());
        verifyNoMoreInteractions(variable);
    }

    @Test
    public void testGetNumericAttributeValue_withDefault() {
        final String attributeName = "the_attribute";
        final Variable variable = mock(Variable.class);
        final Attribute attribute = mock(Attribute.class);

        when(attribute.getNumericValue()).thenReturn(109.7);
        when(variable.findAttribute(attributeName)).thenReturn(attribute);

        final Number value = NcTools.getNumericAttributeValue(variable, attributeName, 22.8);
        assertEquals(109.7, value.doubleValue(), 1e-8);

        verify(variable, times(1)).findAttribute(attributeName);
        verify(attribute, times(1)).getNumericValue();
        verifyNoMoreInteractions(variable);
        verifyNoMoreInteractions(attribute);
    }

    @Test
    public void testGetNumericAttributeValue_withDefault_defaultValueReturned() {
        final Variable variable = mock(Variable.class);

        when(variable.findAttribute(anyString())).thenReturn(null);

        final Number value = NcTools.getNumericAttributeValue(variable, "is_not_there", 29.66);
        assertNotNull(value);
        assertEquals(29.66, value.doubleValue(), 1e-8);

        verify(variable, times(1)).findAttribute(anyString());
        verifyNoMoreInteractions(variable);
    }

    @Test
    public void testGetScaleFactor() {
        final Variable variable = mock(Variable.class);
        final Attribute attribute = mock(Attribute.class);

        when(attribute.getNumericValue()).thenReturn(14.881);
        when(variable.findAttribute("scale_factor")).thenReturn(attribute);

        assertEquals(14.881, NcTools.getScaleFactor(variable), 1e-8);

        verify(variable, times(1)).findAttribute("scale_factor");
        verifyNoMoreInteractions(variable);
    }

    @Test
    public void testGetScaleFactor_noScalefactorAttribute() {
        final Variable variable = mock(Variable.class);

        when(variable.findAttribute("scale_factor")).thenReturn(null);

        assertEquals(1.0, NcTools.getScaleFactor(variable), 1e-8);

        verify(variable, times(1)).findAttribute("scale_factor");
        verifyNoMoreInteractions(variable);
    }

    @Test
    public void testGetAddOffset() {
        final Variable variable = mock(Variable.class);
        final Attribute attribute = mock(Attribute.class);

        when(attribute.getNumericValue()).thenReturn(15.812);
        when(variable.findAttribute("add_offset")).thenReturn(attribute);

        assertEquals(15.812, NcTools.getAddOffset(variable), 1e-8);

        verify(variable, times(1)).findAttribute("add_offset");
        verifyNoMoreInteractions(variable);
    }

    @Test
    public void testGetAddOffset_noOffsetAttribute() {
        final Variable variable = mock(Variable.class);

        when(variable.findAttribute("add_offset")).thenReturn(null);

        assertEquals(0.0, NcTools.getAddOffset(variable), 1e-8);

        verify(variable, times(1)).findAttribute("add_offset");
        verifyNoMoreInteractions(variable);
    }

    @Test
    public void testGetFillValue() {
        final Variable variable = mock(Variable.class);
        final Attribute attribute = mock(Attribute.class);

        when(attribute.getNumericValue()).thenReturn(16.108);
        when(variable.findAttribute("_FillValue")).thenReturn(attribute);

        final Number fillValue = NcTools.getFillValue(variable);
        assertNotNull(fillValue);
        assertEquals(16.108, fillValue.doubleValue(), 1e-8);

        verify(variable, times(1)).findAttribute("_FillValue");
        verifyNoMoreInteractions(variable);
    }

    @Test
    public void testGetFillValue_noFillValuePresent() {
        final Variable variable = mock(Variable.class);

        when(variable.findAttribute("_FillValue")).thenReturn(null);

        final Number fillValue = NcTools.getFillValue(variable);
        assertNull(fillValue);

        verify(variable, times(1)).findAttribute("_FillValue");
        verifyNoMoreInteractions(variable);
    }

}
