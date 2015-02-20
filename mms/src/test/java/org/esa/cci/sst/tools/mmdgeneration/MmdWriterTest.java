package org.esa.cci.sst.tools.mmdgeneration;


import org.esa.cci.sst.data.Column;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.tools.Constants;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import static org.mockito.Mockito.*;

@SuppressWarnings("deprecation")
public class MmdWriterTest {

    @Test
    public void testConstruct() throws IOException {
        final NetcdfFileWriter fileWriter = mock(NetcdfFileWriter.class);
        final HashMap<String, Integer> dimensions = new HashMap<>();
        dimensions.put("x", 108);
        dimensions.put("y", 106);
        final ArrayList<Item> variables = new ArrayList<>();
        final Column column = new Column();
        column.setName("the_column");
        column.setType("FLOAT");
        column.setDimensions("x,y");
        variables.add(column);

        new MmdWriter(fileWriter, 28, dimensions, variables);

        verify(fileWriter, times(1)).setLargeFile(true);
        verify(fileWriter, times(1)).addDimension(null, Constants.DIMENSION_NAME_MATCHUP, 28);
        verify(fileWriter, times(1)).addDimension(null, "x", 108);
        verify(fileWriter, times(1)).addDimension(null, "y", 106);
        verify(fileWriter, times(1)).addGroupAttribute(null, new Attribute("title", "SST CCI multi-sensor match-up dataset (MMD)"));
        verify(fileWriter, times(1)).addGroupAttribute(null, new Attribute("institution", "Brockmann Consult"));
        verify(fileWriter, times(1)).addGroupAttribute(null, new Attribute("contact", "Ralf Quast (ralf.quast@brockmann-consult.de)"));
        // due to the varying date this can not be tested. Unfortunately, matchers are only allowed when all arguments are provided by
        // matchers, which does not make sense here tb 2015-02-10
        //verify(fileWriter, times(1)).addGroupAttribute(null, new Attribute("creation_date", "data.popate"));
        verify(fileWriter, times(1)).addGroupAttribute(null, new Attribute("total_number_of_matchups", 28));
        verify(fileWriter, times(1)).addVariable(null, "the_column", DataType.FLOAT, "x,y");
        verify(fileWriter, times(1)).create();
    }

    @Test
    public void testClose() throws IOException {
        final NetcdfFileWriter fileWriter = mock(NetcdfFileWriter.class);

        final MmdWriter mmdWriter = new MmdWriter(fileWriter, 28, new HashMap<String, Integer>(), new ArrayList<Item>());
        mmdWriter.close();

        verify(fileWriter, times(1)).close();
    }

    @Test
    public void testWrite() throws IOException, InvalidRangeException {
        final NetcdfFileWriter fileWriter = mock(NetcdfFileWriter.class);
        final Variable variable = mock(Variable.class);
        final Array array = mock(Array.class);
        final int[] origin = {5, 6};

        final MmdWriter mmdWriter = new MmdWriter(fileWriter, 28, new HashMap<String, Integer>(), new ArrayList<Item>());
        mmdWriter.write(variable, origin, array);

        verify(fileWriter, times(1)).write(variable, origin, array);
    }
}
