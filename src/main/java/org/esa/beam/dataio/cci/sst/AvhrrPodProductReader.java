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
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static com.bc.ceres.binio.TypeBuilder.*;

/**
 * Reads NOAA POD AVHRR GAC files as BEAM products, according to specification provided in the NOAA Polar Orbiter Data
 * User's Guide, Section 3.1 (http://www.ncdc.noaa.gov/oa/pod-guide/ncdc/docs/podug/html/c3/sec3-1.htm).
 *
 * @author Thomas Storm
 */
public class AvhrrPodProductReader extends AbstractProductReader {

    private static final String EARTH_LOCATIONS = "earth locations";
    private static final String SCAN_LINE_NUMBER = "scan line number from 1 to n";
    private static final String QUALITY_INDICATORS = "quality indicators";
    private static final String DATE = "date";
    private static final String MILLISECONDS = "milliseconds";
    private static final String VALID_COUNT = "valid count";
    private static final String VIDEO = "video";
    private static final String NUMBER_OF_SCANS = "number of scans";

    static final CompoundType GAC_DATA_RECORD =
            COMPOUND("GAC record",
                     MEMBER(SCAN_LINE_NUMBER, SHORT),
                     MEMBER(DATE, SEQUENCE(BYTE, 2)),
                     MEMBER(MILLISECONDS, INT),
                     MEMBER(QUALITY_INDICATORS, INT),
                     MEMBER("calibration coefficients", SEQUENCE(BYTE, 40)),
                     MEMBER(VALID_COUNT, UBYTE),
                     MEMBER("sza", SEQUENCE(BYTE, 51)),
                     MEMBER(EARTH_LOCATIONS, SEQUENCE(SHORT, 102)),
                     MEMBER("telemetry", SEQUENCE(BYTE, 140)),
                     MEMBER(VIDEO, SEQUENCE(BYTE, 2728)),
                     MEMBER("sza plus", SEQUENCE(BYTE, 20)),
                     MEMBER("clock drift", SEQUENCE(BYTE, 2)),
                     MEMBER("spare", SEQUENCE(BYTE, 22))
            );

    static final CompoundType HEADER_RECORD =
            COMPOUND("header record",
                     MEMBER("spacecraft id", BYTE),
                     MEMBER("datatype", BYTE),
                     MEMBER(DATE, SEQUENCE(BYTE, 2)),
                     MEMBER("milliseconds", INT),
                     MEMBER(NUMBER_OF_SCANS, SHORT),
                     MEMBER("fill", SEQUENCE(BYTE, 14)),
                     MEMBER("number of data gaps", SHORT),
                     MEMBER("fill", SEQUENCE(BYTE, 3194))
            );

    private File file;
    private IOHandler ioHandler;
    private final List<CompoundData> scanlines = new ArrayList<CompoundData>(14000);

