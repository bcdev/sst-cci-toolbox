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

package org.esa.cci.sst.rules;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author Thomas Storm
 */
public class SensorNameGenerator {

    private static final String CONTENT = "/*\n" +
                                          " * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)\n" +
                                          " * \n" +
                                          " * This program is free software; you can redistribute it and/or modify it\n" +
                                          " * under the terms of the GNU General Public License as published by the Free\n" +
                                          " * Software Foundation; either version 3 of the License, or (at your option)\n" +
                                          " * any later version.\n" +
                                          " * This program is distributed in the hope that it will be useful, but WITHOUT\n" +
                                          " * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or\n" +
                                          " * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for\n" +
                                          " * more details.\n" +
                                          " * \n" +
                                          " * You should have received a copy of the GNU General Public License along\n" +
                                          " * with this program; if not, see http://www.gnu.org/licenses/\n" +
                                          " */\n" +
                                          "\n" +
                                          "package org.esa.cci.sst.rules;\n" +
                                          "\n" +
                                          "/**\n" +
                                          " * Sets the sensor to '%s'.\n" +
                                          " *\n" +
                                          " * @author Thomas Storm\n" +
                                          " */\n" +
                                          "@SuppressWarnings({\"ClassTooDeepInInheritanceTree\", \"UnusedDeclaration\"})\n" +
                                          "class %s extends SensorRule {\n" +
                                          "\n" +
                                          "    %s() {\n" +
                                          "        super(\"%s\");\n" +
                                          "    }\n" +
                                          "}";

    public static void main(String[] args) throws IOException {
        createClass("SeaiceSensor", "seaice");
        createClass("AaiSensor", "aai");
        createClass("TmiSensor", "tmi");
        createClass("AmsreSensor", "amsre");
        createClass("AvhrrTnSensor", "avhrr.tn");
        createClass("AvhrrM2Sensor", "avhrr.M2");
        for (int i = 6; i <= 19; i++) {
            createClass("avhrr." + i, "Avhrr" + i + "Sensor");
        }
        for (int i = 1; i <= 3; i++) {
            createClass("atsr." + i, "Atsr" + i + "Sensor");
        }
    }

    private static void createClass(String className, String sensorName) throws IOException {
        final File file = new File(className + ".java");
        FileWriter fw = new FileWriter(file);
        fw.write(String.format(CONTENT, sensorName, className, className, sensorName));
        fw.close();
    }

}
