package org.esa.cci.sst.regavg;

import org.esa.cci.sst.regavg.filetypes.*;
import org.esa.cci.sst.util.GridDef;

import java.io.File;
import java.text.ParseException;
import java.util.Date;

/**
 * Represents the product types handled by the {@link RegionalAverageTool}.
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
