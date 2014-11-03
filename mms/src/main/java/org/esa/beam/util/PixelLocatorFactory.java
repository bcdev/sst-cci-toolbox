package org.esa.beam.util;

import org.esa.beam.framework.datamodel.PixelLocator;
import org.esa.beam.framework.datamodel.RasterDataNode;

/**
 * @author Ralf Quast
 */
public class PixelLocatorFactory {

    public static PixelLocator forSwath(RasterDataNode lonNode,
                                        RasterDataNode latNode) {
        final RasterDataNodeSampleSource lonSource = new RasterDataNodeSampleSource(lonNode);
        final RasterDataNodeSampleSource latSource = new RasterDataNodeSampleSource(latNode);

        return SwathPixelLocator.create(lonSource, latSource);
    }

    public static PixelLocator forSubscene(SampleSource lonSource,
                                           SampleSource latSource) {
        return SubscenePixelLocator.create(lonSource, latSource);
    }

}