    /**
     * Constructs a new abstract product reader.
     *
     * @param plugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *               implementations
     */
    protected AvhrrPodProductReader(ProductReaderPlugIn plugIn) {
        super(plugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        file = new File(getInput().toString());
        final RandomAccessFile raf = new RandomAccessFile(file, "r");
        ioHandler = new RandomAccessFileIOHandler(raf);
        readRecords();
        return createProduct();
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand,
                                          int destOffsetX, int destOffsetY, int destWidth, int destHeight,
                                          ProductData destBuffer, ProgressMonitor pm) throws IOException {
        final RenderedImage image = destBand.getSourceImage();
        final Raster data = image.getData(new Rectangle(destOffsetX, destOffsetY, destWidth, destHeight));
        data.getDataElements(destOffsetX, destOffsetY, destWidth, destHeight, destBuffer.getElems());
    }

    Product createProduct() throws IOException {
        final Product product = new Product(file.getName(), getReaderPlugIn().getFormatNames()[0], 409, scanlines.size(), this);
        product.setFileLocation(file);
        product.setStartTime(getStartTime());
        product.setEndTime(getEndTime());
        setGeoCoding(product);
//        addBands(product);
        return product;
    }

    void addBands(Product product) throws IOException {
        final String spaceCraftIdFileName = file.getName().substring(9, 11);
        int bandCount = (Arrays.asList("TN", "NA", "NE", "NG").contains(spaceCraftIdFileName)) ? 4 : 5;
        for (int i = 0; i < bandCount; i++) {
            product.addBand("Channel " + (i + 1), ProductData.TYPE_FLOAT32);
        }
    }

    ProductData.UTC getStartTime() throws IOException {
        final CompoundData firstRecord = scanlines.get(0);

        final SequenceData dateSequence = firstRecord.getSequence(1);
        final byte firstByte = dateSequence.getByte(0);
        final byte secondByte = dateSequence.getByte(1);

        final int year = getYear(firstByte);
        final int day = getDay(firstByte, secondByte);
        final int milliseconds = firstRecord.getInt("milliseconds");

        final GregorianCalendar calendar = new GregorianCalendar();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.DAY_OF_YEAR, day);
        calendar.set(Calendar.HOUR_OF_DAY, milliseconds / (60 * 60 * 1000));
        calendar.set(Calendar.MINUTE, (milliseconds / (60 * 1000)) % 60);
        calendar.set(Calendar.SECOND, (milliseconds / 1000) % 60);
        calendar.set(Calendar.MILLISECOND, (milliseconds % 1000));

        return ProductData.UTC.create(calendar.getTime(), 0);
    }

    ProductData.UTC getEndTime() throws IOException {
        final CompoundData lastRealRecord = scanlines.get(scanlines.size() - 1);

        final SequenceData dateSequence = lastRealRecord.getSequence(1);
        final byte firstByte = dateSequence.getByte(0);
        final byte secondByte = dateSequence.getByte(1);

        final int year = getYear(firstByte);
        final int day = getDay(firstByte, secondByte);
        final int milliseconds = lastRealRecord.getInt("milliseconds");

        final GregorianCalendar calendar = new GregorianCalendar();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.DAY_OF_YEAR, day);
        calendar.set(Calendar.HOUR_OF_DAY, milliseconds / (60 * 60 * 1000));
        calendar.set(Calendar.MINUTE, (milliseconds / (60 * 1000)) % 60);
        calendar.set(Calendar.SECOND, (milliseconds / 1000) % 60);
        calendar.set(Calendar.MILLISECOND, (milliseconds % 1000));

