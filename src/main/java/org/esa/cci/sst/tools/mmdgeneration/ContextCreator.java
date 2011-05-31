package org.esa.cci.sst.tools.mmdgeneration;

import org.esa.cci.sst.data.Coincidence;
import org.esa.cci.sst.data.DataFile;
import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.reader.ExtractDefinition;
import org.esa.cci.sst.reader.Reader;
import org.esa.cci.sst.rules.Context;
import org.esa.cci.sst.tools.ToolException;
import org.esa.cci.sst.util.ExtractDefinitionBuilder;
import org.esa.cci.sst.util.TimeUtil;
import org.postgis.Point;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Variable;

import java.io.IOException;
import java.text.MessageFormat;

class ContextCreator {

    private final MmdTool mmdTool;

    ContextCreator(MmdTool mmdTool) {
        this.mmdTool = mmdTool;
    }

    Context createContext(int recordNo, Matchup matchup, Coincidence coincidence, Variable variable) {
        final byte insituDataset = readInsituDataset(matchup, recordNo);
        final double matchupTime = readMatchupTime(matchup, recordNo);
        final double metopTime = readMetopTime(recordNo, coincidence);
        final Array metopDTimes = readMetopDTimes(recordNo, coincidence, variable);
        return new ContextBuilder()
                .matchup(matchup)
                .insituDataset(insituDataset)
                .matchupTime(matchupTime)
                .metopTime(metopTime)
                .metopDTimes(metopDTimes)
                .build();
    }

    double readMetopTime(int recordNo, Coincidence coincidence) {
        if (coincidence == null || !coincidence.getObservation().getSensor().equalsIgnoreCase("metop")) {
            return Double.NaN;
        }
        final ReferenceObservation observation = (ReferenceObservation) coincidence.getObservation();
        final Reader reader = tryAndGetReader(observation.getDatafile());
        return readObservationTime(recordNo, reader, observation);
    }

    Array readMetopDTimes(int recordNo, Coincidence coincidence, Variable variable) {
        final int rowCount = variable.getDimension(1).getLength();
        if (coincidence == null || !coincidence.getObservation().getSensor().equalsIgnoreCase("metop")) {
            return Array.factory(DataType.SHORT, new int[]{1, rowCount});
        }
        final ReferenceObservation observation = (ReferenceObservation) coincidence.getObservation();
        final Reader reader = tryAndGetReader(observation.getDatafile());
        final ExtractDefinition extractDefinition = new ExtractDefinitionBuilder()
                .coincidence(coincidence)
                .recordNo(recordNo)
                .shape(new int[]{1, rowCount})
                .build();
        return tryAndRead(reader, "dtime", extractDefinition);
    }

    double readMatchupTime(Matchup matchup, int recordNo) {
        final Reader reader = tryAndGetReader(matchup.getRefObs().getDatafile());
        final String sensor = matchup.getRefObs().getSensor();
        final OneDimOneValue oneDimOneValue = new OneDimOneValue(recordNo);
        if ("atsr_md".equalsIgnoreCase(sensor)) {
            return tryAndRead(reader, "atsr.time.julian", oneDimOneValue).getDouble(0);
        } else if ("metop".equalsIgnoreCase(sensor) || "seviri".equalsIgnoreCase(sensor)) {
            return readObservationTime(recordNo, reader, matchup.getRefObs());
        }
        return Double.NaN;
    }

    double readObservationTime(int recordNo, Reader reader, ReferenceObservation observation) {
        final OneDimOneValue oneDimOneValue = new OneDimOneValue(recordNo);
        final double msrTime = tryAndRead(reader, "msr_time", oneDimOneValue).getDouble(0);
        final Point point = observation.getPoint().getGeometry().getFirstPoint();
        final double lon = point.getX();
        final double lat = point.getY();
        final double dtime = tryAndRead(reader, "dtime", new TwoDimsOneValue(recordNo, lon, lat)).getDouble(0);
        final double julianMsrTime = TimeUtil.julianDateToSecondsSinceEpoch(msrTime);
        return julianMsrTime + dtime;
    }

    byte readInsituDataset(Matchup matchup, int recordNo) {
        final ReferenceObservation referenceObservation = matchup.getRefObs();
        final Reader reader = tryAndGetReader(referenceObservation.getDatafile());
        final String sensor = referenceObservation.getSensor();
        String variableName;
        if ("atsr_md".equalsIgnoreCase(sensor)) {
            variableName = "insitu.dataset";
        } else if ("metop".equalsIgnoreCase(sensor) || "seviri".equalsIgnoreCase(sensor)) {
            variableName = "msr_type";
        } else {
            throw new IllegalStateException(MessageFormat.format("Illegal primary sensor: ''{0}''.", sensor));
        }

        Array value = tryAndRead(reader, variableName, new OneDimOneValue(recordNo));
        return value.getByte(0);
    }

    Array tryAndRead(Reader reader, String variableName, ExtractDefinition extractDefinition) {
        Array value;
        try {
            value = reader.read(variableName, extractDefinition);
        } catch (IOException e) {
            throw new ToolException(MessageFormat.format("Unable to read from variable ''{0}''.", variableName), e,
                                    ToolException.TOOL_IO_ERROR);
        }
        return value;
    }

    Reader tryAndGetReader(DataFile datafile) {
        final Reader reader;
        try {
            reader = mmdTool.getReader(datafile);
        } catch (IOException e) {
            throw new ToolException(MessageFormat.format("Unable to get reader for datafile ''{0}''.",
                                                         datafile.toString()), e, ToolException.TOOL_IO_ERROR);
        }
        return reader;
    }
}