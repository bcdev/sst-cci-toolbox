package org.esa.cci.sst.reader;

import org.esa.beam.dataio.avhrr.AvhrrReaderPlugIn;
import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductSubsetBuilder;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.GlobalObservation;
import org.esa.cci.sst.data.Observation;
import org.esa.cci.sst.data.RelatedObservation;
import org.esa.cci.sst.data.VariableDescriptor;
import org.postgis.LinearRing;
import org.postgis.PGgeometry;
import org.postgis.Point;
import org.postgis.Polygon;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;

public class SubsceneProductIOHandler extends ProductIOHandler {

    private final BoundaryCalculator bc;

    Product getProduct() {
        return product;
    }

    public SubsceneProductIOHandler(String sensorName) {
        super(sensorName);
        this.bc = new DefaultBoundaryCalculator();
    }

    @Override
    public void init(DataFile dataFile) throws IOException {
        if (this.product != null) {
            close();
        }
        Product product = ProductIO.readProduct(new File(dataFile.getPath()), EnvisatConstants.ENVISAT_FORMAT_NAME,
                                                AvhrrReaderPlugIn.FORMAT_NAME, "NetCDF-CF");
        if (product == null) {
            throw new IOException(
                    MessageFormat.format("Unable to read observation file ''{0}''.", dataFile.getPath()));
        }
        if (bc != null) {
            try {
                product = createSubsetProductIfNecessary(product, bc);
            } catch (Exception e) {
                product.dispose();
                throw new IOException(e);
            }
        }
        workAroundBeamIssue1240(product);
        workAroundBeamIssue1241(product);

        this.product = product;
        this.dataFile = dataFile;
    }

    @Override
    public final Observation readObservation(int recordNo) throws IOException {
        final Observation observation;
        if (bc == null) {
            final GlobalObservation globalObservation = new GlobalObservation();
            globalObservation.setTime(getCenterTimeAsDate());
            observation = globalObservation;
        } else {
            final RelatedObservation relatedObservation = new RelatedObservation();
            try {
                relatedObservation.setLocation(createGeometry(bc.getGeoBoundary(product)));
            } catch (Exception e) {
                throw new IOException(e);
            }
            relatedObservation.setTime(getCenterTimeAsDate());
            observation = relatedObservation;
        }

        observation.setDatafile(dataFile);
        observation.setRecordNo(0);
        observation.setSensor(sensorName);

        return observation;
    }

    @Override
    public VariableDescriptor[] getVariableDescriptors() throws IOException {
        final ArrayList<VariableDescriptor> variableDescriptorList = new ArrayList<VariableDescriptor>();
        for (RasterDataNode node : product.getTiePointGrids()) {
            final VariableDescriptor variableDescriptor = setUpVariableDescriptor(node);
            variableDescriptor.setUnits(node.getUnit());
            variableDescriptorList.add(variableDescriptor);
        }
        for (RasterDataNode node : product.getBands()) {
            final VariableDescriptor variableDescriptor = setUpVariableDescriptor(node);
            final String units = node.getUnit();
            if (units != null && !units.isEmpty()) {
                variableDescriptor.setUnits(units);
            }
            variableDescriptorList.add(variableDescriptor);
        }
        return variableDescriptorList.toArray(new VariableDescriptor[variableDescriptorList.size()]);
    }

    private static PGgeometry createGeometry(Point[] geoBoundary) throws IOException {
        if (geoBoundary == null) {
            return null;
        }
        return new PGgeometry(new Polygon(new LinearRing[]{new LinearRing(geoBoundary)}));
    }

    private static Product createSubsetProductIfNecessary(Product product, BoundaryCalculator bc) throws IOException {
        final ProductSubsetDef def = new ProductSubsetDef();
        try {
            def.setRegion(bc.getPixelBoundary(product));
        } catch (Exception e) {
            throw new IOException(e);
        }
        final Rectangle subRegion = def.getRegion();
        if (subRegion == null) {
            return product;
        }
        if (product.getSceneRasterHeight() == subRegion.height && product.getSceneRasterWidth() == subRegion.width) {
            return product;
        }
        return ProductSubsetBuilder.createProductSubset(product, true, def, product.getName(),
                                                        product.getDescription());
    }
}
