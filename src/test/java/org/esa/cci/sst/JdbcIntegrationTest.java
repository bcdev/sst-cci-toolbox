package org.esa.cci.sst;

import org.junit.Ignore;
import org.postgis.PGgeometry;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;

/**
 * TODO add API doc
 *
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
