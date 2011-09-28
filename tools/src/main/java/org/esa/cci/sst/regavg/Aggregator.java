package org.esa.cci.sst.regavg;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * todo - add api doc
 *
 * @author Norman Fomferra
 */
public class Aggregator {
    List<VarAcc> varAccList = new ArrayList<VarAcc>();

    public void add(String name, Accumulator accumulator) {
        varAccList.add(new VarAcc(name, accumulator));
    }

    public void aggregate(File[] files, Processor processor) throws IOException {
        NetcdfFile[] netcdfFiles = new NetcdfFile[files.length];

        for (int i = 0; i < files.length; i++) {
            netcdfFiles[i] = NetcdfFile.open(files[i].getPath());
        }

        int w = 7200; // todo
        int h = 3600; // todo
        int numLines = 36;
        for (int y = 0; y < h; y += numLines) {
            int[] offset = new int[]{y, 0};
            int[] shape = new int[]{numLines, w};
            for (VarAcc varAcc : varAccList) {
                Array[] arrays = new Array[netcdfFiles.length];
                for (int i = 0; i < netcdfFiles.length; i++) {
                    NetcdfFile netcdfFile = netcdfFiles[i];
                    Variable variable = netcdfFile.findTopVariable(varAcc.name);
                    try {
                        arrays[i] = variable.read(offset, shape);
                    } catch (InvalidRangeException e) {
                        throw new IllegalArgumentException(e);
                    }
                }
                Array result = accu(varAcc, w, numLines, arrays);
                processor.process(varAcc.name, 0, y, w, numLines, result);
            }
        }
    }

    public Array accu(VarAcc varAcc,  int w, int h, Array[] arrays) {

        double[] input = new double[arrays.length];
        Array array = Array.factory(Float.TYPE, new int[]{h, w});
        for (int j = 0; j < w * h; j++) {
            for (int i = 0; i < arrays.length; i++) {
                input[i] = arrays[i].getFloat(j);
            }
            array.setFloat(j, (float) varAcc.accumulator.accu(input));
        }

        return array;
    }


    private static class VarAcc {
        String name;
        Accumulator accumulator;

        private VarAcc(String name, Accumulator accumulator) {
            this.name = name;
            this.accumulator = accumulator;
        }
    }

    interface Accumulator {
        double accu(double[] values);
    }

    interface Processor {
        void process(String name, int x, int y, int w, int h, Array result);
    }

    /**
     * Nick's Eq 1.2
     */
    public static class MeanAcccumulator implements Accumulator {
        @Override
        public double accu(double[] values) {
            final int n = values.length;
            double sum = 0.0;
            for (double x : values) {
                sum += x;
            }
            return sum / n;
        }
    }

    /**
     * Nick's Eq 1.1
     */
    public static class MeanSqrAcccumulator implements Accumulator {
        @Override
        public double accu(double[] values) {
            final int n = values.length;
            double sum = 0.0;
            for (double x : values) {
                sum += x * x;
            }
            return Math.sqrt(sum / (n*n));
        }
    }
}
