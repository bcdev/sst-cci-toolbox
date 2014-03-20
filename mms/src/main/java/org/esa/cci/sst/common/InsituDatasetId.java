package org.esa.cci.sst.common;


public enum InsituDatasetId {

    drifter((byte) 0),
    mooring((byte) 1),
    ship((byte) 2),
    gtmba((byte) 3),
    radiometer((byte) 4),
    argo((byte) 5),
    dummy_sea_ice((byte) 6),
    dummy_diurnal_variability((byte) 7),
    dummy_bc((byte) 8),
    xbt((byte) 9),
    mbt((byte) 10),
    ctd((byte) 11),
    animal((byte) 12),
    bottle((byte) 13);

    private final byte value;

    InsituDatasetId(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }

    public static InsituDatasetId create(byte value) {
        for (InsituDatasetId datasetId : values()) {
            if (datasetId.getValue() == value) {
                return datasetId;
            }
        }

        throw new IllegalArgumentException("Invalid InsituDatasetId: " + value);
    }

    public static String getNames() {
        final StringBuilder builder = new StringBuilder();
        for (InsituDatasetId datasetId : values()) {
            builder.append(datasetId);
            builder.append(" ");
        }

        return builder.substring(0, builder.length() - 1);
    }

    public static byte[] getValues() {
        final InsituDatasetId[] values = InsituDatasetId.values();
        final byte[] byteValues = new byte[values.length];

        for (int i = 0; i < values.length; i++) {
            byteValues[i] = values[i].getValue();
        }

        return byteValues;
    }
}
