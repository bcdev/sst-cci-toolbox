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

package org.esa.cci.sst.tools.ingestion;

import org.esa.cci.sst.tools.ToolException;

/**
 * MmsTool responsible for ingesting mmd files which have been processed by ARC3. Uses {@link IngestionTool} as delegate.
 *
 * @author Thomas Storm
 */
public class MmdIngestionTool {

    private MmdIngestionTool() {
    }

    public static void main(String[] args) throws ToolException {
        final MmdIngester tool = new MmdIngester();
        final boolean performWork = tool.setCommandLineArgs(args);
        if (!performWork) {
            return;
        }
        tool.init(args);
        ingestDataInfo(tool);
        ingestVariableDescriptors(tool);
        ingestObservations(tool);
        ingestCoincidences(tool);
    }

    private static void ingestDataInfo(final MmdIngester tool) {
        final MmdDataInfoIngester mmdDataInfoIngester = new MmdDataInfoIngester(tool);
        mmdDataInfoIngester.ingestDataInfo();
    }

    private static void ingestVariableDescriptors(final MmdIngester tool) throws ToolException {
        tool.ingestVariableDescriptors();
    }

    private static void ingestObservations(final MmdIngester tool) throws ToolException {
        final MmdObservationIngester observationIngester = new MmdObservationIngester(tool);
        observationIngester.ingestObservations();
    }

    private static void ingestCoincidences(final MmdIngester tool) throws ToolException {
        final MmdCoincidenceIngester coincidenceIngester = new MmdCoincidenceIngester(tool);
        coincidenceIngester.ingestCoincidences();
    }
}
