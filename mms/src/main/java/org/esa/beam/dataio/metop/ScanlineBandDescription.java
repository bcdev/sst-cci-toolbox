package org.esa.beam.dataio.metop;

class ScanlineBandDescription {

    private String name;
    private String description;
    private int dataType;
    private int lineOffset;

    ScanlineBandDescription(String name, String description, int dataType, int lineOffset) {
        this.name = name;
        this.description = description;
        this.dataType = dataType;
        this.lineOffset = lineOffset;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getDataType() {
        return dataType;
    }

    public int getLineOffset(){
        return lineOffset;
    }
}
