package org.esa.beam.dataio.metop;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.avhrr.AvhrrConstants;
import org.esa.beam.dataio.avhrr.AvhrrFile;
import org.esa.beam.framework.datamodel.ProductData;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.text.MessageFormat;

class Ch3BandReader extends PlainBandReader {


    Ch3BandReader(int channel, AvhrrFile metopFile, ImageInputStream inputStream) {
        super(channel, metopFile, inputStream);
    }

    @Override
    public void readBandRasterData(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY, ProductData destBuffer, ProgressMonitor pm) throws IOException {
        final AvhrrFile.RawCoordinates rawCoord = metopFile.getRawCoordinates(sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight);
        final short[] targetData = (short[]) destBuffer.getElems();

        pm.beginTask(MessageFormat.format("Reading AVHRR band ''{0}''...", getBandName()), rawCoord.maxY - rawCoord.minY);

        int targetIdx = rawCoord.targetStart;
        for (int sourceY = rawCoord.minY; sourceY <= rawCoord.maxY; sourceY += sourceStepY) {
            if (pm.isCanceled()) {
                break;
            }

            final int frameIndicator = metopFile.readFrameIndicator(sourceY);
            if (createFillValueLine(frameIndicator)) {
                for (int sourceX = rawCoord.minX; sourceX <= rawCoord.maxX; sourceX += sourceStepX) {
                    targetData[targetIdx] = AvhrrConstants.NO_DATA_VALUE;
                    targetIdx += rawCoord.targetIncrement;
                }
            } else {
                final int dataOffset = getDataOffset(sourceOffsetX, sourceY);
                synchronized (inputStream) {
                    inputStream.seek(dataOffset);
                    inputStream.readFully(targetData, targetIdx, sourceWidth);
                }
                targetIdx += sourceWidth;
            }

            pm.worked(1);
        }
        pm.done();
    }


    // package access for testing only tb 2016-09-14
    boolean createFillValueLine(int frameIndicator) {
        return isChannel3a(frameIndicator) ^ channel == AvhrrConstants.CH_3A;
    }

    // package access for testing only tb 2016-09-14
    static boolean isChannel3a(int frameIndicator) {
        return (frameIndicator & 1) == 1;
    }

}
