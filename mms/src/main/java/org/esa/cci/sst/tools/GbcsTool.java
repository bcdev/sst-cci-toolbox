package org.esa.cci.sst.tools;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.cci.sst.util.ProcessRunner;

import java.io.IOException;
import java.util.Properties;

public class GbcsTool extends BasicTool {

    private static final String TEMPLATE =
            "#! /bin/sh\n" +
            "module load intel/${mms.gbcs.intelversion} &&" +
            "${mms.gbcs.home}/${mms.gbcs.version}/bin/MMD_SCREEN_Linux ${mms.gbcs.home}/${mms.gbcs.version}/dat_cci/${INP_FILE} ${mms.gbcs.mmd.source} ${mms.gbcs.nwp.source} ${mms.gbcs.mmd.target}";

    public GbcsTool() {
        super("gbcs-tool", "1.0");
    }

    public static void main(String[] args) {
        final GbcsTool tool = new GbcsTool();
        try {
            if (!tool.setCommandLineArgs(args)) {
                return;
            }
            tool.initialize();
            tool.run();
        } catch (ToolException e) {
            tool.getErrorHandler().terminate(e);
        } catch (Exception e) {
            tool.getErrorHandler().terminate(new ToolException(e.getMessage(), e, ToolException.UNKNOWN_ERROR));
        }
    }

    @Override
    public void initialize() {
        // nothing
    }

    private void run() throws IOException, InterruptedException {
        final Properties properties = new Properties();

        // TODO - select Sensor input file

        final ProcessRunner runner = new ProcessRunner("org.esa.cci.sst");
        final String resolvedTemplate = ProcessRunner.resolveTemplate(TEMPLATE, properties);

        runner.execute(ProcessRunner.writeExecutableScript(resolvedTemplate, "gbcs", ".sh").getPath());
    }

}
