/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.cci.sst.tools.arcprocessing;

import org.esa.cci.sst.tools.Constants;
import org.esa.cci.sst.tools.MmsTool;
import org.esa.cci.sst.tools.ToolException;

import java.util.List;

/**
 * @author Thomas Storm
 */
public class Arc2ProcessingTool extends MmsTool {

    public static void main(String[] args) throws ToolException {
        final Arc2ProcessingTool tool = new Arc2ProcessingTool();
        tool.setCommandLineArgs(args);
        tool.initialize();
        final List<AvhrrInfo> avhrrFilesAndPoints = inquireAvhrrFilesAndPoints(args);
        tool.uploadPixelPosFile();
        tool.prepareArc2Calls(avhrrFilesAndPoints);

    }

    private void uploadPixelPosFile() {
        // todo - ts 19Apr2011 - replace by actual call
        System.out.format("scp %s tstorm@eddie.ecdf.ed.ac.uk:tmp/\n", getPixelPosFile());
    }

    private static List<AvhrrInfo> inquireAvhrrFilesAndPoints(final String[] args) throws ToolException {
        final Arc1ProcessingTool delegate = new Arc1ProcessingTool();
        delegate.setCommandLineArgs(args);
        delegate.initialize();
        return delegate.inquireAvhrrInfos();
    }

    public Arc2ProcessingTool() {
        super("mmsarc2.sh", "0.1");
    }

    private String[] prepareArc2Calls(final List<AvhrrInfo> avhrrFilesAndPoints) {
        String[] calls = new String[avhrrFilesAndPoints.size()];
        StringBuilder callBuilder = new StringBuilder();
        for (AvhrrInfo info : avhrrFilesAndPoints) {
            callBuilder.append("/exports/work/geos_gc_sst_cci/avhrr/scripts/start_ARC2.bash");
            callBuilder.append(' ');
            callBuilder.append(info.getFilename());
            callBuilder.append(' ');
            callBuilder.append(getPixelPosFile());
        }
        return calls;
    }

    private String getPixelPosFile() {
        final String locationFile = getConfiguration().getProperty(Constants.LOCATIONFILE_PROPERTY);
        return locationFile + Constants.PIXEL_POS_SUFFIX;
    }
}
