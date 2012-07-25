package org.esa.cci.sst.regrid;

import org.esa.cci.sst.tool.Tool;
import org.esa.cci.sst.util.Grid;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Bettina Scholze
 *         Date: 23.07.12 13:40
 */
public class Regridder {

//    public static final GridDef GRID_DEF_GLOBAL_5 = GridDef.createGlobal(5.0);
//    public static final GridDef GRID_DEF_GLOBAL_90 = GridDef.createGlobal(90.0);

    private static final Logger LOGGER = Tool.LOGGER;
    private final FileStore fileStore;


    public Regridder(FileStore fileStore) {
        this.fileStore = fileStore;
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
            //here createAggregationCellContext
            //--> seeehr zeit-aggregation-mäßig
            RegridContext regridContext = new RegridContext(); //todo
//            AggregationCell5Context aggregationCell5Context = createAggregationCell5Context(netcdfFile);
//            aggregateSources(aggregationCell5Context, combinedRegionMask, cell5Grid);

            final Grid[] sourceGrids = readSourceGrids(netcdfFile);

        }
    }

    private Grid[] readSourceGrids(NetcdfFile netcdfFile) throws IOException {
        long t0 = System.currentTimeMillis();
        LOGGER.fine("Reading source grid(s)...");
        Grid[] grids = fileStore.getProductType().getFileType().readSourceGrids(netcdfFile);
        LOGGER.fine(String.format("Reading source grid(s) took %d ms", (System.currentTimeMillis() - t0)));
        return grids;
    }
}
