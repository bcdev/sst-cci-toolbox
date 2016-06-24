package org.esa.beam.dataio.metop;


import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.avhrr.AvhrrFile;
import org.esa.beam.dataio.avhrr.BandReader;
import org.esa.beam.framework.datamodel.ProductData;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.text.MessageFormat;

class ScanlineValueShortBitFieldReader implements BandReader {

    private final MetopFile metopFile;
    private final ImageInputStream inputStream;
    private final ScanlineBandDescription description;
    private final int lineOffset;

    ScanlineValueShortBitFieldReader(MetopFile metopFile, ImageInputStream inputStream, ScanlineBandDescription description) {
        this.metopFile = metopFile;
        this.inputStream = inputStream;
        this.description = description;
        this.lineOffset = description.getLineOffset();
    }

    @Override
    public String getBandName() {
        return description.getName();
    }

    @Override
    public String getBandUnit() {
        return null;
    }

    @Override
    public String getBandDescription() {
        return description.getDescription();
    }

    @Override
    public double getScalingFactor() {
        return 1.0;
    }

    @Override
    public int getDataType() {
        return description.getDataType();
    }

    @Override
    public void readBandRasterData(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY, ProductData destBuffer, ProgressMonitor pm) throws IOException {
        final AvhrrFile.RawCoordinates rawCoord = metopFile.getRawCoordinates(sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight);
        final short[] targetData = (short[]) destBuffer.getElems();

        pm.beginTask(MessageFormat.format("Reading AVHRR band ''{0}''...", getBandName()), rawCoord.maxY - rawCoord.minY);
        final byte[] bytes = new byte[2];

        int targetIdx = rawCoord.targetStart;
        for (int sourceY = rawCoord.minY; sourceY <= rawCoord.maxY; sourceY += sourceStepY) {
            if (pm.isCanceled()) {
                break;
            }

            int flagOffset = metopFile.getScanLineOffset(sourceY) + lineOffset;
            inputStream.seek(flagOffset);
            inputStream.read(bytes);

            for (int sourceX = rawCoord.minX; sourceX <= rawCoord.maxX; sourceX += sourceStepX) {
                targetData[targetIdx] = (short) bytes[0];   // we can do this because the upper 8 bits are unused tb 2016-06-24
                targetIdx += rawCoord.targetIncrement;
            }
            pm.worked(1);
        }
        pm.done();
    }
}
