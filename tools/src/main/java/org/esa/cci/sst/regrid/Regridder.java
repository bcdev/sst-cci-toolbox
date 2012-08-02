package org.esa.cci.sst.regrid;

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

    private static final Logger LOGGER = Logger.getLogger("org.esa.cci.sst.regrid.Regridder");
    private final FileStore fileStore;
    private final File outputDirectory;

    private final SpatialResolution targetResolution;
    private Map<String, ArrayGrid> sourceGrids;
    private Map<String, ArrayGrid> targetGrids;


    public Regridder(FileStore fileStore, String targetResolution, File outputDirectory) {
        this.fileStore = fileStore;
        this.targetResolution = SpatialResolution.getFromValue(targetResolution);
        this.outputDirectory = outputDirectory;
    }

    public void doIt(Date from, Date to) throws IOException {
        final List<File> files = fileStore.getFiles(from, to);

        for (File file : files) {

            NetcdfFile netcdfFileInput = NetcdfFile.open(file.getPath());
            sourceGrids = readSourceGridsTimeControlled(netcdfFileInput);
            targetGrids = initialiseTargetGrids(targetResolution, sourceGrids);

            LOGGER.info("Start regridding");
            GridAggregation gridAggregation = new GridAggregation(sourceGrids, targetGrids, new MeanCalculator());
            gridAggregation.aggregateGrids();
            LOGGER.info("Finished with regridding");

            getFileType().writeFile(netcdfFileInput, outputDirectory, targetGrids, targetResolution);
            LOGGER.info("Ready with output.");
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
            targetGridDef.setTime(sourceGrid.getGridDef().getTime());

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
