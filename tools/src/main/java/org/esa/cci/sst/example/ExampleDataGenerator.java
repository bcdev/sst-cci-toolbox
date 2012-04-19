package org.esa.cci.sst.example;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Properties;

public class ExampleDataGenerator {

    final Properties properties = new Properties();

    public static void main(String[] args) {
        final ExampleDataGenerator g = new ExampleDataGenerator();

        if (args.length > 0) {
            FileInputStream is = null;
            try {
                is = new FileInputStream(args[0]);
                g.getProperties().load(is);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        }
        try {
            g.generateDataset();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Properties getProperties() {
        return properties;
    }

    public void setGeneratorExecutablePath(String path) {
        properties.setProperty("ncgen", path);
    }

    public void setSourceCdlFilePath(String path) {
        properties.setProperty("sourceCdlFile", path);
    }

    public void setTargetCdlFilePath(String path) {
        properties.setProperty("targetCdlFile", path);
    }

    public void generateDataset() throws Exception {
        final TemplateResolver resolver = new TemplateResolver(properties);
        final File sourceCdlFile = new File(properties.getProperty("sourceCdlFile"));
        final File targetCdlFile = new File(properties.getProperty("targetCdlFile"));
        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            reader = new BufferedReader(new FileReader(sourceCdlFile));
            writer = new BufferedWriter(new FileWriter(targetCdlFile));
            String line = reader.readLine();
            while (line != null) {
                line = resolver.resolve(line, writer);
                writer.write(line + "\n");
                line = reader.readLine();
            }
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                // ignore
            }
        }
        generateNcFile(properties.getProperty("ncgen"), targetCdlFile);
    }

    private void generateNcFile(String ncgen, File cdlFile) throws Exception {
        final String command = ncgen + " -k 4 -b " + cdlFile.getPath();
        System.out.print(command);
        final Process process = Runtime.getRuntime().exec(command);
        if (process.waitFor() != 0) {
            throw new Exception(
                    MessageFormat.format("process <code>{0}</code> terminated with exit value {1}",
                                         command, process.exitValue()));
        }
    }
}
