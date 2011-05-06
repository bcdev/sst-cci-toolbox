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

package org.esa.cci.sst;

import org.postgis.PGgeometry;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;

/**
 * @author Martin Boettcher
 */
public class JdbcIntegrationTest {

    public static void main(String[] args) {
        try {
            JdbcIntegrationTest t = new JdbcIntegrationTest();
            t.testCreateDbEntry();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testCreateDbEntry() throws Exception {
        Connection c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/mygisdb", "mms", "mms");
        ((org.postgresql.PGConnection) c).addDataType("geometry", PGgeometry.class);
        PreparedStatement s = c.prepareStatement("insert into mm_observation values (?,?,?,?,?,?,?,?)");
        s.setInt(1, 12);
        s.setBoolean(2, true);
        s.setObject(3, new PGgeometry("SRID=4326;POINT(-110 30)"));
        s.setString(4, "mist");
        s.setInt(5, 1);
        s.setString(6, "mist");
        s.setTimestamp(7, new Timestamp(System.currentTimeMillis()));
        s.setInt(8, 1);
        s.execute();
        c.close();
    }

}
