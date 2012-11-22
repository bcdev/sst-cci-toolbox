package org.esa.cci.sst.common.cellgrid;

/**
 * {@author Bettina Scholze}
 * Date: 22.11.12 11:47
 */
public class YFlipperArrayGrid extends ArrayGrid {

    public YFlipperArrayGrid(ArrayGrid arrayGrid){
        super(arrayGrid);
    }

    protected int getSourceX(int x) {
        return x;
    }

    protected int getSourceY(int y) {
        return getGridDef().getHeight() - y - 1;
    }

    @Override
    public final double getSampleDouble(int x, int y) {
        return super.getSampleDouble(getSourceX(x), getSourceY(y));
    }

    @Override
    public final int getSampleInt(int x, int y) {
        return this.getSampleInt(getSourceX(x), getSourceY(y));
    }

}