        return ProductData.UTC.create(calendar.getTime(), 0);
    }

    void readRecords() throws IOException {
        final CompoundType gacFileType = COMPOUND("file",
                                                  MEMBER("header", HEADER_RECORD),
                                                  MEMBER("gac file", SEQUENCE(GAC_DATA_RECORD, getNumRecords())));
        DataFormat dataFormat = new DataFormat(gacFileType, ByteOrder.BIG_ENDIAN);
        final DataContext context = dataFormat.createContext(ioHandler);
        final SequenceData records = context.getData().getSequence("gac file");
        for (int i = 0; i < records.getElementCount(); i++) {
            final CompoundData compound = records.getCompound(i);
            scanlines.add(compound);
        }
        removeInvalidScanlines();
        sortScanlines();
    }

    private void sortScanlines() {
        Collections.sort(scanlines, new Comparator<CompoundData>() {
            @Override
            public int compare(CompoundData o1, CompoundData o2) {
                try {
                    final String byteName = SCAN_LINE_NUMBER;
                    return o1.getShort(byteName) < o2.getShort(byteName) ? -1 : 1;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void removeInvalidScanlines() throws IOException {
        final List<CompoundData> toRemove = new ArrayList<CompoundData>(3);
        final Set<Short> scanlineNumbers = new HashSet<Short>(14000);
        for (CompoundData scanline : scanlines) {
            final short scanlineNumber = scanline.getShort(SCAN_LINE_NUMBER);
            final boolean isDuplicate = scanlineNumbers.contains(scanlineNumber);
            final boolean isInvalid = scanlineNumber < 1;
            final String qualityString = getQualityString(scanline);
            final boolean isFatal = qualityString.startsWith("1");
            final boolean hasNoEarthLocation = qualityString.charAt(5) == '1';
            if(isDuplicate || isInvalid || isFatal || hasNoEarthLocation) {
                toRemove.add(scanline);
            } else {
                scanlineNumbers.add(scanlineNumber);
            }
        }
        scanlines.removeAll(toRemove);
    }

    private static String getQualityString(CompoundData scanline) throws IOException {
        final StringBuilder qualityString = new StringBuilder(Integer.toBinaryString(scanline.getInt(QUALITY_INDICATORS)));
        final int length = qualityString.length();
        if (length < 32) {
            for (int i = 0; i < 32 - length; i++) {
                qualityString.insert(0, '0');
            }
        }
        return qualityString.toString();
    }

    int getNumRecords() {
        final long recordsLength = file.length() - HEADER_RECORD.getSize();
        final int recordSize = GAC_DATA_RECORD.getSize();
        return (int) (recordsLength / recordSize);
    }

    private void setGeoCoding(Product product) throws IOException {
        TiePointGrid[] grids = createTiePointGrids(product);
        final TiePointGrid latGrid = grids[0];
        final TiePointGrid lonGrid = grids[1];
        product.addTiePointGrid(latGrid);
        product.addTiePointGrid(lonGrid);
        product.setGeoCoding(new TiePointGeoCoding(latGrid, lonGrid));
    }

    private TiePointGrid[] createTiePointGrids(Product product) throws IOException {
        final int width = product.getSceneRasterWidth() / 8;
        final int height = product.getSceneRasterHeight();

        float[] latPoints = new float[width * height];
        float[] lonPoints = new float[latPoints.length];
        int k = 0;
        for (int i = 0; i < scanlines.size(); i++) {
            validateCompound(i);
            final SequenceData scanLineData = scanlines.get(i).getSequence(EARTH_LOCATIONS);
            for (int j = 0; j < scanLineData.getElementCount(); j += 2) {
                latPoints[k] = scanLineData.getShort(j) / 128.0f;
                lonPoints[k] = scanLineData.getShort(j + 1) / 128.0f;
                k++;
            }
        }

        final TiePointGrid[] grids = new TiePointGrid[2];
        grids[0] = createTiePointGrid("latitude", width, height, 5, 0, 8, 1, latPoints);
        grids[1] = createTiePointGrid("longitude", width, height, 5, 0, 8, 1, lonPoints);
        return grids;
    }

    private void validateCompound(int i) throws IOException {
        final int validCount = scanlines.get(i).getUByte(VALID_COUNT);
        if (validCount != 51) {
            final Logger logger = Logger.getLogger("org.esa.cci.sst");
            logger.warning(MessageFormat.format("Invalid geo-location data in file {0} and scanline {1}{2}",
                                                getInput().toString(), i, '.'));
        }
    }

    private CompoundData readHeader() throws IOException {
        final CompoundType headerType = COMPOUND("file", MEMBER("header", SEQUENCE(HEADER_RECORD, 1)));
        DataFormat headerDataFormat = new DataFormat(headerType, ByteOrder.BIG_ENDIAN);
        DataContext headerContext = headerDataFormat.createContext(ioHandler);
        final SequenceData headerRecords = headerContext.getData().getSequence("header");
        return headerRecords.getCompound(0);
    }

    private static int getYear(byte firstByte) {
        String s = AvhrrReaderUtils.toBinaryString(firstByte);
        s = s.substring(0, 7);
        s = '0' + s;
        int year = Integer.parseInt(s, 2);
        year += year > 90 ? 1900 : 2000;
        return year;
    }

    private static int getDay(byte firstByte, byte secondByte) {
        final String s = String.valueOf(AvhrrReaderUtils.toBinaryString(firstByte).charAt(7));
        String s1 = AvhrrReaderUtils.toBinaryString(secondByte);
        s1 = s + s1;
        return Integer.parseInt(s1, 2);
    }
}
