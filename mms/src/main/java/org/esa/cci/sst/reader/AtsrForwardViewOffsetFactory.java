package org.esa.cci.sst.reader;

import java.text.MessageFormat;

/**
 * @author Ralf Quast
 */
class AtsrForwardViewOffsetFactory {

    Offset createOffset(String name, int year) {
        if (name == null) {
            throw new IllegalArgumentException("Parameter 'name' must not be null.");
        }
        if (name.startsWith("ATS1")) {
            switch (year) {
                case 1990:
                case 1991:
                    return new Offset(-1, 0);
                case 1992:
                case 1993:
                case 1994:
                case 1995:
                    return new Offset(0, 0);
                case 1996:
                    return new Offset(0, 1);
                case 1997:
                case 1998:
                    return new Offset(0, 0);
                default:
                    throw new IllegalArgumentException(MessageFormat.format("Illegal year {0}.", year));
            }
        }
        return new Offset(0, 0);
    }

    static class Offset {

        private int acrossTrackOffset;
        private int alongTrackOffset;

        public Offset(int acrossTrackOffset, int alongTrackOffset) {
            this.acrossTrackOffset = acrossTrackOffset;
            this.alongTrackOffset = alongTrackOffset;
        }

        public int getAcrossTrackOffset() {
            return acrossTrackOffset;
        }

        public int getAlongTrackOffset() {
            return alongTrackOffset;
        }
    }
}
