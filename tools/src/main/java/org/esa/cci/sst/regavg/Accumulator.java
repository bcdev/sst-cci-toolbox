package org.esa.cci.sst.regavg;

/**
 * An accumulator function.
 *
 * @author Norman Fomferra
 */
public abstract class Accumulator {

    public abstract double accumulate(double[] values);

    /**
      * Nick's Eq 1.2
      */
     public static class MeanAccumulator extends Accumulator {
         @Override
         public double accumulate(double[] values) {
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
     public static class MeanSqrAccumulator extends Accumulator {
         @Override
         public double accumulate(double[] values) {
             final int n = values.length;
             double sum = 0.0;
             for (double x : values) {
                 sum += x * x;
             }
             return Math.sqrt(sum / (n*n));
         }
     }
}
