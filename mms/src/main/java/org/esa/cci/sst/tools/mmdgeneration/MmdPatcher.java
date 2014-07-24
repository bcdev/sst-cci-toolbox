/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.cci.sst.tools.mmdgeneration;

import org.esa.cci.sst.tools.BasicTool;
import org.esa.cci.sst.tools.ToolException;
import org.esa.cci.sst.util.TimeUtil;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.Date;

/**
 * Re-writes given variables to given MMD file.
 *
 * @author Thomas Storm
 */
public class MmdPatcher extends BasicTool {

    private static final int SCALE_FACTOR = 100;
    private static final short FILL_VALUE = Short.MIN_VALUE;

    NetcdfFileWriteable mmd;

    protected MmdPatcher() {
        super("mmdpatch-tool.sh", "0.1");
    }

    public static void main(String[] args) throws Exception {
        final MmdPatcher mmdPatcher = new MmdPatcher();
        if (! mmdPatcher.setCommandLineArgs(args)) {
            return;
        }
        mmdPatcher.initialize();
        mmdPatcher.run();
        mmdPatcher.close();
    }

    @Override
    public void initialize() {
        super.initialize();
        final String mmdLocation = getConfig().getStringValue("mms.mmdpatch.mmd");
        try {
            final boolean canOpen = NetcdfFileWriteable.canOpen(mmdLocation);
            if(!canOpen) {
                throw new Exception("Cannot open file '" + mmdLocation + "'.");
            }
            mmd = NetcdfFileWriteable.openExisting(mmdLocation);
        } catch (Exception e) {
            throw new ToolException(e.getMessage(), e, ToolException.TOOL_CONFIGURATION_ERROR);
        }
    }

    private void run() throws IOException, InvalidRangeException {
        Variable timeVariable = mmd.findVariable(NetcdfFile.makeValidPathName("atsr.1.time"));
        Variable detectorTemperatureVariable = mmd.findVariable(NetcdfFile.makeValidPathName("atsr.1.detector_temperature_12"));
        int noOfRecords = timeVariable.getShape()[0];
        final Array timeBuffer = timeVariable.read(new int[]{0}, timeVariable.getShape());
        final Array detectorTemperatureBuffer = detectorTemperatureVariable.read(new int[]{0}, detectorTemperatureVariable.getShape());
        for (int recordNo = 0; recordNo < noOfRecords; ++recordNo) {
            short temperature = FILL_VALUE;
            int time = timeBuffer.getInt(recordNo);
            if (time != Short.MIN_VALUE) {
                Date date = TimeUtil.secondsSince1978ToDate(time);
                float detectorTemperature = DetectorTemperatureProvider.create().getDetectorTemperature(date);
                if (detectorTemperature != DetectorTemperatureProvider.FILL_VALUE) {
                    temperature = (short) (detectorTemperature * SCALE_FACTOR);
                }
            }
            detectorTemperatureBuffer.setShort(recordNo, temperature);
        }
        mmd.write(NetcdfFile.makeValidPathName("atsr.1.detector_temperature_12"), new int[] { 0 }, detectorTemperatureBuffer);
        // set atsr.2.detector_temperature_12 to fill value
        Variable timeVariable2 = mmd.findVariable(NetcdfFile.makeValidPathName("atsr.2.time"));
        Variable detectorTemperatureVariable2 = mmd.findVariable(NetcdfFile.makeValidPathName("atsr.2.detector_temperature_12"));
        int noOfRecords2 = timeVariable2.getShape()[0];
        final Array detectorTemperatureBuffer2 = detectorTemperatureVariable.read(new int[]{0}, detectorTemperatureVariable2.getShape());
        for (int recordNo = 0; recordNo < noOfRecords2; ++recordNo) {
            short temperature = FILL_VALUE;
            detectorTemperatureBuffer2.setShort(recordNo, temperature);
        }
        mmd.write(NetcdfFile.makeValidPathName("atsr.2.detector_temperature_12"), new int[] { 0 }, detectorTemperatureBuffer2);

    }

    private void close() {
        try {
            mmd.close();
        } catch (IOException e) {
            getLogger().warning("File '" + mmd.getLocation() + "' could not be closed.");
        }
    }
}
