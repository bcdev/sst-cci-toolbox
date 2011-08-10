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
import com.bc.ceres.binio.IOHandler;
import com.bc.ceres.binio.SequenceData;
import com.bc.ceres.binio.util.RandomAccessFileIOHandler;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;

import static com.bc.ceres.binio.TypeBuilder.*;
import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class AvhrrPodProductReaderPlugInTest {

    private RandomAccessFile raf;
    private IOHandler ioHandler;

    private static final CompoundType GAC_DATA_RECORD =
            COMPOUND("GAC record",
                     MEMBER("scan line number from 1 to n", SHORT),
                     MEMBER("date", SEQUENCE(BYTE, 2)),
                     MEMBER("milliseconds", INT),
                     MEMBER("quality indicators", INT),
                     MEMBER("calibration coefficients", SEQUENCE(BYTE, 40)),
                     MEMBER("validCount", UBYTE),
                     MEMBER("sza", SEQUENCE(BYTE, 51)),
                     MEMBER("earth locations", SEQUENCE(BYTE, 204)),
                     MEMBER("telemetry", SEQUENCE(BYTE, 140)),
                     MEMBER("video", SEQUENCE(BYTE, 2728)),
                     MEMBER("sza plus", SEQUENCE(BYTE, 20)),
                     MEMBER("clock drift", SEQUENCE(BYTE, 2)),
                     MEMBER("spare", SEQUENCE(BYTE, 22))
            );

    private static final CompoundType HEADER_RECORD =
            COMPOUND("header record",
                     MEMBER("spacecraft id", BYTE),
                     MEMBER("datatype", BYTE),
                     MEMBER("date", SEQUENCE(BYTE, 2)),
                     MEMBER("milliseconds", INT),
                     MEMBER("number of scans", SHORT),
                     MEMBER("fill", SEQUENCE(BYTE, 14)),
                     MEMBER("number of data gaps", SHORT)
            );
    private short numScans;

    @Test
    public void testGetDecodeQualification() throws Exception {
        final AvhrrPodProductReaderPlugIn plugIn = new AvhrrPodProductReaderPlugIn();
        final DecodeQualification decodeQualification91 = plugIn.getDecodeQualification(getClass().getResource("NSS.GHRR.NH.D91246.S0013.E0154.B1514546.GC").getFile());
        final DecodeQualification decodeQualification94 = plugIn.getDecodeQualification(getClass().getResource("NSS.GHRR.ND.D94062.S0103.E0257.B1454446.GC").getFile());
        final DecodeQualification decodeQualification95 = plugIn.getDecodeQualification(getClass().getResource("NSS.GHRR.ND.D95095.S0050.E0244.B2020204.GC").getFile());
        final DecodeQualification decodeQualification96 = plugIn.getDecodeQualification(getClass().getResource("NSS.GHRR.NJ.D96153.S1331.E1514.B0732425.GC").getFile());
        final DecodeQualification decodeQualification10 = plugIn.getDecodeQualification(getClass().getResource("NSS.GHRR.NP.D10312.S0739.E0921.B0902324.GC").getFile());

        assertEquals(DecodeQualification.INTENDED, decodeQualification91);
        assertEquals(DecodeQualification.INTENDED, decodeQualification94);
        assertEquals(DecodeQualification.INTENDED, decodeQualification95);
        assertEquals(DecodeQualification.INTENDED, decodeQualification96);
        assertEquals(DecodeQualification.UNABLE, decodeQualification10);
    }

    @Test
    @Ignore
    public void test91() throws Exception {
        final File file = new File(getClass().getResource("NSS.GHRR.NH.D91246.S0013.E0154.B1514546.GC").getFile());
        testSth(file);
    }

    public void testSth(File file) throws Exception {
        try {
            raf = new RandomAccessFile(file, "r");
        } catch (FileNotFoundException ignored) {
        }
        ioHandler = new RandomAccessFileIOHandler(raf);
        final long fileLength = file.length();

        final int recordSize = GAC_DATA_RECORD.getSize();
        assertEquals(3220, recordSize);

        final int numRecords = (int) (fileLength / recordSize);
//        assertEquals(13688, numRecords);

        final long calculatedFileLength = numRecords * recordSize;
        assertEquals(fileLength, calculatedFileLength);

        final CompoundType gacFileType = COMPOUND("file", MEMBER("gac file", SEQUENCE(GAC_DATA_RECORD, numRecords)));
        final CompoundType headerType = COMPOUND("file", MEMBER("header", SEQUENCE(HEADER_RECORD, numRecords)));

        DataFormat headerDataFormat = new DataFormat(headerType, ByteOrder.BIG_ENDIAN);
        DataFormat dataFormat = new DataFormat(gacFileType, ByteOrder.BIG_ENDIAN);
        testHeaderRecord(headerDataFormat);
        testDataRecord(dataFormat);
    }

    private void testHeaderRecord(DataFormat headerDataFormat) throws IOException {
        DataContext context = headerDataFormat.createContext(ioHandler);
        final SequenceData records = context.getData().getSequence("header");
        final CompoundData header = records.getCompound(0);
        final byte spaceCraftId = header.getByte("spacecraft id");
        System.out.println("spaceCraftId = " + spaceCraftId);
        final byte datatype = header.getByte("datatype");
        System.out.println("datatype = " + datatype);
        final SequenceData dateSequence = header.getSequence("date");
        final byte firstByte = dateSequence.getByte(0);
        String s = toBinaryString(firstByte);
        s = s.substring(0, 7);
        s = "0" + s;
        final int year = Integer.parseInt(s, 2);
        final byte secondByte = dateSequence.getByte(1);
        s = "" + toBinaryString(firstByte).charAt(7);
        String s1 = toBinaryString(secondByte);
        s1 = s + s1;
        final int day = Integer.parseInt(s1, 2);
        System.out.println("year = " + year);
        System.out.println("day = " + day);
        final short numDataGaps = header.getShort("number of data gaps");
        System.out.println("numDataGaps = " + numDataGaps);
        numScans = header.getShort("number of scans");
        System.out.println("numScans = " + numScans);
    }

    private void testDataRecord(DataFormat dataFormat) {
        DataContext context = null;
        try {
            context = dataFormat.createContext(ioHandler);
            final SequenceData records = context.getData().getSequence("gac file");
            System.out.println("records.getElementCount() = " + records.getElementCount());
            for (int i = 2; i <= numScans + 1; i++) {
                System.out.println("record# = " + i);

                final CompoundData aRecord = records.getCompound(i);
                final short scanLineNumber = aRecord.getShort(0);
                System.out.println("scanLineNumber = " + scanLineNumber);

                //time
                final SequenceData dateSequence = aRecord.getSequence(1);
                final byte firstByte = dateSequence.getByte(0);
                String s = toBinaryString(firstByte);
                s = s.substring(0, 7);
                s = "0" + s;
                final int year = Integer.parseInt(s, 2);
//                assertEquals(95, year);

                final byte secondByte = dateSequence.getByte(1);
                s = "" + toBinaryString(firstByte).charAt(7);
                String s1 = toBinaryString(secondByte);
                s1 = s + s1;
                final int day = Integer.parseInt(s1, 2);
//                assertEquals(95, day);

                final int milliseconds = aRecord.getInt("milliseconds");
                int hour = milliseconds / (60 * 60 * 1000);
                int minute = (milliseconds / (60 * 1000)) % 60;
                int seconds = (milliseconds / 1000) % 60;
                int millis = (milliseconds % 1000);
//                System.out.println("time = " + hour + ":" + minute + ":" + seconds + "::" + millis);

//                assertEquals(0, hour);
//                assertEquals(50, minute);

//                System.out.println("#" + String.format("%32s", Integer.toBinaryString(aRecord.getInt("quality indicators"))));
//                System.out.println(aRecord.getInt("validCount"));
//                System.out.println("===================================");
            }

//
//            final TiePointGrid latGrid = new TiePointGrid("lat", 409, 0, 4, 0, 8, 0, null);
//            final TiePointGrid lonGrid = new TiePointGrid("lon", 409, 0, 4, 0, 8, 0, null);
//            new TiePointGeoCoding(latGrid, lonGrid);

        } catch (IOException ignore) {
        } finally {
            if (context != null) {
                context.dispose();
            }
        }
    }

    public static String toBinaryString(byte n) {
        StringBuilder sb = new StringBuilder("00000000");
        for (int bit = 0; bit < 8; bit++) {
            if (((n >> bit) & 1) > 0) {
                sb.setCharAt(7 - bit, '1');
            }
        }
        return sb.toString();
    }


}
