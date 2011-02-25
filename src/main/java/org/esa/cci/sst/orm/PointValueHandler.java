package org.esa.cci.sst.orm;

import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.meta.JavaSQLTypes;
import org.apache.openjpa.jdbc.meta.ValueMapping;
import org.apache.openjpa.jdbc.meta.strats.AbstractValueHandler;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.ColumnIO;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.postgis.PGgeometry;
import org.postgresql.util.PGobject;

import java.sql.SQLException;
import java.sql.Types;

/**
 * Mapping between PGGeometry Java objects and GEOGRAPHY database entries.
 * This mapping is used in the ReferenceObservation class in the annotation of the
 * corresponding field's getter. Note that there are in fact two "conversions"
 *  involved: JPA and JDBC. This handler only performs the JPA mapping.
 *
 * @author Martin Boettcher
 */
public class PointValueHandler extends AbstractValueHandler {

    /**
     * @deprecated
     */
    @Override
    public Column[] map(ValueMapping vm, String name, ColumnIO io,
                        boolean adapt) {
        DBDictionary dict = vm.getMappingRepository().getDBDictionary();
        DBIdentifier colName = DBIdentifier.newColumn(name, dict != null && dict.delimitAll());
        return map(vm, colName, io, adapt);
    }

    public Column[] map(ValueMapping vm, DBIdentifier name, ColumnIO io,
                        boolean adapt) {
        Column col = new Column();
        col.setIdentifier(name);
        col.setJavaType(JavaSQLTypes.JDBC_DEFAULT);  // essential in order not to use BLOB
        col.setSize(-1);
        col.setTypeIdentifier(DBIdentifier.newColumnDefinition("GEOGRAPHY(POINT,4326)"));
        col.setType(Types.BINARY);  // essential in order not to use BLOB
        return new Column[] {col};
    }

    @Override
    public Object toDataStoreValue(ValueMapping vm, Object val, JDBCStore store) {
        return super.toDataStoreValue(vm, val, store);
    }

    @Override
    public Object toObjectValue(ValueMapping vm, Object val) {
        // handle geography null value
        if (val == null) {
            return null;
        }
        try {
            return new PGgeometry(PGgeometry.geomFromString(((PGobject) val).getValue()));
        } catch (SQLException e) {
            throw new RuntimeException("conversion to PGgeometry failed", e);
        }
    }
}
