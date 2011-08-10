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
import com.bc.ceres.core.Assert;
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
import java.util.Calendar;
import java.util.GregorianCalendar;

import static com.bc.ceres.binio.TypeBuilder.*;

/**
 * TODO fill out or delete
 *
 * @author Thomas Storm
 */
public class AvhrrPodProductReader extends AbstractProductReader {

    static final CompoundType GAC_DATA_RECORD =
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

    static final CompoundType HEADER_RECORD =
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

    private File file;
    private IOHandler ioHandler;

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
        final Product product = new Product(file.getName(), getReaderPlugIn().getFormatNames()[0], 409, getSceneRasterHeight(), this);
        product.setFileLocation(file);
        product.setStartTime(getStartTime());
        product.setEndTime(getEndTime());
        setGeoCoding(product);
        return product;
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
        for(int i = 0; i < getRecords().getElementCount(); i++) {
            final SequenceData sequenceData = getRecords().getCompound(i).getSequence("earth locations");
            for (int j = 0, k = 0; j < sequenceData.getElementCount(); j += 2, k++) {
                latPoints[k] = sequenceData.getByte(j);
                lonPoints[k] = sequenceData.getByte(j + 1);
            }
        }

        final TiePointGrid[] grids = new TiePointGrid[2];
        grids[0] = createTiePointGrid("latitude", width, height, 5, 0, 8, 1, latPoints);
        grids[1] = createTiePointGrid("longitude", width, height, 5, 0, 8, 1, lonPoints);
        return grids;
    }

    ProductData.UTC getStartTime() throws IOException {
        final CompoundData firstRealRecord = getFirstRealRecord();

        final SequenceData dateSequence = firstRealRecord.getSequence(1);
        final byte firstByte = dateSequence.getByte(0);
        final byte secondByte = dateSequence.getByte(1);

        final int year = getYear(firstByte);
        final int day = getDay(firstByte, secondByte);
        final int milliseconds = firstRealRecord.getInt("milliseconds");

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
        final CompoundData lastRealRecord = getLastRealRecord();

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

    private CompoundData getFirstRealRecord() throws IOException {
        final CompoundData firstRealRecord = getRecords().getCompound(2);
        validateScanLine(firstRealRecord);
        return firstRealRecord;
    }

    private CompoundData getLastRealRecord() throws IOException {
        return getRecords().getCompound(getSceneRasterHeight() + 1);
    }

    private SequenceData getRecords() throws IOException {
        final CompoundType gacFileType = COMPOUND("file", MEMBER("gac file", SEQUENCE(GAC_DATA_RECORD, getNumRecords())));
        DataFormat dataFormat = new DataFormat(gacFileType, ByteOrder.BIG_ENDIAN);
        final DataContext context = dataFormat.createContext(ioHandler);
        return context.getData().getSequence("gac file");
    }

    private int getYear(byte firstByte) {
        String s = AvhrrReaderUtils.toBinaryString(firstByte);
        s = s.substring(0, 7);
        s = '0' + s;
        int year = Integer.parseInt(s, 2);
        year += year > 90 ? 1900 : 2000;
        return year;
    }

    private int getDay(byte firstByte, byte secondByte) {
        final String s = String.valueOf(AvhrrReaderUtils.toBinaryString(firstByte).charAt(7));
        String s1 = AvhrrReaderUtils.toBinaryString(secondByte);
        s1 = s + s1;
        return Integer.parseInt(s1, 2);
    }

    private void validateScanLine(CompoundData firstRealRecord) throws IOException {
        final short scanLineNumber = firstRealRecord.getShort("scan line number from 1 to n");
        Assert.state(scanLineNumber == 1, "scanLineNumber == 1");
    }

    int getSceneRasterHeight() throws IOException {
        final int numRecords = getNumRecords();
        final CompoundType headerType = COMPOUND("file", MEMBER("header", SEQUENCE(HEADER_RECORD, numRecords)));
        DataFormat headerDataFormat = new DataFormat(headerType, ByteOrder.BIG_ENDIAN);
        DataContext headerContext = headerDataFormat.createContext(ioHandler);
        final SequenceData headerRecords = headerContext.getData().getSequence("header");
        final CompoundData header = headerRecords.getCompound(0);
        return header.getShort("number of scans");
    }

    int getNumRecords() {
        final long fileLength = file.length();
        final int recordSize = GAC_DATA_RECORD.getSize();
        return (int) (fileLength / recordSize);
    }
}
