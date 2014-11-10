package org.esa.cci.sst.netcdf;

import org.esa.cci.sst.common.GridDef;
import org.junit.Before;
import org.junit.Test;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.awt.*;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
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
        final Variable mockVariable = mock(Variable.class);
        when(mockVariable.getRank()).thenReturn(2);

        final Dimension y_dim = mock(Dimension.class);
        when(y_dim.getLength()).thenReturn(10);

        final Dimension x_dim = mock(Dimension.class);
        when(x_dim.getLength()).thenReturn(20);

        when(mockVariable.getDimension(0)).thenReturn(y_dim);
        when(mockVariable.getDimension(1)).thenReturn(x_dim);

        final GridDef gridDef = GridDef.createGlobal(20, 10);


        final Rectangle gridRectangle = NcTools.getGridRectangle(netcdfFile, mockVariable, gridDef);
        assertNotNull(gridRectangle);
        assertEquals(0, gridRectangle.getX(), 1e-8);
        assertEquals(0, gridRectangle.getY(), 1e-8);
        assertEquals(20, gridRectangle.getWidth(), 1e-8);
        assertEquals(10, gridRectangle.getHeight(), 1e-8);

        verify(mockVariable, times(1)).getRank();
        verify(mockVariable, times(1)).getDimension(0);
        verify(mockVariable, times(1)).getDimension(1);
        verifyNoMoreInteractions(netcdfFile);
    }
}
