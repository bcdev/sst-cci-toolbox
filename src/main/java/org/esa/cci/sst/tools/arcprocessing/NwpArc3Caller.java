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

package org.esa.cci.sst.tools.arcprocessing;

/**
 * Interface for generating nwp/arc3 calls.
 *
 * @author Thomas Storm
 */
public interface NwpArc3Caller {

    String SETUP = "if [ ! -d \"$MMS_HOME\" ]\n" +
                                    "then\n" +
                                    "    PRGDIR=`dirname $0`\n" +
                                    "    export MMS_HOME=`cd \"$PRGDIR/..\" ; pwd`\n" +
                                    "fi\n\n" +
                                    "set -e # one fails, all fail\n\n" +
                                    '\n' +
                                    "if [ -z \"$MMS_HOME\" ]; then\n" +
                                    "    echo\n" +
                                    "    echo Error:\n" +
                                    "    echo MMS_HOME does not exists in your environment. Please\n" +
                                    "    echo set the MMS_HOME variable in your environment to the\n" +
                                    "    echo location of your CCI SST installation.\n" +
                                    "    echo\n" +
                                    "    exit 2\n" +
                                    "fi\n\n" +
                                    "MMS_OPTIONS=\"\"\n" +
                                    "if [ ! -z $MMS_DEBUG ]; then\n" +
                                    "    MMS_OPTIONS=\"-Xdebug -Xrunjdwp:transport=dt_socket,address=8001,server=y,suspend=y\"\n" +
                                    "fi\n\n" +
                                    "export LD_LIBRARY_PATH=${LD_LIBRARY_PATH}:/home/tstorm/opt/local/lib\n" +
                                    "export PATH=${PATH}:/home/tstorm/opt/local/bin\n\n";

    String createNwpArc3Call();

    String createReingestionCall();

    String createCleanupCall(String... scripts);
}
