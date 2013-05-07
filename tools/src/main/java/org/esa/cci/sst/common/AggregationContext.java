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

package org.esa.cci.sst.common;

import org.esa.cci.sst.common.calculator.CoverageUncertaintyProvider;
import org.esa.cci.sst.common.calculator.SynopticUncertaintyProvider;
import org.esa.cci.sst.common.cellgrid.Grid;
import org.esa.cci.sst.common.cellgrid.GridDef;
import org.esa.cci.sst.common.cellgrid.RegionMask;

import java.util.List;

/**
 * Provides the input grids for a {@link org.esa.cci.sst.common.cell.SpatialAggregationCell}.
 *
 * @author Norman Fomferra
 * @author Ralf Quast
 */
public final class AggregationContext {

    private Grid sstGrid;
    private Grid climatologySstGrid;
    private Grid randomUncertaintyGrid;
    private Grid standardDeviationGrid;
    private Grid largeScaleUncertaintyGrid;
    private Grid adjustmentUncertaintyGrid;
    private Grid synopticUncertaintyGrid;
    private Grid qualityGrid;

    private Grid seaCoverageGrid;
    private GridDef targetGridDef;

    private CoverageUncertaintyProvider coverageUncertaintyProvider;
    private SynopticUncertaintyProvider synopticUncertaintyProvider;

    private double minCoverage;

    private List<RegionMask> targetRegionMaskList;
    private RegionMask targetRegionMask;
    private Grid seaIceFractionGrid;

    public AggregationContext() {
    }

    public GridDef getSourceGridDef() {
        if (sstGrid != null) {
            return sstGrid.getGridDef();
        }
        throw new IllegalStateException("No SST grid has been set.");
    }

    public GridDef getTargetGridDef() {
        return targetGridDef;
    }

    public void setTargetGridDef(GridDef targetGridDef) {
        this.targetGridDef = targetGridDef;
    }

    public Grid getClimatologySstGrid() {
        return climatologySstGrid;
    }

    public void setClimatologySstGrid(Grid climatologySstGrid) {
        this.climatologySstGrid = climatologySstGrid;
    }

    public Grid getSeaCoverageGrid() {
        return seaCoverageGrid;
    }

    public void setSeaCoverageGrid(Grid seaCoverageGrid) {
        this.seaCoverageGrid = seaCoverageGrid;
    }

    public Grid getStandardDeviationGrid() {
        return standardDeviationGrid;
    }

    public void setStandardDeviationGrid(Grid standardDeviationGrid) {
        this.standardDeviationGrid = standardDeviationGrid;
    }

    public void setSynopticUncertaintyProvider(SynopticUncertaintyProvider synopticUncertaintyProvider) {
        this.synopticUncertaintyProvider = synopticUncertaintyProvider;
    }

    public SynopticUncertaintyProvider getSynopticUncertaintyProvider() {
        return synopticUncertaintyProvider;
    }

    public void setCoverageUncertaintyProvider(CoverageUncertaintyProvider coverageUncertaintyProvider) {
        this.coverageUncertaintyProvider = coverageUncertaintyProvider;
    }

    public CoverageUncertaintyProvider getCoverageUncertaintyProvider() {
        return coverageUncertaintyProvider;
    }

    public double getMinCoverage() {
        return minCoverage;
    }

    public void setMinCoverage(double minCoverage) {
        this.minCoverage = minCoverage;
    }

    public void setTargetRegionMaskList(RegionMaskList targetRegionMaskList) {
        this.targetRegionMaskList = targetRegionMaskList;
        this.targetRegionMask = RegionMask.combine(targetRegionMaskList);
    }

    public RegionMask getTargetRegionMask() {
        return targetRegionMask;
    }

    public Grid getSstGrid() {
        return sstGrid;
    }

    public void setSstGrid(Grid sstGrid) {
        this.sstGrid = sstGrid;
    }

    public Grid getRandomUncertaintyGrid() {
        return randomUncertaintyGrid;
    }

    public void setRandomUncertaintyGrid(Grid randomUncertaintyGrid) {
        this.randomUncertaintyGrid = randomUncertaintyGrid;
    }

    public Grid getLargeScaleUncertaintyGrid() {
        return largeScaleUncertaintyGrid;
    }

    public void setLargeScaleUncertaintyGrid(Grid largeScaleUncertaintyGrid) {
        this.largeScaleUncertaintyGrid = largeScaleUncertaintyGrid;
    }

    public Grid getAdjustmentUncertaintyGrid() {
        return adjustmentUncertaintyGrid;
    }

    public void setAdjustmentUncertaintyGrid(Grid adjustmentUncertaintyGrid) {
        this.adjustmentUncertaintyGrid = adjustmentUncertaintyGrid;
    }

    public Grid getSynopticUncertaintyGrid() {
        return synopticUncertaintyGrid;
    }

    public void setSynopticUncertaintyGrid(Grid synopticUncertaintyGrid) {
        this.synopticUncertaintyGrid = synopticUncertaintyGrid;
    }

    public Grid getQualityGrid() {
        return qualityGrid;
    }

    public void setQualityGrid(Grid qualityGrid) {
        this.qualityGrid = qualityGrid;
    }

    public List<RegionMask> getTargetRegionMaskList() {
        return targetRegionMaskList;
    }

    public void setSeaIceFractionGrid(Grid seaIceFractionGrid) {
        this.seaIceFractionGrid = seaIceFractionGrid;
    }

    public Grid getSeaIceFractionGrid() {
        return seaIceFractionGrid;
    }
}
