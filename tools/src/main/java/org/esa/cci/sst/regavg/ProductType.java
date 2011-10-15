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
    ARC_L3U(new ArcL3UFileType()),
    ARC_L2P(new ArcL2PFileType()),
    CCI_L3U(new CciL3UFileType()),
    CCI_L3C(new CciL3CFileType()),
    CCI_L4(new CciL4FileType());

    private final FileType fileType;

    private ProductType(FileType fileType) {
        this.fileType = fileType;
    }

    public FileType getFileType() {
        return fileType;
    }

    public Date parseDate(File file) throws ParseException {
        return fileType.parseDate(file);
    }

    public String getDefaultFilenameRegex() {
        return fileType.getDefaultFilenameRegex();
    }

    public GridDef getGridDef() {
        return fileType.getGridDef();
    }

    public ProcessingLevel getProcessingLevel() {
        return fileType.getProcessingLevel();
    }
}
