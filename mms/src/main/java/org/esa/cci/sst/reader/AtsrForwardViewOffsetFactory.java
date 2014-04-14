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
        if (name.startsWith("AT1")) {
            switch (year) {
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
                    return new Offset(0, 0);
                default:
                    throw new IllegalArgumentException(MessageFormat.format("Illegal year {0}.", year));
            }
        }
        if (name.startsWith("AT2")) {
            switch (year) {
                case 1995:
                case 1996:
                case 1997:
                case 1998:
                    return new Offset(-1, 0);
                case 1999:
                case 2000:
                    return new Offset(0, 0);
                case 2001:
                    return new Offset(1, 1);
                case 2002:
                    return new Offset(0, 0);
                case 2003:
                    return new Offset(1, 0);
                default:
                    throw new IllegalArgumentException(MessageFormat.format("Illegal year {0}.", year));
            }
        }
        if (name.startsWith("ATS")) {
            switch (year) {
                case 2002:
                case 2003:
                case 2004:
                case 2005:
                case 2006:
                case 2007:
                case 2008:
                    return new Offset(0, -1);
                case 2009:
                    return new Offset(1, -1);
                case 2010:
                    return new Offset(0, -1);
                case 2011:
                    return new Offset(1, -1);
                case 2012:
                    return new Offset(1, 0);
                default:
                    throw new IllegalArgumentException(MessageFormat.format("Illegal year {0}.", year));
            }
        }
        throw new IllegalArgumentException(MessageFormat.format("Illegal name {0}.", name));
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
