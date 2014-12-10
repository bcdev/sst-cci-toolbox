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

package org.esa.cci.sst.grid;

/**
 * Decorator for flipping an existing grid upside down.
 *
 * @author Ralf Quast
 */
public final class YFlip extends AbstractSamplePermuter {

    private final int h;

    private YFlip(Grid grid) {
        super(grid);
        h = getGridDef().getHeight();
    }

    public static Grid create(Grid grid) {
        return new YFlip(grid);
    }

    @Override
    protected final int getSourceX(int x) {
        return x;
    }

    @Override
    protected final int getSourceY(int y) {
        return h - y - 1;
    }
}
