package org.esa.beam.framework.datamodel;

import org.esa.beam.util.PixelFinder;

/**
 * Workaround for BEAM-1241
 *
 * @author Ralf Quast
 */
public class TiePointGeoCodingWithFallback extends ForwardingGeoCoding {

    private final int sceneRasterWidth;
    private final int sceneRasterHeight;
    private final PixelFinder pixelFinder;

    public TiePointGeoCodingWithFallback(TiePointGeoCoding tiePointGeoCoding, PixelFinder pixelFinder) {
        super(tiePointGeoCoding);
        sceneRasterWidth = tiePointGeoCoding.getLatGrid().getSceneRasterWidth();
        sceneRasterHeight = tiePointGeoCoding.getLatGrid().getSceneRasterHeight();
        this.pixelFinder = pixelFinder;
    }

    @Override
    public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
        super.getPixelPos(geoPos, pixelPos);
        if (geoPos.isValid()) {
            if (!pixelPos.isValid() ||
                pixelPos.x < 0 || pixelPos.y < 0 ||
                pixelPos.x > sceneRasterWidth || pixelPos.y > sceneRasterHeight) {
                final boolean pixelFound = pixelFinder.findPixel(geoPos.getLon(), geoPos.getLat(), pixelPos);
                if (!pixelFound) {
                    pixelPos.setInvalid();
                }
            }
        }
        return pixelPos;
    }
}
