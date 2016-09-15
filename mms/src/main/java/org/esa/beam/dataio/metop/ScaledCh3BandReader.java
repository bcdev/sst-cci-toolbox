package org.esa.beam.dataio.metop;


import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.avhrr.AvhrrConstants;
import org.esa.beam.dataio.avhrr.AvhrrFile;
import org.esa.beam.dataio.avhrr.calibration.RadianceCalibrator;
import org.esa.beam.framework.datamodel.ProductData;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.text.MessageFormat;

class ScaledCh3BandReader extends CalibratedBandReader {

    ScaledCh3BandReader(int channel, MetopFile metopFile, ImageInputStream inputStream, RadianceCalibrator radianceCalibrator) {
        super(channel, metopFile, inputStream, radianceCalibrator);
    }

    @Override
    public void readBandRasterData(int sourceOffsetX,
                                   int sourceOffsetY,
                                   int sourceWidth,
                                   int sourceHeight,
                                   int sourceStepX,
                                   int sourceStepY,
                                   final ProductData destBuffer,
                                   final ProgressMonitor pm) throws IOException {

        final AvhrrFile.RawCoordinates rawCoord = metopFile.getRawCoordinates(sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight);
        final float[] targetData = (float[]) destBuffer.getElems();
        final float scalingFactor = (float) 1e-4;

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
                    final short[] radianceScanLine = new short[metopFile.getProductWidth()];
                    inputStream.seek(dataOffset);
                    inputStream.readFully(radianceScanLine, 0, sourceWidth);

                    for (int sourceX = 0; sourceX <= sourceWidth - 1; sourceX++) {
                        targetData[targetIdx] = calibrator.calibrate(radianceScanLine[sourceX] * scalingFactor);
                        targetIdx += rawCoord.targetIncrement;
                    }
                }
            }

            pm.worked(1);
        }
        pm.done();
    }

    // package access for testing only tb 2016-09-14
    boolean createFillValueLine(int frameIndicator) {
        return MetopReader.isChannel3a(frameIndicator) ^ channel == AvhrrConstants.CH_3A;
    }
}
