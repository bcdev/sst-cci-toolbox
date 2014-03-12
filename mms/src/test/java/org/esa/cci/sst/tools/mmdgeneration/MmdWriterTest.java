package org.esa.cci.sst.tools.mmdgeneration;

import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.tools.Constants;
import org.esa.cci.sst.util.IoUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFileWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({IoUtil.class})
public class MmdWriterTest {

    private MmdWriter mmdWriter;
    private NetcdfFileWriter fileWriter;

    @Before
    public void setUp() {
        fileWriter = mock(NetcdfFileWriter.class);
        mockStatic(IoUtil.class);

        mmdWriter = new MmdWriter(fileWriter);
    }

    @Test
    public void testInitialize() throws IOException {
        final HashMap<String, Integer> dimensionConfig = new HashMap<>();
        dimensionConfig.put("left", 67);
        dimensionConfig.put("right", 108);
        final int matchupCount = 34;

        final List<Item> variableList = new ArrayList<>();
        final Item variable_1 = new ColumnBuilder().name("variable_1").build();
        final Item variable_2 = new ColumnBuilder().name("variable_2").build();
        variableList.add(variable_1);
        variableList.add(variable_2);

        mmdWriter.initialize(matchupCount, dimensionConfig, variableList);

        verify(fileWriter, times(1)).addDimension(null, Constants.DIMENSION_NAME_MATCHUP, matchupCount);
        verify(fileWriter, times(1)).addDimension(null, "left", 67);
        verify(fileWriter, times(1)).addDimension(null, "right", 108);

        verify(fileWriter, times(1)).addGroupAttribute(null, new Attribute("title", "SST CCI multi-sensor match-up dataset (MMD) template"));
        verify(fileWriter, times(1)).addGroupAttribute(null, new Attribute("institution", "Brockmann Consult"));
        verify(fileWriter, times(1)).addGroupAttribute(null, new Attribute("contact", "Ralf Quast (ralf.quast@brockmann-consult.de)"));
        verify(fileWriter, times(1)).addGroupAttribute(null, new Attribute("total_number_of_matchups", matchupCount));
        verify(fileWriter, times(5)).addGroupAttribute((Group) isNull(), any(Attribute.class));     // this is a crude workaround because we can not match the varying date here tb 2014-03-12

        verify(fileWriter, times(1)).create();

        verifyNoMoreInteractions(fileWriter);

        verifyStatic();
        IoUtil.addVariable(fileWriter, variable_1);

        verifyStatic();
        IoUtil.addVariable(fileWriter, variable_2);
    }

}
