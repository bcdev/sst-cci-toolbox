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

package org.esa.beam.dataio.cci.sst;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.DataContext;
import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.binio.SequenceData;
import com.bc.ceres.binio.util.RandomAccessFileIOHandler;
import org.esa.beam.framework.datamodel.Product;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.bc.ceres.binio.TypeBuilder.*;
import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class AvhrrPodProductReaderTest {

    private AvhrrPodProductReader reader;
    private File file;

    @Before
    public void setUp() throws Exception {
        final AvhrrPodProductReaderPlugIn plugIn = new AvhrrPodProductReaderPlugIn();
        reader = (AvhrrPodProductReader) plugIn.createReaderInstance();
        final String fileName = getClass().getResource("NSS.GHRR.NH.D91246.S0013.E0154.B1514546.GC").getFile();
        reader.readProductNodes(fileName, null);
        file = new File(fileName);
    }

    @Test
    public void testCreateProduct() throws Exception {
        final Product product = reader.createProduct();
        assertEquals("NSS.GHRR.NH.D91246.S0013.E0154.B1514546.GC", product.getName());
        assertEquals("AVHRR GAC POD", product.getProductType());
        assertEquals(409, product.getSceneRasterWidth());
        assertEquals(12110, product.getSceneRasterHeight());
        assertSame(reader, product.getProductReader());
        assertEquals("NSS.GHRR.NH.D91246.S0013.E0154.B1514546.GC", product.getFileLocation().getName());
        assertEquals(2, product.getTiePointGrids().length);

    }

    @Test
    public void testGetSceneRasterHeight() throws Exception {
        assertEquals(12110, reader.getSceneRasterHeight());
    }

    @Test
    public void testGetStartTime() throws Exception {
        final Date startTime = reader.getStartTime().getAsDate();
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy MM dd hh mm");
        final String formattedStartTime = sdf.format(startTime);
        assertEquals("1991 08 03 00 13", formattedStartTime);
    }

    @Test
    public void testGetEndTime() throws Exception {
        final Date endTime = reader.getEndTime().getAsDate();
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy MM dd hh mm");
        final String formattedEndTime = sdf.format(endTime);
        assertEquals("1991 08 03 01 54", formattedEndTime);
    }

    @Test
    public void testGetNumRecords() throws Exception {
        assertEquals(12112, reader.getNumRecords());
    }

    @Test
    @Ignore
    public void testDataRecord() throws Exception {
        final long fileLength = file.length();
        final int recordSize = AvhrrPodProductReader.GAC_DATA_RECORD.getSize();
        assertEquals(3220, recordSize);

        final int numRecords = (int) (fileLength / recordSize);
        final CompoundType gacFileType = COMPOUND("file", MEMBER("gac file", SEQUENCE(AvhrrPodProductReader.GAC_DATA_RECORD, numRecords)));
        final CompoundType headerType = COMPOUND("file", MEMBER("header", SEQUENCE(AvhrrPodProductReader.HEADER_RECORD, numRecords)));

        DataFormat headerDataFormat = new DataFormat(headerType, ByteOrder.BIG_ENDIAN);
        DataFormat dataFormat = new DataFormat(gacFileType, ByteOrder.BIG_ENDIAN);

        final RandomAccessFile raf = new RandomAccessFile(file, "r");
        final RandomAccessFileIOHandler ioHandler = new RandomAccessFileIOHandler(raf);
        DataContext dataContext = dataFormat.createContext(ioHandler);

        DataContext headerContext = headerDataFormat.createContext(ioHandler);
        final SequenceData headerRecords = headerContext.getData().getSequence("header");
        final CompoundData header = headerRecords.getCompound(0);
        System.out.println(header.getShort("number of scans"));

        final SequenceData records = dataContext.getData().getSequence("gac file");
        for (int i = 0; i < records.getElementCount(); i++) {
//            System.out.println("record# = " + i);
//
//            final CompoundData aRecord = records.getCompound(i);
//            final short scanLineNumber = aRecord.getShort(0);
//            System.out.println("scanLineNumber = " + scanLineNumber);

//                System.out.println("#" + String.format("%32s", Integer.toBinaryString(aRecord.getInt("quality indicators"))));
//                System.out.println(aRecord.getInt("validCount"));
//                System.out.println("===================================");
        }

//
//            final TiePointGrid latGrid = new TiePointGrid("lat", 409, 0, 4, 0, 8, 0, null);
//            final TiePointGrid lonGrid = new TiePointGrid("lon", 409, 0, 4, 0, 8, 0, null);
//            new TiePointGeoCoding(latGrid, lonGrid);

    }
}
