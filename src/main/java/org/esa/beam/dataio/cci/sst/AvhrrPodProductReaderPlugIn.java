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
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.bc.ceres.binio.TypeBuilder.*;

/**
 * Plugin for ${@link AvhrrPodProductReader}.
 *
 * @author Thomas Storm
 */
public class AvhrrPodProductReaderPlugIn implements ProductReaderPlugIn {

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        DecodeQualification decodeQualification;
        try {
            decodeQualification = new DecodeQualificationTester(input).getDecodeQualification();
        } catch (IOException e) {
            decodeQualification = DecodeQualification.UNABLE;
        }
        return decodeQualification;
    }

    @Override
    public Class[] getInputTypes() {
        return new Class[]{String.class, File.class};
    }

    @Override
    public ProductReader createReaderInstance() {
        return new AvhrrPodProductReader(this);
    }

    @Override
    public String[] getFormatNames() {
        return new String[]{"AVHRR GAC POD"};
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return new String[0];
    }

    @Override
    public String getDescription(Locale locale) {
        return null;
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return null;
    }

    private static class DecodeQualificationTester {

        private static final CompoundType HEADER_RECORD =
                COMPOUND("header record",
                         MEMBER("spacecraft id", BYTE),
                         MEMBER("datatype", BYTE),
                         MEMBER("date", SEQUENCE(BYTE, 2)),
                         MEMBER("milliseconds", INT),
                         MEMBER("number of scans", SHORT),
                         MEMBER("fill", SEQUENCE(BYTE, 14)),
                         MEMBER("number of data gaps", SHORT),
                         MEMBER("fill", SEQUENCE(BYTE, 3194))
                );

        private RandomAccessFile raf;
        private IOHandler ioHandler;

        private final Object input;
        private static final Map<String, Integer> SPACECRAFT_IDS = new HashMap<String, Integer>(10);

        static {
            SPACECRAFT_IDS.put("TN", 1);
            SPACECRAFT_IDS.put("NH", 1);
            SPACECRAFT_IDS.put("NA", 2);
            SPACECRAFT_IDS.put("NJ", 3);
            SPACECRAFT_IDS.put("NC", 4);
            SPACECRAFT_IDS.put("ND", 5);
            SPACECRAFT_IDS.put("NE", 6);
            SPACECRAFT_IDS.put("NF", 7);
            SPACECRAFT_IDS.put("NG", 8);
        }

        DecodeQualificationTester(Object input) {
            this.input = input;
        }

        DecodeQualification getDecodeQualification() throws IOException {
            final File file = new File(input.toString());
            DataContext context = createHeaderContext(file);

            final SequenceData records = context.getData().getSequence("header");
            final CompoundData header = records.getCompound(0);
            if (!hasCorrectSpaceCraftId(file, header)) {
                return DecodeQualification.UNABLE;
            }

            final byte datatype = header.getByte("datatype");
            if (datatype != 32) {
                return DecodeQualification.UNABLE;
            }

            final SequenceData dateSequence = header.getSequence("date");
            final byte firstByte = dateSequence.getByte(0);
            final byte secondByte = dateSequence.getByte(1);
            if (!isCorrectYear(file, firstByte)) {
                return DecodeQualification.UNABLE;
            }

            if (!isCorrectDay(file, firstByte, secondByte)) {
                return DecodeQualification.UNABLE;
            }

            return DecodeQualification.INTENDED;

        }

        private static boolean isCorrectDay(File file, byte firstByte, byte secondByte) {
            final String s = String.valueOf(AvhrrReaderUtils.toBinaryString(firstByte).charAt(7));
            String s1 = AvhrrReaderUtils.toBinaryString(secondByte);
            s1 = s + s1;
            final int day = Integer.parseInt(s1, 2);
            final String dayInFilename = file.getName().substring(15, 18);
            return Integer.parseInt(dayInFilename) == day;
        }

        private static boolean isCorrectYear(File file, byte firstByte) {
            String s = AvhrrReaderUtils.toBinaryString(firstByte);
            s = s.substring(0, 7);
            s = '0' + s;
            final int year = Integer.parseInt(s, 2);
            final String yearInFilename = file.getName().substring(13, 15);
            return Integer.parseInt(yearInFilename) == year;
        }

        private DataContext createHeaderContext(File file) throws FileNotFoundException {
            raf = new RandomAccessFile(file, "r");
            ioHandler = new RandomAccessFileIOHandler(raf);
            final long fileLength = file.length();
            final int recordSize = HEADER_RECORD.getSize();
            final int numRecords = (int) (fileLength / recordSize);

            final CompoundType headerType = COMPOUND("file", MEMBER("header", SEQUENCE(HEADER_RECORD, numRecords)));
            DataFormat headerDataFormat = new DataFormat(headerType, ByteOrder.BIG_ENDIAN);
            return headerDataFormat.createContext(ioHandler);
        }

        private static boolean hasCorrectSpaceCraftId(File file, CompoundData header) throws IOException {
            final int spaceCraftIdHeader = header.getByte("spacecraft id");
            final String spaceCraftIdFileName = file.getName().substring(9, 11);
            return SPACECRAFT_IDS.get(spaceCraftIdFileName) != null &&
                   SPACECRAFT_IDS.get(spaceCraftIdFileName) == spaceCraftIdHeader;
        }

    }
}
