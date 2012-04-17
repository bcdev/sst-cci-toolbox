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

    public static void main(String[] args) {
        final Properties properties = new Properties();

        if (args.length > 0) {
            FileInputStream is = null;
            try {
                is = new FileInputStream(args[0]);
                properties.load(is);
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
        final ExampleDataGenerator generator = new ExampleDataGenerator();
        try {
            generator.generateDataset(properties);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void generateDataset(Properties properties) throws Exception {
        final TemplateResolver resolver = new TemplateResolver(properties);
        final File sourceCdlFile = new File(resolver.resolveProperty("sourceCdlFile"));
        final File targetCdlFile = new File(resolver.resolveProperty("targetCdlFile"));
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
        generateNcFile(resolver.resolve("ncgen"), targetCdlFile);
    }

    private void generateNcFile(String ncgen, File cdlFile) throws Exception {
        final String command = ncgen + " -k 3 -b " + cdlFile.getPath();
        final Process process = Runtime.getRuntime().exec(command);
        if (process.waitFor() != 0) {
            throw new Exception(
                    MessageFormat.format("process <code>{0}</code> terminated with exit value {1}",
                                         command, process.exitValue()));
        }
    }
}
