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

public class GbcsTool {

    private static final String GBCS_CALL_TEMPLATE =
            "#! /bin/sh\n" +
            "module load intel/${mms.gbcs.intelversion} &&" +
            "${mms.gbcs.home}/bin/MMD_SCREEN_Linux ${mms.gbcs.home}/dat_cci/${INP_FILE} ${mms.gbcs.mmd.source} ${mms.gbcs.nwp.source} ${mms.gbcs.mmd.target}";

    public static void main(String[] args) {
        final Properties properties = new Properties();

        // TODO - select Sensor input file

        final ProcessRunner runner = new ProcessRunner("org.esa.cci.sst");
        try {
            runner.execute(ProcessRunner.writeExecutableScript(GBCS_CALL_TEMPLATE, properties).getPath());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }

}
