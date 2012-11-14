package org.esa.cci.sst.common.auxiliary;

import org.esa.cci.sst.common.cellgrid.ArrayGrid;
import org.esa.cci.sst.common.cellgrid.GridDef;

import java.io.File;

/**
 * {@author Bettina Scholze}
 * Date: 09.11.12 15:30
 */
public class LutForStdDeviation {

    private ArrayGrid stdDeviationGrid; //0.5 °

    private static final GridDef INPUT_GRID_DEF = GridDef.createGlobal(0.05);

    private LutForStdDeviation(File file) {
    }
}
