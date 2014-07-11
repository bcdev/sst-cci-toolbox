package org.esa.beam.util;

import javax.media.jai.PlanarImage;
import java.awt.image.Raster;

/**
* @author Ralf Quast
*/
final class MaskSampleSource implements SampleSource {

    private final Raster maskData;
    private final int width;
    private final int height;

    public MaskSampleSource(PlanarImage maskImage) {
        maskData = maskImage.getData();
        width = maskImage.getWidth();
        height = maskImage.getHeight();
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public double getSample(int x, int y) {
        return maskData.getSample(x, y, 0);
    }

    @Override
    public boolean isFillValue(int x, int y) {
        return false;
    }
}
