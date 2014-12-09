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
 * involved: JPA and JDBC. This handler only performs the JPA mapping.
 *
 * @author Martin Boettcher
 */
public class GeographyValueHandler extends AbstractValueHandler {

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
        col.setTypeIdentifier(DBIdentifier.newColumnDefinition("GEOGRAPHY(GEOMETRY,4326)"));
        col.setType(Types.BINARY);  // essential in order not to use BLOB
        return new Column[]{col};
    }

    @Override
    public Object toDataStoreValue(ValueMapping vm, Object val, JDBCStore store) {
//        if (val == null) {
//            try {
//                return PGgeometry.geomFromString("POINT EMPTY");
//            } catch (SQLException e) {
//                throw new RuntimeException("conversion to PGgeometry failed", e);
//            }
//        }
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
