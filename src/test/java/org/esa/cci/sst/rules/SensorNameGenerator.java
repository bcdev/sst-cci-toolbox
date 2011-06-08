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

    private static final String CONTENT = new StringBuilder()
            .append("/*\n")
            .append(" * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)\n")
            .append(" * \n")
            .append(" * This program is free software; you can redistribute it and/or modify it\n")
            .append(" * under the terms of the GNU General Public License as published by the Free\n")
            .append(" * Software Foundation; either version 3 of the License, or (at your option)\n")
            .append(" * any later version.\n")
            .append(" * This program is distributed in the hope that it will be useful, but WITHOUT\n")
            .append(" * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or\n")
            .append(" * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for\n")
            .append(" * more details.\n")
            .append(" * \n")
            .append(" * You should have received a copy of the GNU General Public License along\n")
            .append(" * with this program; if not, see http://www.gnu.org/licenses/\n")
            .append(" */\n")
            .append('\n')
            .append("package org.esa.cci.sst.rules;\n")
            .append('\n')
            .append("/**\n")
            .append(" * Sets the sensor to '%s'.\n")
            .append(" *\n")
            .append(" * @author Thomas Storm\n")
            .append(" */\n")
            .append("@SuppressWarnings({\"ClassTooDeepInInheritanceTree\", \"UnusedDeclaration\"})\n")
            .append("class %s extends SensorRule {\n")
            .append('\n')
            .append("    %s() {\n")
            .append("        super(\"%s\");\n")
            .append("    }\n")
            .append('}').toString();

    public static void main(String[] args) throws IOException {
        createClass("InsituSensor", "history");
//        createClass("SeaiceSensor", "seaice");
//        createClass("AaiSensor", "aai");
//        createClass("TmiSensor", "tmi");
//        createClass("AmsreSensor", "amsre");
//        createClass("AvhrrTnSensor", "avhrr.tn");
//        createClass("AvhrrM2Sensor", "avhrr.M2");
//        for (int i = 6; i <= 19; i++) {
//            createClass("avhrr." + i, "Avhrr" + i + "Sensor");
//        }
//        for (int i = 1; i <= 3; i++) {
//            createClass("atsr." + i, "Atsr" + i + "Sensor");
//        }
    }

    private static void createClass(String className, String sensorName) throws IOException {
        final File file = new File(className + ".java");
        FileWriter fw = new FileWriter(file);
        fw.write(String.format(CONTENT, sensorName, className, className, sensorName));
        fw.close();
    }

}
