package org.esa.cci.sst.rules;

import org.esa.beam.dataio.cci.sst.NcAvhrrGacProductReader;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.reader.Reader;
import ucar.ma2.Array;
import ucar.ma2.DataType;

/**
 * @author Ralf Quast
 */
public class GacFilename extends Rule {

    @Override
    public Item apply(Item sourceColumn) throws RuleException {
        return new ColumnBuilder(sourceColumn)
                .type(DataType.CHAR)
                .build();
    }

    @Override
    public Array apply(Array sourceArray, Item sourceColumn) throws RuleException {
        final Array array = Array.factory(DataType.CHAR, new int[]{1, 80});
        final Reader observationReader = getContext().getObservationReader();
        if (observationReader == null) {
            return array;
        }
        final Product product = observationReader.getProduct();
        final MetadataElement globalAttributes = product.getMetadataRoot().getElement(
                NcAvhrrGacProductReader.ELEMENT_NAME_GLOBAL_ATTRIBUTES);
        final String gacFilename = globalAttributes.getAttributeString("gac_file");
        for (int i = 0; i < Math.min(gacFilename.length(), 80); i++) {
            array.setChar(i, gacFilename.charAt(i));
        }
        return array;
    }
}
