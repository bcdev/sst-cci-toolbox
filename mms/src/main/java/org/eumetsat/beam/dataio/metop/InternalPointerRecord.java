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

package org.eumetsat.beam.dataio.metop;

import javax.imageio.stream.ImageInputStream;

import org.eumetsat.beam.dataio.metop.GenericRecordHeader.InstrumentGroup;
import org.eumetsat.beam.dataio.metop.GenericRecordHeader.RecordClass;

import java.io.IOException;

/**
 * Reads an Internal Pointer record (IPR) and holds it's data
 * in accessible fields.
 *
 * @author marcoz
 * @version $Revision: 1.1.1.1 $ $Date: 2007/03/22 11:12:51 $
 */
class InternalPointerRecord {

    GenericRecordHeader header;

    public RecordClass targetRecordClass;
    public InstrumentGroup targetInstrumentGroup;
    public int targetRecordSubclass;
    public int targetRecordOffset;

    public void readRecord(ImageInputStream imageInputStream) throws IOException {
        header = new GenericRecordHeader();
        boolean correct = header.readGenericRecordHeader(imageInputStream);
        if (!correct ||
                header.recordClass != RecordClass.IPR ||
                header.instrumentGroup != InstrumentGroup.GENERIC) {
            throw new IllegalArgumentException("Bad GRH in IPR");
        }
        byte trc = imageInputStream.readByte();
        if (!RecordClass.isValid(trc)) {
            throw new IllegalArgumentException("Bad IPR: wrong targetRecordClass="+trc);
        }
        targetRecordClass = RecordClass.values()[trc];
        byte tig = imageInputStream.readByte();
        if (!InstrumentGroup.isValid(tig)) {
            throw new IllegalArgumentException("Bad IPR: wrong targetInstrumentGroup"+tig);
        }
        targetInstrumentGroup = InstrumentGroup.values()[tig];
        targetRecordSubclass = imageInputStream.readByte();
        targetRecordOffset = (int) imageInputStream.readUnsignedInt();
    }

    public void printIPR() {
        System.out.println("IPR");
        System.out.println("targetRecordClass " + targetRecordClass);
        System.out.println("targetInstrumentGroup " + targetInstrumentGroup);
        System.out.println("targetRecordSubclass " + targetRecordSubclass);
        System.out.println("targetRecordOffset " + targetRecordOffset);
    }
}
