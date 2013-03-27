/*
 * SST_cci Tools
 *
 * Copyright (C) 2011-2013 by Brockmann Consult GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.esa.cci.sst.common.calculator;

/**
 * Interface for coverage uncertainty calculation.
 *
 * @author Bettina Scholze
 * @author Ralf Quast
 */
public interface CoverageUncertainty {

    /**
     * Calculates the coverage uncertainty of the cell, which is a grosser target cell.
     *
     * @param cellX The x index of a cell.
     * @param cellY The y index of a cell.
     * @param n     The number of grid cells used for averaging.
     * @param a     An additional parameter.
     *
     * @return Coverage uncertainty for current cell
     */
    double calculate(int cellX, int cellY, long n, double a);
}
