package org.esa.cci.sst.reader;

import org.junit.Test;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Structure;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

public class SeaIceObservationReaderTest {

    private static final File TEST_FILE = new File("testdata/SeaIceConc", "ice_conc_sh_qual_201006301200.hdf");

    @Test
    public void testReadObservation() throws IOException {
        assertTrue(NetcdfFile.canOpen(TEST_FILE.getPath()));

        final NetcdfFile ncFile = NetcdfFile.open(TEST_FILE.getPath());
        assertNotNull(ncFile);

        final List<Variable> variableList = ncFile.getVariables();
        assertFalse(variableList.isEmpty());
        for (Variable v : variableList) {
            System.out.println("v.getName() = " + v.getName());
        }

        final Variable header = ncFile.findVariable("Header");
        assertTrue(header instanceof Structure);
        final List<Variable> headerVariables = ((Structure) header).getVariables();
        for (Variable v : headerVariables) {
            System.out.println("v.getName() = " + v.getName());
            switch (v.getDataType()) {
                case CHAR:
                    System.out.println(v.readScalarString());
                    break;
                case FLOAT:
                    System.out.println(v.readScalarFloat());
                    break;
                case INT:
                    System.out.println(v.readScalarInt());
                    break;
                case SHORT:
                    System.out.println(v.readScalarShort());
                    break;
            }
        }
        final List<Attribute> attributeList = header.getAttributes();
        for (Attribute a : attributeList) {
            System.out.println("a.getName() = " + a.getName());
            System.out.println("a.getValue() = " + a.getValue(0));
        }
        final Variable data = ncFile.findVariable("Data/data[00]");
        assertNotNull(data);

        System.out.println(ncFile.toString());
    }
}
