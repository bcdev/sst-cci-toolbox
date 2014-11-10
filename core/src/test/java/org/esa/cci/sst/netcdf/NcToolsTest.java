package org.esa.cci.sst.netcdf;

import org.junit.Test;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class NcToolsTest {

    @Test
    public void testGetVariable() throws IOException {
        final NetcdfFile netcdfFile = mock(NetcdfFile.class);
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
        final NetcdfFile netcdfFile = mock(NetcdfFile.class);
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
}
