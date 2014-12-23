/*
 * Copyright (c) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.dataio.metop;

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductData.UTC;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * Can read a Generic Record Header (GRH) and holds it's information
 * in public accessible fields.
 *
 * @author Marco Zühlke
 */
class GenericRecordHeader {

    RecordClass recordClass;

    InstrumentGroup instrumentGroup;

    int recordSubclass;

    int recordSubclassVersion;

    long recordSize;

    ProductData.UTC recordStartTime;

    ProductData.UTC recordEndTime;

    boolean readGenericRecordHeader(ImageInputStream imageInputStream) throws IOException {
        byte rc = imageInputStream.readByte();
        if (!RecordClass.isValid(rc)) {
            return false;
        }
        recordClass = RecordClass.values()[rc];
        
        byte ig = imageInputStream.readByte();
        if (!InstrumentGroup.isValid(ig)) {
            return false;
        }
        instrumentGroup = InstrumentGroup.values()[ig];
        
        recordSubclass = imageInputStream.readByte();
        recordSubclassVersion = imageInputStream.readByte();
        recordSize = imageInputStream.readUnsignedInt();
        
        int day = imageInputStream.readUnsignedShort();
        long millis = imageInputStream.readUnsignedInt();
        long seconds = millis / 1000;
        long micros = (millis - seconds * 1000) * 1000;
        recordStartTime = new UTC(day, (int) seconds, (int) micros);
        
        day = imageInputStream.readUnsignedShort();
        millis = imageInputStream.readUnsignedInt();
        seconds = millis / 1000;
        micros = (millis - seconds * 1000) * 1000;
        recordEndTime = new UTC(day, (int) seconds, (int) micros);
        
        return true;
    }

    enum RecordClass {
        RESERVED,
        MPHR,
        SPHR,
        IPR,
        GEADR,
        GIADR,
        VEADR,
        VIADR,
        MDR;
        
        public static boolean isValid(int index) {
            return (index >=0 && index <= 8);
        }
    }

    enum InstrumentGroup {
        GENERIC,
        AMSU_A,
        ASCAT,
        ATOVS,
        AVHRR_3,
        GOME,
        GRAS,
        HIRS_4,
        IASI,
        MHS,
        SEM,
        ADCS,
        SBUV,
        DUMMY,
        ARCHIVE,
        IASI_L2;
        
        static boolean isValid(int index) {
            return (index >=0 && index <= 15);
        }
    }

}
