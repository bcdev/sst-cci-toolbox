/*
 * SST_cci Tools
 *
 * Copyright (C) 2011-2013 by Brockmann Consult GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.esa.cci.sst.util;

import org.esa.cci.sst.regavg.FileType;
import org.esa.cci.sst.regavg.ProcessingLevel;
import org.esa.cci.sst.regavg.filetypes.*;
import org.esa.cci.sst.util.GridDef;

import java.io.File;
import java.text.ParseException;
import java.util.Date;

/**
 * Represents the product types handled by the {@link org.esa.cci.sst.regavg.RegionalAverageTool}.
 *
 * @author Norman Fomferra
 */
public enum ProductType {
    ARC_L3U(ArcL3UFileType.INSTANCE),
    ARC_L2P(ArcL2PFileType.INSTANCE),
    CCI_L3U(CciL3UFileType.INSTANCE),
    CCI_L3C(CciL3CFileType.INSTANCE),
    CCI_L4(CciL4FileType.INSTANCE);

    private final FileType fileType;

    private ProductType(FileType fileType) {
        this.fileType = fileType;
    }

    public FileType getFileType() {
        return fileType;
    }

    public Date parseDate(File file) throws ParseException {
        return getFileType().parseDate(file);
    }

    public String getDefaultFilenameRegex() {
        return getFileType().getFilenameRegex();
    }

    public GridDef getGridDef() {
        return getFileType().getGridDef();
    }

    public ProcessingLevel getProcessingLevel() {
        return getFileType().getProcessingLevel();
    }
}
