package org.esa.cci.sst.common.cellgrid;/*
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

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import ucar.ma2.Array;

/**
 * Decorator for downscaling an existing grid to a lower resolution.
 *
 * @author Ralf Quast
 */
public final class Downscaling implements Grid {

    private final Grid targetGrid;

    private Downscaling(Grid targetGrid) {
        this.targetGrid = targetGrid;
    }

    public static Grid create(Grid sourceGrid, GridDef targetGridDef) {
        final int sourceW = sourceGrid.getGridDef().getWidth();
        final int sourceH = sourceGrid.getGridDef().getHeight();
        final int targetW = targetGridDef.getWidth();
        final int targetH = targetGridDef.getHeight();
        final int scaleX = sourceW / targetW;
        final int scaleY = sourceH / targetH;

        return create(sourceGrid, scaleX, scaleY);
    }

    public static Grid create(Grid sourceGrid, int scale) {
        return create(sourceGrid, scale, scale);
    }

    public static Grid create(Grid sourceGrid, int scaleX, int scaleY) {
        final GridDef sourceGridDef = sourceGrid.getGridDef();
        final int sourceW = sourceGridDef.getWidth();
        final int sourceH = sourceGridDef.getHeight();

        if (scaleX == 0 || sourceW % scaleX != 0 || scaleY == 0 || sourceH % scaleY != 0) {
            throw new IllegalArgumentException(
                    "scaleX == 0 || sourceW % scaleX != 0 || scaleY == 0 || sourceH % scaleY != 0");
        }

        final GridDef targetGridDef = GridDef.createGlobal(sourceW / scaleX, sourceH / scaleY);
        final int targetW = targetGridDef.getWidth();
        final int targetH = targetGridDef.getHeight();
        final Array targetArray = Array.factory(Double.TYPE, new int[]{targetH, targetW});
        final ArrayGrid targetGrid = new ArrayGrid(targetGridDef, targetArray, null, 1.0, 0.0);

        for (int targetY = 0; targetY < targetH; targetY++) {
            for (int targetX = 0; targetX < targetW; targetX++) {
                double valueSum = 0.0;
                int valueCount = 0;

                for (int deltaY = 0; deltaY < scaleY; deltaY++) {
                    final int sourceY = targetY * scaleY + deltaY;

                    for (int deltaX = 0; deltaX < scaleX; deltaX++) {
                        final int sourceX = targetX * scaleX + deltaX;
                        final double sourceSample = sourceGrid.getSampleDouble(sourceX, sourceY);

                        if (!Double.isNaN(sourceSample)) {
                            valueSum += sourceSample;
                            valueCount++;
                        }
                    }
                }
                final double targetSample = valueCount > 0 ? valueSum / valueCount : Double.NaN;
                targetGrid.setSample(targetX, targetY, targetSample);
            }
        }

        return new Downscaling(targetGrid);
    }

    @Override
    public GridDef getGridDef() {
        return targetGrid.getGridDef();
    }

    @Override
    public double getSampleDouble(int x, int y) {
        return targetGrid.getSampleDouble(x, y);
    }

    @Override
    public int getSampleInt(int x, int y) {
        return targetGrid.getSampleInt(x, y);
    }

    @Override
    public boolean getSampleBoolean(int x, int y) {
        throw new RuntimeException("Method not implemented."); // no rule for downscaling boolean grids
    }
}
