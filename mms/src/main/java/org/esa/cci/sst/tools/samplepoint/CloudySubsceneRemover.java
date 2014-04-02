/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.cci.sst.tools.samplepoint;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.cci.sst.common.ExtractDefinition;
import org.esa.cci.sst.common.ExtractDefinitionBuilder;
import org.esa.cci.sst.data.Column;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.orm.ColumnStorage;
import org.esa.cci.sst.orm.Storage;
import org.esa.cci.sst.reader.Reader;
import org.esa.cci.sst.reader.ReaderFactory;
import org.esa.cci.sst.tools.Configuration;
import org.esa.cci.sst.tools.ToolException;
import org.esa.cci.sst.util.PixelCounter;
import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.SensorNames;
import ucar.ma2.Array;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CloudySubsceneRemover {

    private String sensorName;
    private boolean primary;
    private int subSceneWidth;
    private int subSceneHeight;
    private String cloudFlagsVariableName;
    private int cloudFlagsMask;
    private double cloudyPixelFraction;
    private Configuration config;
    private Storage storage;
    private ColumnStorage columnStorage;
    private Logger logger;
    private int[] shape;

    public CloudySubsceneRemover() {
        primary = true;
        shape = new int[]{1, subSceneHeight, subSceneWidth};
    }

    public CloudySubsceneRemover sensorName(String sensorName) {
        this.sensorName = sensorName;
        return this;
    }

    public CloudySubsceneRemover primary(boolean primary) {
        this.primary = primary;
        return this;
    }

    public CloudySubsceneRemover subSceneWidth(int subSceneWidth) {
        this.subSceneWidth = subSceneWidth;
        return this;
    }

    public CloudySubsceneRemover subSceneHeight(int subSceneHeight) {
        this.subSceneHeight = subSceneHeight;
        return this;
    }

    public CloudySubsceneRemover cloudFlagsVariableName(String cloudFlagsVariableName) {
        this.cloudFlagsVariableName = cloudFlagsVariableName;
        return this;
    }

    public CloudySubsceneRemover cloudFlagsMask(int cloudFlagsMask) {
        this.cloudFlagsMask = cloudFlagsMask;
        return this;
    }

    public CloudySubsceneRemover cloudyPixelFraction(double cloudyPixelFraction) {
        this.cloudyPixelFraction = cloudyPixelFraction;
        return this;
    }

    public CloudySubsceneRemover config(Configuration config) {
        this.config = config;
        return this;
    }

    public CloudySubsceneRemover storage(Storage storage) {
        this.storage = storage;
        return this;
    }

    public CloudySubsceneRemover columnStorage(ColumnStorage columnStorage) {
        this.columnStorage = columnStorage;
        return this;
    }

    public CloudySubsceneRemover logger(Logger logger) {
        this.logger = logger;
        return this;
    }

    public boolean getPrimary() {
        return primary;
    }

    public static void removeSamples(List<SamplingPoint> samples, String sensorName, boolean primarySensor,
                                     int subSceneWidth, int subSceneHeight,
                                     Configuration config, Storage storage, ColumnStorage columnStorage, Logger logger, String cloudFlagsName,
                                     int pixelMask, double cloudyPixelFraction) {
        final CloudySubsceneRemover remover = new CloudySubsceneRemover()
                .sensorName(sensorName)
                .primary(primarySensor)
                .subSceneWidth(subSceneWidth)
                .subSceneHeight(subSceneHeight)
                .cloudFlagsVariableName(cloudFlagsName)
                .cloudFlagsMask(pixelMask)
                .cloudyPixelFraction(cloudyPixelFraction)
                .config(config)
                .storage(storage)
                .columnStorage(columnStorage)
                .logger(logger);

        remover.removeSamples(samples);
    }

    public void removeSamples(List<SamplingPoint> samples) {
        logInfo("Starting removing cloudy samples...");

        final Map<Integer, List<SamplingPoint>> samplesByDatafile = splitByFileId(samples, primary);

        final Number fillValue = getColumnFillValue(sensorName, cloudFlagsVariableName, columnStorage);
        final PixelCounter pixelCounter = new PixelCounter(cloudFlagsMask, fillValue);

        final ExtractDefinitionBuilder builder = new ExtractDefinitionBuilder().shape(shape).fillValue(fillValue);
        final List<SamplingPoint> clearSkySamples = new ArrayList<>(samples.size());

        for (final int id : samplesByDatafile.keySet()) {
            final List<SamplingPoint> points = samplesByDatafile.get(id);
            final Observation observation = storage.getObservation(id);
            if (observation == null) {
                continue;
            }

            final DataFile datafile = observation.getDatafile();
            try (final Reader reader = ReaderFactory.open(datafile, config)) {
                logInfo(MessageFormat.format("Starting removing cloudy samples: data file ''{0}''...", datafile.getPath()));

                final int numCols = reader.getElementCount();
                final int numRows = reader.getScanLineCount();

                final GeoCoding geoCoding = reader.getGeoCoding(0);
                for (final SamplingPoint point : points) {
                    final double lat = point.getLat();
                    final double lon = point.getLon();

                    final GeoPos geoPos = new GeoPos((float) lat, (float) lon);
                    final PixelPos pixelPos = geoCoding.getPixelPos(geoPos, new PixelPos());
                    final int pixelX = (int) Math.floor(pixelPos.getX());
                    final int pixelY = (int) Math.floor(pixelPos.getY());

                    if (pixelPos.isValid() && pixelX >= 0 && pixelY >= 0 && pixelX < numCols && pixelY < numRows) {
                        if (primary) {
                            point.setX(pixelX);
                            point.setY(pixelY);
                            point.setReferenceTime(reader.getTime(0, pixelY));
                            geoCoding.getGeoPos(pixelPos, geoPos);
                            point.setReferenceLat(geoPos.getLat());
                            point.setReferenceLon(geoPos.getLon());
                        } else {
                            point.setReference2Time(reader.getTime(0, pixelY));
                        }

                        final ExtractDefinition extractDefinition = builder.lat(lat).lon(lon).build();
                        final Array array = reader.read(cloudFlagsVariableName, extractDefinition);
                        logInfo(array.toString());
                        final int cloudyPixelCount = pixelCounter.count(array);
                        logInfo(MessageFormat.format("Found {0} cloudy pixels in sub-scene at ({1}, {2}).", cloudyPixelCount, lon, lat));
                        if (cloudyPixelCount <= (subSceneWidth * subSceneHeight) * cloudyPixelFraction) {
                            clearSkySamples.add(point);
                        }
                    } else {
                        logInfo(MessageFormat.format("Cannot find pixel at ({0}, {1}) in datafile ''{2}''.", lon, lat, datafile.getPath()));
                    }
                }
                logInfo(MessageFormat.format("Finished removing cloudy samples: data file ''{0}'' ({1} clear-sky samples)", datafile.getPath(), clearSkySamples.size()));
            } catch (IOException e) {
                throw new ToolException(
                        MessageFormat.format("Cannot read data file ''{0}''.", datafile.getPath()), e,
                        ToolException.TOOL_IO_ERROR);
            }
        }
        samples.clear();
        samples.addAll(clearSkySamples);

        logInfo(MessageFormat.format("Finished removing cloudy samples: {0} clear-sky samples found in total", samples.size()));
    }

    private void logInfo(String message) {
        if (logger != null && logger.isLoggable(Level.INFO)) {
            logger.info(message);
        }
    }

    // package access for testing only tb 2014-03-31
    static Map<Integer, List<SamplingPoint>> splitByFileId(List<SamplingPoint> samples, boolean isPrimary) {
        final Map<Integer, List<SamplingPoint>> samplesByDatafile = new HashMap<>();
        for (final SamplingPoint point : samples) {
            final int id = isPrimary ? point.getReference() : point.getReference2();
            if (!samplesByDatafile.containsKey(id)) {
                samplesByDatafile.put(id, new ArrayList<SamplingPoint>());
            }
            samplesByDatafile.get(id).add(point);
        }
        return samplesByDatafile;
    }

    // package access for testing only tb 2014-03-31
    static Number getColumnFillValue(String sensorName, String cloudFlagsVariableName, ColumnStorage columnStorage) {
        final String columnName = SensorNames.ensureOrbitName(sensorName) + "." + cloudFlagsVariableName;
        final Column column = columnStorage.getColumn(columnName);
        if (column == null) {
            throw new ToolException(MessageFormat.format("Unable to find column ''{0}''.", columnName), ToolException.TOOL_ERROR);
        }
        return column.getFillValue();
    }

    // package access for testing only tb 2014-04-02
    static boolean isInsituCase(Configuration configuration) {
        final String generatorName = configuration.getStringValue(Configuration.KEY_MMS_SAMPLING_GENERATOR, null);
        return "insitu".equalsIgnoreCase(generatorName);
    }
}
