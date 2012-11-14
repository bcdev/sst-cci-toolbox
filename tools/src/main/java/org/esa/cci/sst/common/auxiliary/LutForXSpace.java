package org.esa.cci.sst.common.auxiliary;

import org.esa.cci.sst.common.cellgrid.GridDef;
import org.esa.cci.sst.regrid.SpatialResolution;

/**
 * {@author Bettina Scholze}
 * Date: 09.11.12 15:32
 */
public class LutForXSpace {

    private static final GridDef sourceGridDef = GridDef.createGlobal(2.0);
    private SpatialResolution targetResolution;

    public LutForXSpace(SpatialResolution targetResolution) {
        this.targetResolution = targetResolution;
    }


}
