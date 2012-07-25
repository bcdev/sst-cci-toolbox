package org.esa.cci.sst.regrid;


import org.esa.cci.sst.regrid.filetypes.CciL3CFileType;
import org.esa.cci.sst.regrid.filetypes.CciL3UFileType;
import org.esa.cci.sst.regrid.filetypes.CciL4FileType;

import java.io.File;
import java.text.ParseException;
import java.util.Date;

/**
 * @author Bettina Scholze
 *         Date: 23.07.12 10:51
 *         To change this template use File | Settings | File Templates.
 */
enum ProductType {

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
}
