package org.esa.cci.sst.reader;

public interface PixelSource {

    int getWidth();

    int getHeight();

    double getSample(int x, int y);
}
