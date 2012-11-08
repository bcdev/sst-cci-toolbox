package org.esa.cci.sst.example;

import java.io.FileWriter;
import java.io.IOException;

class MapDataGenerator {

    private double latResolution = 0.05;
    private double lonResolution = 0.05;
    private int latCount = 3600;
    private int lonCount = 7200;
    private String latFilePath = "lat.txt";
    private String lonFilePath = "lon.txt";
    private String sstFilePath = "sst.txt";
    private String latBoundsFilePath = "latBounds.txt";
    private String lonBoundsFilePath = "lonBounds.txt";

    public static void main(String args[]) {
        final MapDataGenerator g = new MapDataGenerator();

        try {
            g.generate();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    MapDataGenerator latResolution(double latResolution) {
        this.latResolution = latResolution;
        return this;
    }

    MapDataGenerator lonResolution(double lonResolution) {
        this.lonResolution = lonResolution;
        return this;
    }

    MapDataGenerator latCount(int latCount) {
        this.latCount = latCount;
        return this;
    }

    MapDataGenerator lonCount(int lonCount) {
        this.lonCount = lonCount;
        return this;
    }

    MapDataGenerator latFilePath(String path) {
        this.latFilePath = path;
        return this;
    }

    MapDataGenerator lonFilePath(String path) {
        this.lonFilePath = path;
        return this;
    }

    MapDataGenerator latBoundsFilePath(String path) {
        this.latBoundsFilePath = path;
        return this;
    }

    MapDataGenerator lonBoundsFilePath(String path) {
        this.lonBoundsFilePath = path;
        return this;
    }

    void generate() throws IOException {
        FileWriter latWriter = null;
        try {
            latWriter = new FileWriter(latFilePath);

            for (int i = 0; i < latCount; i++) {
                if (i > 0) {
                    latWriter.write(", ");
                }
                final float lat = (float) (90.0 - (i + 0.5) * latResolution);
                latWriter.write(lat + "f");
            }
        } finally {
            if (latWriter != null) {
                try {
                    latWriter.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        FileWriter lonWriter = null;
        try {
            lonWriter = new FileWriter(lonFilePath);
            for (int i = 0; i < lonCount; i++) {
                if (i > 0) {
                    lonWriter.write(", ");
                }
                final float lon = (float) (-180.0 + (i + 0.5) * lonResolution);
                lonWriter.write(lon + "f");
            }
        } finally {
            if (lonWriter != null) {
                try {
                    lonWriter.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        FileWriter latBoundsWriter = null;
        try {
            latBoundsWriter = new FileWriter(latBoundsFilePath);

            for (int i = 0; i < latCount; i++) {
                if (i > 0) {
                    latBoundsWriter.write(", ");
                }
                final float lat1 = (float) (90.0 - i * latResolution);
                final float lat2 = (float) (90.0 - (i + 1) * latResolution);
                latBoundsWriter.write(lat1 + "f, " + lat2 + "f");
            }
        } finally {
            if (latBoundsWriter != null) {
                try {
                    latBoundsWriter.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        FileWriter lonBoundsWriter = null;
        try {
            lonBoundsWriter = new FileWriter(lonBoundsFilePath);
            for (int i = 0; i < lonCount; i++) {
                if (i > 0) {
                    lonBoundsWriter.write(", ");
                }
                final float lon1 = (float) (-180.0 + i * lonResolution);
                final float lon2 = (float) (-180.0 + (i + 1) * lonResolution);
                lonBoundsWriter.write(lon1 + "f, " + lon2 + "f");
            }
        } finally {
            if (lonBoundsWriter != null) {
                try {
                    lonBoundsWriter.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        FileWriter sstWriter = null;
        try {
            sstWriter = new FileWriter(sstFilePath);
            for (int i = 0; i < lonCount * latCount; i++) {
                if (i > 0) {
                    sstWriter.write(", ");
                }
                final short sst;
                if (isEvenLine(i)) {
                    sst = (short) ((293.15f - 273.15f) / 0.01f);
                } else {
                    sst = -32768;
                }
                sstWriter.write(sst + "s");
            }
        } finally {
            if (sstWriter != null) {
                try {
                    sstWriter.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    boolean isEvenLine(int i) {
        return (i / lonCount) % 2 == 0;
    }

    String getLatFilePath() {
        return latFilePath;
    }

    String getLonFilePath() {
        return lonFilePath;
    }

    String getLatBoundsFilePath() {
        return latBoundsFilePath;
    }

    String getLonBoundsFilePath() {
        return lonBoundsFilePath;
    }

    public String getSstFilePath() {
        return sstFilePath;
    }
}
