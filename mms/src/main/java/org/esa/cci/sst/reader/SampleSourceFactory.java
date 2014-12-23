package org.esa.cci.sst.reader;

import org.esa.beam.util.SampleSource;
import ucar.ma2.Array;
import ucar.nc2.Variable;

/**
 * @author Ralf Quast
 */
public class SampleSourceFactory {

    public static SampleSource forVariable(Variable variable, Array slice) {
        return new VariableSampleSource(variable, slice);
    }

}
