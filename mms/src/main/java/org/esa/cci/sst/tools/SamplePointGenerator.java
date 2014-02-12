package org.esa.cci.sst.tools;

public class SamplePointGenerator extends BasicTool{

    public SamplePointGenerator() {
        super("sampling-point-generator", ".0");
    }

    public static void main(String[] args) {
        final SamplePointGenerator tool = new SamplePointGenerator();
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
    }

    private void run() {

    }
}
