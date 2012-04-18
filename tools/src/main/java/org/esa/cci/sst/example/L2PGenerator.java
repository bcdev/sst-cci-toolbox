package org.esa.cci.sst.example;

import java.io.IOException;

class L2PGenerator {

    public static void main(String args[]) {
        final SwathLatLonGenerator g = new SwathLatLonGenerator();

        try {
            g.generate();
        } catch (IOException e) {
            e.printStackTrace();
        }

        final ExampleDataGenerator exampleDataGenerator = new ExampleDataGenerator();
        exampleDataGenerator.setGeneratorExecutablePath("/usr/local/bin/ncgen");
        exampleDataGenerator.setSourceCdlFilePath("src/main/cdl/20100701000000-ESACCI-L2P_GHRSST-SSTskin-AATSR-DM-v02.0-fv01.0.cdl");
        exampleDataGenerator.setTargetCdlFilePath("20100701000000-ESACCI-L2P_GHRSST-SSTskin-AATSR-DM-v02.0-fv01.0.cdl");
        exampleDataGenerator.getProperties().setProperty("LAT", g.getLatFilePath());
        exampleDataGenerator.getProperties().setProperty("LON", g.getLonFilePath());

        try {
            exampleDataGenerator.generateDataset();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
