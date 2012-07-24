package org.esa.cci.sst.example;

import java.io.IOException;

class L3UL4Generator {

    public static void main(String args[]) {
        final MapDataGenerator g = new MapDataGenerator();

        try {
            g.generate();
        } catch (IOException e) {
            e.printStackTrace();
        }

        final ExampleDataGenerator exampleDataGenerator = new ExampleDataGenerator();
        exampleDataGenerator.setGeneratorExecutablePath("/usr/local/bin/ncgen");
        exampleDataGenerator.setSourceCdlFilePath("src/main/cdl/20100701000000-ESACCI-L3U_GHRSST-SSTskin-AATSR-LT-v02.0-fv01.0.cdl");
        exampleDataGenerator.setTargetCdlFilePath("20100701000000-ESACCI-L3U_GHRSST-SSTskin-AATSR-LT-v02.0-fv01.0.cdl");
        exampleDataGenerator.getProperties().setProperty("LAT", g.getLatFilePath());
        exampleDataGenerator.getProperties().setProperty("LON", g.getLonFilePath());
        exampleDataGenerator.getProperties().setProperty("LAT_BNDS", g.getLatBoundsFilePath());
        exampleDataGenerator.getProperties().setProperty("LON_BNDS", g.getLonBoundsFilePath());
        exampleDataGenerator.getProperties().setProperty("SST", g.getSstFilePath());

        try {
            exampleDataGenerator.generateDataset();
        } catch (Exception e) {
            e.printStackTrace();
        }

        exampleDataGenerator.setSourceCdlFilePath("src/main/cdl/20100701000000-ESACCI-L4_GHRSST-SSTdepth-OSTIA-LT-v02.0-fv01.0.cdl");
        exampleDataGenerator.setTargetCdlFilePath("20100701000000-ESACCI-L4_GHRSST-SSTdepth-OSTIA-LT-v02.0-fv01.0.cdl");

        try {
            exampleDataGenerator.generateDataset();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
