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

    private static final String LOCATIONFILE_PROPERTY = "mms.arcprocessing.locationfile";
    private static final String LATLONFILE_PROPERTY = "mms.arcprocessing.latlonfile";

    private StringBuilder buffer;

    public static void main(String[] args) throws ToolException {
        final ArcPixelPosTool tool = new ArcPixelPosTool();
        tool.setCommandLineArgs(args);
        tool.initialize();
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
        final String latlonFile = configuration.getProperty(LATLONFILE_PROPERTY);
        final String locationFile = configuration.getProperty(LOCATIONFILE_PROPERTY);
        validateInput(locationFile, latlonFile);
        final Product product = readProduct(locationFile);
        final GeoCoding geoCoding = product.getGeoCoding();
        final BufferedReader reader = new BufferedReader(new FileReader(latlonFile));
        initBuffer(reader.readLine());
        String line;
        final PixelPos pixelPos = new PixelPos();
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
        buffer.append(pixelPos.x);
        buffer.append('\t');
        buffer.append(pixelPos.y);
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
            final ProductReader reader = new ArcReaderPlugIn().createReaderInstance();
            product = reader.readProductNodes(locationFile, null);
        } catch (IOException e) {
            throw new ToolException(MessageFormat.format("File ''{0}'' could not be read.", locationFile), e,
                                    ToolException.TOOL_ERROR);
        }
        return product;
    }

    private void createPixelPosFile() throws IOException {
        final String locationFile = getConfiguration().getProperty(LOCATIONFILE_PROPERTY);
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(new File(locationFile + "_pixelPos")));
            writer.write(buffer.toString());
        } finally {
            if(writer != null) {
                writer.close();
            }
        }
    }

    private void validateInput(final String locationFile, final String latlonFile) {
        Assert.notNull(locationFile, "Property '" + LOCATIONFILE_PROPERTY + "' must not be null.");
        Assert.notNull(latlonFile, "Property '" + LOCATIONFILE_PROPERTY + "' must not be null.");
    }

    private void initBuffer(final String firstLine) {
        buffer = new StringBuilder();
        buffer.append(firstLine.trim());
        buffer.append('\n');
    }

    private static class ArcReaderPlugIn extends CfNetCdfReaderPlugIn {

        @Override
        public ProfilePartReader createBandPartReader() {
            return new ArcBandPartReader();
        }

        @Override
        public ProfilePartReader createGeoCodingPartReader() {
            return new ArcPixelGeoCodingReader();
        }
    }
}
