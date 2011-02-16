package org.esa.cci.sst.reader;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.Variable;
import org.esa.cci.sst.util.PgUtil;
import org.postgis.LinearRing;
import org.postgis.PGgeometry;
import org.postgis.Point;
import org.postgis.Polygon;
import ucar.ma2.InvalidRangeException;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ProductObservationReader implements ObservationReader {

    private final String sensorName;

    private DataFile dataFile;
    private Product product;

    public ProductObservationReader(String sensorName) {
        this.sensorName = sensorName;
    }

    @Override
    public final void init(File observationFile, DataFile dataFileEntry) throws IOException {
        final Product product = ProductIO.readProduct(observationFile);
        if (product == null) {
            throw new IOException(
                    MessageFormat.format("Unable to read observation file ''{0}''.", observationFile.getPath()));
        }
        this.product = product;
        this.dataFile = dataFileEntry;
    }

    @Override
    public final void close() throws IOException {
        if (product != null) {
            product.dispose();
        }
    }

    @Override
    public final int getNumRecords() {
        return 1;
    }

    @Override
    public final long getTime(int recordNo) throws IOException, InvalidRangeException {
        if (product == null) {
            throw new IOException("Unable to get start time.");
        }
        return getCenterTimeAsDate().getTime();
    }

    @Override
    public final Observation readObservation(int recordNo) throws IOException, InvalidRangeException {
        if (product == null) {
            return null;
        }

        final Observation observation = new Observation();
        observation.setClearSky(true);
        observation.setDatafile(dataFile);
        observation.setLocation(new PGgeometry(new Polygon(new LinearRing[]{new LinearRing(getGeoBoundary())})));
        observation.setRecordNo(0);
        observation.setSensor(sensorName);
        observation.setTime(getCenterTimeAsDate());

        return observation;
    }

    @Override
    public final Observation readRefObs(int recordNo) throws IOException, InvalidRangeException {
        return null;
    }

    @Override
    public Variable[] getVariables() throws IOException {
        final ArrayList<Variable> variableList = new ArrayList<Variable>();
        final Variable observationTime = new Variable();
        observationTime.setName(String.format("%s.%s", sensorName, "observationTime"));
        observationTime.setDataSchema(dataFile.getDataSchema());
        variableList.add(observationTime);
        for (RasterDataNode node : product.getTiePointGrids()) {
            final Variable variable = new Variable();
            variable.setName(String.format("%s.%s", sensorName, node.getName()));
            variable.setDataSchema(dataFile.getDataSchema());
            variableList.add(variable);
            // todo: add dimension and other attributes
        }
        for (RasterDataNode node : product.getBands()) {
            final Variable variable = new Variable();
            variable.setName(String.format("%s.%s", sensorName, node.getName()));
            variable.setDataSchema(dataFile.getDataSchema());
            variableList.add(variable);
        }
        return variableList.toArray(new Variable[variableList.size()]);
    }

    private Date getCenterTimeAsDate() throws IOException {
        final ProductData.UTC startTime = product.getStartTime();
        if (startTime == null) {
            throw new IOException("Unable to get start time for product '" + product.getName() + "'.");
        }
        final ProductData.UTC endTime = product.getEndTime();
        if (endTime == null) {
            throw new IOException("Unable to get end time for product '" + product.getName() + "'.");
        }
        final ProductData.UTC centerTime = new ProductData.UTC(0.5 * (startTime.getMJD() + endTime.getMJD()));
        return centerTime.getAsDate();
    }

    private Point[] getGeoBoundary() throws IOException {
        final GeoCoding geoCoding = product.getGeoCoding();
        if (geoCoding == null) {
            throw new IOException("Unable to get geo-coding for product '" + product.getName() + "'.");
        }

        final int w = product.getSceneRasterWidth();
        final int h = product.getSceneRasterHeight();
        int minX = getMinX();
        int maxX = getMaxX();
        int minY = 0;
        int maxY = h - 1;
        if (minX == -1 || maxX == -1) {
            // no pair of opposing geo-coordinates at the horizontal boundaries is valid, try vertical boundaries
            minX = 0;
            maxX = w - 1;
            minY = getMinY();
            maxY = getMaxY();
            if (minY == -1 || maxY == -1) {
                // no pair of opposing geo-coordinates at the vertical boundaries is valid
                throw new IOException("Unable to get geo-boundary for product '" + product.getName() + "'.");
            }
        }
        final int stepX = Math.max(1, w / 25);
        final int stepY = Math.max(1, h / 25);

        final PixelPos p = new PixelPos();
        final GeoPos g = new GeoPos();
        final List<Point> geoBoundary = new ArrayList<Point>();
        for (int i = minY; i < maxY; i += stepY) {
            p.setLocation(minX + 0.5, i + 0.5);
            geoCoding.getGeoPos(p, g);
            geoBoundary.add(new Point(g.getLon(), g.getLat()));
        }
        for (int i = minX; i < maxX; i += stepX) {
            p.setLocation(i + 0.5, maxY + 0.5);
            geoCoding.getGeoPos(p, g);
            geoBoundary.add(new Point(g.getLon(), g.getLat()));
        }
        for (int i = maxY; i > minY; i -= stepY) {
            p.setLocation(maxX + 0.5, i + 0.5);
            geoCoding.getGeoPos(p, g);
            geoBoundary.add(new Point(g.getLon(), g.getLat()));
        }
        for (int i = maxX; i > minX; i -= stepX) {
            p.setLocation(i + 0.5, minY + 0.5);
            geoCoding.getGeoPos(p, g);
            geoBoundary.add(new Point(g.getLon(), g.getLat()));
        }
        geoBoundary.add(geoBoundary.get(0));
        if (PgUtil.isClockwise(geoBoundary)) {
            Collections.reverse(geoBoundary);
        }

        return geoBoundary.toArray(new Point[geoBoundary.size()]);
    }

    private int getMinX() {
        final GeoCoding geoCoding = product.getGeoCoding();

        final int w = product.getSceneRasterWidth();
        final int h = product.getSceneRasterHeight();

        final PixelPos p = new PixelPos();
        final GeoPos g = new GeoPos();
        for (int i = 0; i < w; i++) {
            p.setLocation(i + 0.5, 0.5);
            geoCoding.getGeoPos(p, g);
            if (g.isValid()) {
                p.setLocation(i + 0.5, h - 0.5);
                geoCoding.getGeoPos(p, g);
                if (g.isValid()) {
                    return i;
                }
            }
        }
        return -1;
    }

    private int getMaxX() {
        final GeoCoding geoCoding = product.getGeoCoding();
        final int w = product.getSceneRasterWidth();
        final int h = product.getSceneRasterHeight();

        final PixelPos p = new PixelPos();
        final GeoPos g = new GeoPos();
        for (int i = w; i-- > 0;) {
            p.setLocation(i + 0.5, 0.5);
            geoCoding.getGeoPos(p, g);
            if (g.isValid()) {
                p.setLocation(i + 0.5, h - 0.5);
                geoCoding.getGeoPos(p, g);
                if (g.isValid()) {
                    return i;
                }
            }
        }
        return -1;
    }

    private int getMinY() {
        final GeoCoding geoCoding = product.getGeoCoding();

        final int w = product.getSceneRasterWidth();
        final int h = product.getSceneRasterHeight();

        final PixelPos p = new PixelPos();
        final GeoPos g = new GeoPos();
        for (int i = 0; i < h; i++) {
            p.setLocation(0.5, i + 0.5);
            geoCoding.getGeoPos(p, g);
            if (g.isValid()) {
                p.setLocation(w - 0.5, i + 0.5);
                geoCoding.getGeoPos(p, g);
                if (g.isValid()) {
                    return i;
                }
            }
        }
        return -1;
    }

    private int getMaxY() {
        final GeoCoding geoCoding = product.getGeoCoding();
        final int w = product.getSceneRasterWidth();
        final int h = product.getSceneRasterHeight();

        final PixelPos p = new PixelPos();
        final GeoPos g = new GeoPos();
        for (int i = h; i-- > 0;) {
            p.setLocation(0.5, i + 0.5);
            geoCoding.getGeoPos(p, g);
            if (g.isValid()) {
                p.setLocation(w - 0.5, i + 0.5);
                geoCoding.getGeoPos(p, g);
                if (g.isValid()) {
                    return i;
                }
            }
        }
        return -1;
    }
}
