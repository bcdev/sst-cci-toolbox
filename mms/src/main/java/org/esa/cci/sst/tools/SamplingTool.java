package org.esa.cci.sst.tools;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.cci.sst.common.cellgrid.Grid;
import org.esa.cci.sst.common.cellgrid.GridDef;
import org.esa.cci.sst.common.cellgrid.YFlip;
import org.esa.cci.sst.data.Column;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.data.RelatedObservation;
import org.esa.cci.sst.reader.ExtractDefinition;
import org.esa.cci.sst.reader.Reader;
import org.esa.cci.sst.tools.overlap.RegionOverlapFilter;
import org.esa.cci.sst.util.CloudyPixelCounter;
import org.esa.cci.sst.util.NcUtils;
import org.esa.cci.sst.util.ReaderCache;
import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.SobolSequenceGenerator;
import org.esa.cci.sst.util.TimeUtil;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;

import javax.imageio.ImageIO;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SamplingTool extends BasicTool {

    private long startTime;
    private long stopTime;
    private int sampleCount;

    SamplingTool() {
        super("sampling-tool", "1.0");
    }

    public static void main(String[] args) {
        final SamplingTool tool = new SamplingTool();
        try {
            if (!tool.setCommandLineArgs(args)) {
                return;
            }
            tool.initialize();
            tool.run();
        } catch (ToolException e) {
            tool.getErrorHandler().terminate(e);
        } catch (Exception e) {
            tool.getErrorHandler().terminate(new ToolException(e.getMessage(), e, ToolException.UNKNOWN_ERROR));
        }
    }

    @Override
    public void initialize() {
        super.initialize();
        final String startTimeString = getConfiguration().getProperty("mms.sampling.startTime",
                                                                      "2004-06-01T00:00:00Z");
        final String stopTimeString = getConfiguration().getProperty("mms.sampling.stopTime",
                                                                     "2004-06-04T00:00:00Z");
        final String countString = getConfiguration().getProperty("mms.sampling.count", "10000");

        try {
            startTime = TimeUtil.parseCcsdsUtcFormat(startTimeString).getTime();
            stopTime = TimeUtil.parseCcsdsUtcFormat(stopTimeString).getTime();
            sampleCount = Integer.parseInt(countString);
        } catch (ParseException e) {
            throw new ToolException("Unable to parse sampling start and stop times.", e,
                                    ToolException.TOOL_CONFIGURATION_ERROR);
        } catch (NumberFormatException e) {
            throw new ToolException("Unable to parse sample counts.", e, ToolException.TOOL_CONFIGURATION_ERROR);
        }
    }

    private void run() {
        createSamples();
    }

    List<SamplingPoint> createSamples() {
        final SobolSequenceGenerator sequenceGenerator = new SobolSequenceGenerator(4);
        final List<SamplingPoint> sampleList = new ArrayList<>();

        for (int i = 0; i < sampleCount; i++) {
            final double[] sample = sequenceGenerator.nextVector();
            final double x = sample[0];
            final double y = sample[1];
            final double t = sample[2];
            final double random = sample[3];

            final double lon = x * 360.0 - 180.0;
            final double lat = 90.0 - y * 180.0;
            final long time = (long) (t * (stopTime - startTime)) + startTime;

            sampleList.add(new SamplingPoint(lon, lat, time, random));
        }

        return sampleList;
    }

    void removeLandSamples(List<SamplingPoint> sampleList) {
        final BufferedImage waterImage;
        try {
            final URL url = getClass().getResource("water.png");
            waterImage = ImageIO.read(url);
        } catch (IOException e) {
            throw new ToolException("Unable to read land/water mask image.", e, ToolException.TOOL_IO_ERROR);
        }

        final GridDef gridDef = GridDef.createGlobal(0.01);
        final Raster waterImageRaster = waterImage.getRaster();

        for (Iterator<SamplingPoint> iterator = sampleList.iterator(); iterator.hasNext(); ) {
            final SamplingPoint point = iterator.next();

            final int x = gridDef.getGridX(point.getLon(), true);
            final int y = gridDef.getGridY(point.getLat(), true);
            final int sample = waterImageRaster.getSample(x, y, 0);
            if (sample == 0) {
                iterator.remove();
            }
        }
    }

    void reduceClearSamples(List<SamplingPoint> sampleList) {
        final NetcdfFile file;
        try {
            file = NetcdfFile.openInMemory(getClass().getResource("AATSR_prior_run081222.nc").toURI());
        } catch (IOException | URISyntaxException e) {
            throw new ToolException("Cannot read cloud priors.", e, ToolException.TOOL_IO_ERROR);
        }

        final GridDef gridDef = GridDef.createGlobal(1.0);
        final Grid grid;
        try {
            grid = YFlip.create(NcUtils.readGrid(file, "clr_prior", gridDef));
            for (Iterator<SamplingPoint> iterator = sampleList.iterator(); iterator.hasNext(); ) {
                final SamplingPoint point = iterator.next();

                final double lon = point.getLon();
                final double lat = point.getLat();
                final int x = gridDef.getGridX(lon, true);
                final int y = gridDef.getGridY(lat, true);
                // final double f = Math.abs(lat) < 30.0 ? 1.0 : Math.cos(Math.toRadians(Math.abs(lat) - 30.0));
                final double f = 0.05 / grid.getSampleDouble(x, y);
                if (point.getRandom() > f) {
                    iterator.remove();
                }
            }
        } catch (IOException e) {
            throw new ToolException("Cannot read cloud priors.", e, ToolException.TOOL_IO_ERROR);
        } finally {
            try {
                file.close();
            } catch (IOException ignored) {
            }
        }
    }

    void findSatelliteSubscenes(List<SamplingPoint> sampleList) {
        // TODO - make parameters from variables below
        final String cloudFlagsName = "cloud_flags_nadir";
        final int cloudFlag = 2;
        final double cloudyPixelFraction = 0.0;
        final String satelliteName = "atsr_orb.3";
        final int subSceneSizeX = 7;
        final int subSceneSizeY = 7;

        final Query columnQuery = getPersistenceManager().createQuery("select c from Column c where c.name = ?1");
        final String columnName = satelliteName + "." + cloudFlagsName;
        columnQuery.setParameter(1, columnName);

        final Object columnQueryResult = columnQuery.getSingleResult();
        if (!(columnQueryResult instanceof Column)) {
            throw new ToolException("No such column '" + columnName + "'.", ToolException.UNKNOWN_ERROR);
        }
        final Column column = (Column) columnQueryResult;
        final Number fillValue = column.getFillValue();
        final CloudyPixelCounter cloudyPixelCounter = new CloudyPixelCounter(cloudFlag, fillValue);

        final Map<Integer, List<SamplingPoint>> sampleListsByDatafile = new HashMap<Integer, List<SamplingPoint>>();
        for (final SamplingPoint point : sampleList) {
            final int id = point.getReference();

            if (!sampleListsByDatafile.containsKey(id)) {
                sampleListsByDatafile.put(id, new ArrayList<SamplingPoint>());
            }
            sampleListsByDatafile.get(id).add(point);
        }

        final ReaderCache readerCache = new ReaderCache(10, getConfiguration(), getLogger());
        for (final int id : sampleListsByDatafile.keySet()) {
            final List<SamplingPoint> points = sampleListsByDatafile.get(id);

            final Query observationQuery = getPersistenceManager().createQuery(
                    "select o from Observation o where o.id = ?1");
            observationQuery.setParameter(1, id);

            final Object observationQueryResult = observationQuery.getSingleResult();
            if (observationQueryResult instanceof RelatedObservation) {
                final RelatedObservation observation = (RelatedObservation) observationQueryResult;
                final DataFile datafile = observation.getDatafile();
                try {
                    final Reader reader = readerCache.getReader(datafile, true);
                    for (final SamplingPoint point : points) {
                        final double lat = point.getLat();
                        final double lon = point.getLon();

                        final GeoCoding geoCoding = reader.getGeoCoding(0);
                        final GeoPos geoPos = new GeoPos((float) lat, (float) lon);
                        final PixelPos pixelPos = geoCoding.getPixelPos(geoPos, new PixelPos());

                        if (pixelPos.isValid()) {
                            final int pixelX = (int) Math.floor(pixelPos.getX());
                            final int pixelY = (int) Math.floor(pixelPos.getY());
                            point.setX(pixelX);
                            point.setY(pixelY);
                            point.setTime(reader.getTime(0, pixelY));

                            final ExtractDefinition extractDefinition = new ExtractDefinition() {
                                @Override
                                public double getLat() {
                                    return lat;
                                }

                                @Override
                                public double getLon() {
                                    return lon;
                                }

                                @Override
                                public int getRecordNo() {
                                    return 0;
                                }

                                @Override
                                public int[] getShape() {
                                    return new int[]{1, subSceneSizeY, subSceneSizeX};
                                }

                                @Override
                                public Date getDate() {
                                    return null;
                                }

                                @Override
                                public Number getFillValue() {
                                    return fillValue;
                                }
                            };
                            final Array array = reader.read(cloudFlagsName, extractDefinition);
                            final int cloudyPixelCount = cloudyPixelCounter.count(array);
                            if (cloudyPixelCount > (subSceneSizeX * subSceneSizeY) * cloudyPixelFraction) {
                                sampleList.remove(point);
                            }
                        } else {
                            final String message = MessageFormat.format(
                                    "Cannot find pixel at ({0}, {1}) in datafile ''{2}''.", lon, lat,
                                    datafile.getPath());
                            getLogger().fine(message);
                            sampleList.remove(point);
                        }
                    }
                } catch (IOException e) {
                    throw new ToolException(
                            MessageFormat.format("Cannot read data file ''{0}''.", datafile.getPath()), e,
                            ToolException.TOOL_IO_ERROR);
                } finally {
                    readerCache.closeReader(datafile);
                }
            }
        }
    }

    private static final String COINCIDING_OBSERVATION_QUERY =
            "select o.id"
            + " from mm_observation o"
            + " where o.sensor = ?1"
            + " and o.time >= timestamp ?2 - interval '72:00:00' and o.time < timestamp ?2 + interval '72:00:00'"
            + " and st_intersects(o.location, st_geomfromewkt(?3))"
            + " order by abs(extract(epoch from o.time) - extract(epoch from timestamp ?2))";

    public void findObservations(List<SamplingPoint> sampleList) throws PersistenceException {
        for (Iterator<SamplingPoint> iterator = sampleList.iterator(); iterator.hasNext(); ) {
            final SamplingPoint point = iterator.next();
            final double lon = point.getLon();
            final double lat = point.getLat();
            final long time = point.getTime();

            // since binding a date to a parameter failed ...
            final String queryString2 = COINCIDING_OBSERVATION_QUERY.replaceAll("\\?2",
                                                                                "'" + TimeUtil.formatCcsdsUtcFormat(
                                                                                        new Date(time)) + "'");
            final Query query = getPersistenceManager().createNativeQuery(queryString2, ReferenceObservation.class);
            query.setParameter(1, "atsr_orb.3");
            //query.setParameter("time", new Date(time), TemporalType.TIMESTAMP);
            query.setParameter(3, String.format("POINT(%.4f %.4f)", lon, lat));
            query.setMaxResults(1);

            ReferenceObservation nearestCoveringObservation = null;
            @SuppressWarnings({"unchecked"})
            final List<? extends ReferenceObservation> observations = query.getResultList();
            if (observations.isEmpty()) {
                iterator.remove();
                continue;
            }
            // select temporally nearest common observation
            nearestCoveringObservation = observations.get(0);
            point.setReference(nearestCoveringObservation.getId());
//            point.setTime(nearestCoveringObservation.getTime().getTime());
        }
    }

    private static final String SENSOR_OBSERVATION_QUERY =
            "select o.id"
            + " from mm_observation o"
            + " where o.sensor = ?1"
            + " and o.time >= timestamp ?2 and o.time < timestamp ?3"
            + " order by o.time, o.id";

    List<ReferenceObservation> findOrbits(String startTimeString, String stopTimeString) throws ParseException {
        //Date startTime = new Date(TimeUtil.parseCcsdsUtcFormat(startTimeString).getTime());
        //Date stopTime = new Date(TimeUtil.parseCcsdsUtcFormat(stopTimeString).getTime());
        final String queryString2 = SENSOR_OBSERVATION_QUERY.replaceAll("\\?2", "'" + startTimeString + "'").replaceAll(
                "\\?3", "'" + stopTimeString + "'");
        final Query query = getPersistenceManager().createNativeQuery(queryString2, ReferenceObservation.class);
        query.setParameter(1, "atsr_orb.3");
        //query.setParameter(2, startTime);
        //query.setParameter(3, stopTime);
        return query.getResultList();
    }

    public void removeOverlappingSamples(List<SamplingPoint> sampleList) {
        // @todo 2 tb/** add width and height as parameters to tool-config tb 2014-01-16
        final RegionOverlapFilter regionOverlapFilter = new RegionOverlapFilter(7, 7);

        final List<SamplingPoint> filteredList = regionOverlapFilter.filterOverlaps(sampleList);
        sampleList.clear();
        sampleList.addAll(filteredList);
    }
}
