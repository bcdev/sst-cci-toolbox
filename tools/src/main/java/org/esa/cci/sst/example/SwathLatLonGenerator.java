package org.esa.cci.sst.example;

import java.awt.geom.Point2D;
import java.io.FileWriter;
import java.io.IOException;

class SwathLatLonGenerator {

    private double latResolution = 0.009;
    private double lonResolution = 0.009;
    private int nx = 512;
    private int ny = 28000;
    private String latFilePath = "lat.txt";
    private String lonFilePath = "lon.txt";

    void generate() throws IOException {
        final double minLon = -ny * latResolution * 0.5;
        final double minLat = -nx * lonResolution * 0.5;

        FileWriter latWriter = null;
        FileWriter lonWriter = null;
        try {
            latWriter = new FileWriter(latFilePath);
            lonWriter = new FileWriter(lonFilePath);

            final Point2D p = new Point2D.Double();
            for (int y = 0; y < ny; y++) {
                for (int x = 0; x < nx; x++) {
                    if (y > 0 || x > 0) {
                        latWriter.write(", ");
                        lonWriter.write(", ");
                    }
                    final double lon = minLon + y * latResolution;
                    final double lat = minLat + x * lonResolution;
                    rotate(lon, lat, p);

                    latWriter.write((float) p.getY() + "f");
                    lonWriter.write((float) p.getX() + "f");
                }
            }
        } finally {
            if (latWriter != null) {
                try {
                    latWriter.close();
                } catch (IOException e) {
                    // ignore
                }
                if (lonWriter != null) {
                    try {
                        lonWriter.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        }
    }

    public String getLatFilePath() {
        return latFilePath;
    }

    private void rotate(double lon, double lat, Point2D p) {
        // TODO - rotate
        p.setLocation(lon, lat);
    }

    public String getLonFilePath() {
        return lonFilePath;
    }
}
