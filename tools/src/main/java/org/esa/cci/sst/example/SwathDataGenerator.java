package org.esa.cci.sst.example;

import java.awt.geom.Point2D;
import java.io.FileWriter;
import java.io.IOException;

class SwathDataGenerator {

    private double latResolution = 0.009;
    private double lonResolution = 0.009;
    private int nx = 512;
    private int ny = 28000;
    private String latFilePath = "lat.txt";
    private String lonFilePath = "lon.txt";
    private String sstFilePath = "sst.txt";

    private Rotation rotation = new Rotation(0.0, 0.0, 260.0);

    void generate() throws IOException {
        final double minLon = -ny * latResolution * 0.5;
        final double minLat = -nx * lonResolution * 0.5;

        FileWriter latWriter = null;
        FileWriter lonWriter = null;
        FileWriter sstWriter = null;
        try {
            latWriter = new FileWriter(latFilePath);
            lonWriter = new FileWriter(lonFilePath);
            sstWriter = new FileWriter(sstFilePath);

            final Point2D p = new Point2D.Double();
            for (int y = 0; y < ny; y++) {
                for (int x = 0; x < nx; x++) {
                    if (y > 0 || x > 0) {
                        latWriter.write(", ");
                        lonWriter.write(", ");
                        sstWriter.write(", ");
                    }
                    final double lon = minLon + y * latResolution;
                    final double lat = minLat + x * lonResolution;
                    p.setLocation(lon, lat);
                    rotation.transform(p);

                    latWriter.write((float) p.getY() + "f");
                    lonWriter.write((float) p.getX() + "f");

                    final short sst;
                    if (y % 2 == 0) {
                        sst = (short) ((293.15f - 273.15f) / 0.01f);
                    } else {
                        sst = -32768;
                    }
                    sstWriter.write(sst + "s");
                }
            }
        } finally {
            if (latWriter != null) {
                try {
                    latWriter.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            if (lonWriter != null) {
                try {
                    lonWriter.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            if (sstWriter != null) {
                try {
                    sstWriter.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    public String getLatFilePath() {
        return latFilePath;
    }

    public String getLonFilePath() {
        return lonFilePath;
    }

    public String getSstFilePath() {
        return sstFilePath;
    }
}
