package org.esa.cci.sst.regrid;

import org.esa.cci.sst.tool.Tool;
import org.esa.cci.sst.util.ArrayGrid;
import org.esa.cci.sst.util.GridDef;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author Bettina Scholze
 *         Date: 23.07.12 13:40
 */
public class Regridder {

    private static final Logger LOGGER = Tool.LOGGER;
    private final FileStore fileStore;

    private final SpatialResolution targetResolution;
    private Map<String, ArrayGrid> sourceGrids;
    private Map<String, ArrayGrid> targetGrids;


    public Regridder(FileStore fileStore, String targetResolution) {
        this.fileStore = fileStore;
        this.targetResolution = SpatialResolution.getFromValue(targetResolution);
    }

    public void doIt(Date from, Date to) throws IOException {
        //LOOP
        //1) read 1st product
        //2) regrid it (as x ArrayGrids)
        //2A) Do other stuff with it (as x ArrayGrids)
        //3) write 1st product

        final List<File> files = fileStore.getFiles(from, to);

        for (File file : files) {
            NetcdfFile netcdfFile = NetcdfFile.open(file.getPath());
            sourceGrids = readSourceGridsTimeControlled(netcdfFile);
            targetGrids = initialiseTargetGrids(targetResolution, sourceGrids);

//            AggregationCell5Context aggregationCell5Context = createAggregationCell5Context(netcdfFile);
//            aggregateSources(aggregationCell5Context, combinedRegionMask, cell5Grid);

            //prepare a Context object (RegridderContext, could be expanded by auxilary data etc.)
            RegridContext regridContext = new RegridContext(); //todo
            GridAggregation gridAggregation = new GridAggregation(sourceGrids, targetGrids, new MeanCalculator());
            gridAggregation.aggregateGrids();

            //write output netcdf file (L3UFileType)
            //todo
        }
    }

    private Map<String, ArrayGrid> readSourceGridsTimeControlled(NetcdfFile netcdfFile) throws IOException {
        long t0 = System.currentTimeMillis();
        LOGGER.info("Reading source grid(s)...");
        final Map<String, ArrayGrid> gridMap = getFileType().readSourceGrids(netcdfFile);
        LOGGER.info(String.format("Reading source grid(s) took %d ms", (System.currentTimeMillis() - t0)));
        return gridMap;
    }

    private FileType getFileType() {
        return fileStore.getProductType().getFileType();
    }

    Map<String, ArrayGrid> initialiseTargetGrids(SpatialResolution targetResolution, Map<String, ArrayGrid> sourceGrids) throws IOException {
        GridDef targetGridDef = targetResolution.getAssociatedGridDef();
        HashMap<String, ArrayGrid> targetGrids = new HashMap<String, ArrayGrid>();

        for (ArrayGrid sourceGrid : sourceGrids.values()) {
            int[] sourceShape = sourceGrid.getArray().getShape();
            int[] targetShape = SpatialResolution.convertShape(targetResolution, sourceShape, sourceGrid.getGridDef());
            Class dataType = sourceGrid.getArray().getElementType();

            Array array = Array.factory(dataType, targetShape);
            ArrayGrid targetGrid = new ArrayGrid(targetGridDef, array, sourceGrid.getFillValue(), sourceGrid.getScaling(), sourceGrid.getOffset());
            targetGrid.setVariable(sourceGrid.getVariable());
            targetGrids.put(sourceGrid.getVariable(), targetGrid);
        }

        return targetGrids;
    }
}
