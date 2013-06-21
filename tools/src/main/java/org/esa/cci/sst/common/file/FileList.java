package org.esa.cci.sst.common.file;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import java.io.File;
import java.util.Date;
import java.util.List;

public final class FileList {

    private final Date date;
    private final List<File> files;

    public FileList(Date date, List<File> files) {
        this.date = date;
        this.files = files;
    }

    public Date getDate() {
        return date;
    }

    public List<File> getFiles() {
        return files;
    }
}
