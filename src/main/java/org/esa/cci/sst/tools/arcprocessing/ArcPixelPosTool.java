/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.cci.sst.tools.arcprocessing;

import com.bc.ceres.core.Assert;
import org.esa.beam.dataio.netcdf.metadata.ProfilePartReader;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfNetCdfReaderPlugIn;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.cci.sst.reader.ArcBandPartReader;
import org.esa.cci.sst.reader.ArcPixelGeoCodingReader;
import org.esa.cci.sst.tools.Constants;
import org.esa.cci.sst.tools.MmsTool;
import org.esa.cci.sst.tools.ToolException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Responsible for creating a file containing matchup_ids and corresponding pixel positions of AVHRR GAC subscene
 * center coordinates for a single AVHRR orbit file.
 *
 * @author Thomas Storm
 */
public class ArcPixelPosTool extends MmsTool {

    private StringBuilder buffer;
    private static final String PIXELPOS_FILE_EXTENSION = ".mmm.txt";

    public static void main(String[] args) throws ToolException {
        final ArcPixelPosTool tool = new ArcPixelPosTool();
        tool.setCommandLineArgs(args);
        // do not initialise, no connection to database required
        // tool.initialize();
        try {
            tool.createPixelPositions();
            tool.createPixelPosFile();
        } catch (IOException e) {
            throw new ToolException("Tool failed.", e, ToolException.TOOL_ERROR);
        }
    }

    public ArcPixelPosTool() {
        super("arcpixelpos.sh", "0.1");
    }

    void createPixelPositions() throws ToolException, IOException {
        final Properties configuration = getConfiguration();
        final String latlonFile = configuration.getProperty(Constants.LATLONFILE_PROPERTY);
        final String locationFile = configuration.getProperty(Constants.LOCATIONFILE_PROPERTY);
        validateInput(locationFile, latlonFile);
        final Product product = readProduct(locationFile);
        final GeoCoding geoCoding = product.getGeoCoding();
        final BufferedReader reader = new BufferedReader(new FileReader(latlonFile));
        initBuffer(reader.readLine());
        final PixelPos pixelPos = new PixelPos();
        String line;
        while ((line = reader.readLine()) != null) {
            addGeoPosLine(geoCoding, line, pixelPos);
        }
    }

    private void addGeoPosLine(final GeoCoding geoCoding, final String line, final PixelPos pixelPos) {
        final GeoPos geoPos = parseGeoPos(line);
        geoCoding.getPixelPos(geoPos, pixelPos);
        buffer.append('\t');
        buffer.append(parseMatchupId(line));
        buffer.append('\t');
        buffer.append((int) pixelPos.x);
        buffer.append('\t');
        buffer.append((int) pixelPos.y);
        buffer.append('\n');
    }

    String parseMatchupId(String line) {
        final Pattern pattern = Pattern.compile("(\\s*[0-9]*)(.*)");
        final Matcher matcher = pattern.matcher(line);
        matcher.matches();
        final String matchupId = matcher.group(1);
        return matchupId.trim();
    }

    GeoPos parseGeoPos(final String line) {
        final Pattern pattern = Pattern.compile("(\\s*[0-9]*)\\s*([0-9.-]*)\\s*([0-9.-]*)");
        final Matcher matcher = pattern.matcher(line);
        matcher.matches();
        final String lon = matcher.group(2);
        final String lat = matcher.group(3);
        return new GeoPos(Float.parseFloat(lat), Float.parseFloat(lon));
    }

    Product readProduct(final String locationFile) throws ToolException {
        final Product product;
        try {
            final ArcReaderPlugIn arcReaderPlugIn = new ArcReaderPlugIn(locationFile);
            final ProductReader reader = arcReaderPlugIn.createReaderInstance();
            product = reader.readProductNodes(locationFile, null);
        } catch (IOException e) {
            throw new ToolException(MessageFormat.format("File ''{0}'' could not be read: {1}", locationFile, e.toString()), e,
                                    ToolException.TOOL_ERROR);
        }
        return product;
    }

    private void createPixelPosFile() throws IOException {
        final String latlonFilename = getConfiguration().getProperty(Constants.LATLONFILE_PROPERTY);
        final String pixelposFilename;
        if (latlonFilename.endsWith(Arc1ProcessingTool.LATLON_FILE_EXTENSION)) {
            pixelposFilename = latlonFilename.substring(0, latlonFilename.length() - Arc1ProcessingTool.LATLON_FILE_EXTENSION.length()) + PIXELPOS_FILE_EXTENSION;
        } else {
            pixelposFilename = latlonFilename + PIXELPOS_FILE_EXTENSION;
        }
        BufferedWriter writer = null;
        try {
            final File pixelposFile = new File(pixelposFilename);
            final FileWriter fileWriter = new FileWriter(pixelposFile);
            writer = new BufferedWriter(fileWriter);
            writer.write(buffer.toString());
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    private void validateInput(final String locationFile, final String latlonFile) {
        Assert.notNull(locationFile, MessageFormat.format("Property ''{0}'' must not be null.", Constants.LOCATIONFILE_PROPERTY));
        Assert.notNull(latlonFile, MessageFormat.format("Property ''{0}'' must not be null.", Constants.LOCATIONFILE_PROPERTY));
    }

    private void initBuffer(final String firstLine) {
        buffer = new StringBuilder();
        buffer.append(firstLine.trim());
        buffer.append('\n');
    }

    private static class ArcReaderPlugIn extends CfNetCdfReaderPlugIn {

        private final String locationFile;

        ArcReaderPlugIn(final String locationFile) {
            this.locationFile = locationFile;
        }

        @Override
        public ProfilePartReader createBandPartReader() {
            return new ArcBandPartReader(locationFile);
        }

        @Override
        public ProfilePartReader createGeoCodingPartReader() {
            return new ArcPixelGeoCodingReader();
        }
    }
}
