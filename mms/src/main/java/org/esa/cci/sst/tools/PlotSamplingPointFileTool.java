package org.esa.cci.sst.tools;

import org.apache.commons.lang.StringUtils;
import org.esa.cci.sst.util.SamplingPoint;
import org.esa.cci.sst.util.SamplingPointIO;
import org.esa.cci.sst.util.SamplingPointPlotter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class PlotSamplingPointFileTool extends BasicTool {


    private String inputFilePath;
    private boolean displayImage;

    protected PlotSamplingPointFileTool() {
        super("plot-sampling-point-file", "1.0");
    }

    public static void main(String[] args) {
        final PlotSamplingPointFileTool tool = new PlotSamplingPointFileTool();

        try {
            boolean ok = tool.setCommandLineArgs(args);
            if (!ok) {
                tool.printHelp();
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
        final Configuration config = getConfig();

        inputFilePath = config.getStringValue("json-file-path");
        displayImage = config.getBooleanValue("display-image");
    }

    private void run() throws IOException {
        if (StringUtils.isEmpty(inputFilePath)) {
            throw new ToolException("Input file not set", -1);
        }

        final File inputJsonFile = new File(inputFilePath);
        if (!inputJsonFile.isFile()) {
            throw new ToolException("Input file does not exist: " + inputJsonFile.getAbsolutePath(), -1);
        }

        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(inputJsonFile);
            final List<SamplingPoint> samplingPoints = SamplingPointIO.read(inputStream);

            final String name = inputJsonFile.getName();
            new SamplingPointPlotter()
                    .samples(samplingPoints)
                    .show(displayImage)
                    .live(false)
                    .windowTitle(name + " - " + samplingPoints.size() + " points")
                    .filePath(name.concat(".png"))
                    .plot();
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }

    }

}
