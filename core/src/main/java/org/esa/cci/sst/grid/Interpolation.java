/*
 * Copyright (c) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.cci.sst.grid;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

/**
 * Decorator for interpolating an existing grid to higher resolution.
 *
 * @author Ralf Quast
 */
public final class Interpolation implements Grid {

    private final Grid sourceGrid;
    private final GridDef sourceGridDef;
    private final GridDef targetGridDef;

    private Interpolation(Grid sourceGrid, GridDef targetGridDef) {
        this.sourceGrid = sourceGrid;
        this.sourceGridDef = sourceGrid.getGridDef();
        this.targetGridDef = targetGridDef;
    }

    public static Grid create(Grid sourceGrid, GridDef targetGridDef) {
        return new Interpolation(sourceGrid, targetGridDef);
    }

    @Override
    public GridDef getGridDef() {
        return targetGridDef;
    }

    @Override
    public double getSampleDouble(int x, int y) {
        final int targetW = targetGridDef.getWidth();
        final int targetH = targetGridDef.getHeight();

        if (x < 0 || x >= targetW || y < 0 || y >= targetH) {
            throw new ArrayIndexOutOfBoundsException(
                    "width: " + targetW + "; height: " + targetH + "; x = " + x + "; y = " + y);
        }

        final int sourceW = sourceGridDef.getWidth();
        final int sourceH = sourceGridDef.getHeight();

        final double targetLon = targetGridDef.getCenterLon(x);
        final double targetLat = targetGridDef.getCenterLat(y);
        int sourceX = sourceGridDef.getGridX(targetLon, true);
        if (sourceX == sourceW - 1) {
            sourceX--;
        }
        int sourceY = sourceGridDef.getGridY(targetLat, true);
        if (sourceY == sourceH - 1) {
            sourceY--;
        }
        final double sourceLon = sourceGridDef.getCenterLon(sourceX);
        final double sourceLat = sourceGridDef.getCenterLat(sourceY);
        final double sourceResolutionX = sourceGrid.getGridDef().getResolutionX();
        final double sourceResolutionY = sourceGrid.getGridDef().getResolutionY();

        double wx = (targetLon - sourceLon) / sourceResolutionX;
        if (wx < 0.0) {
            wx = wx + 1.0;
            sourceX--;
        } else if (wx > 1.0) {
            wx = wx - 1.0;
            sourceX++;
        }
        final double wy = Math.min(1.0, Math.max(0.0, (sourceLat - targetLat) / sourceResolutionY));

        final int x0 = sourceGridDef.wrapX(sourceX);
        final int x1 = sourceGridDef.wrapX(sourceX + 1);
        final double v00 = sourceGrid.getSampleDouble(x0, sourceY);
        final double v10 = sourceGrid.getSampleDouble(x1, sourceY);
        final double v01 = sourceGrid.getSampleDouble(x0, sourceY + 1);
        final double v11 = sourceGrid.getSampleDouble(x1, sourceY + 1);

        final double w11 = wx * wy;
        final double w00 = w11 - wx - wy + 1.0;
        final double w10 = wx - w11;
        final double w01 = wy - w11;

        double vs = 0.0;
        double ws = 0.0;
        if (!Double.isNaN(v00)) {
            vs += w00 * v00;
            ws += w00;
        }
        if (!Double.isNaN(v10)) {
            vs += w10 * v10;
            ws += w10;
        }
        if (!Double.isNaN(v01)) {
            vs += w01 * v01;
            ws += w01;
        }
        if (!Double.isNaN(v11)) {
            vs += w11 * v11;
            ws += w11;
        }

        return ws > 0.0 ? vs / ws : Double.NaN;
    }

    @Override
    public int getSampleInt(int x, int y) {
        return (int) getSampleDouble(x, y);
    }

    @Override
    public boolean getSampleBoolean(int x, int y) {
        throw new RuntimeException("Method not implemented."); // no rule for interpolating boolean grids
    }
}
