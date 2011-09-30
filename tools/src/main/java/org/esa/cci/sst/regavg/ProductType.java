package org.esa.cci.sst.regavg;

import org.esa.cci.sst.util.Grid;
import org.esa.cci.sst.util.UTC;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * Represents the product types handled by the {@link RegionalAverageTool}.
 *
 * @author Norman Fomferra
 */
public enum ProductType {
    ARC_L3U {
        public final DateFormat dateFormat = UTC.getDateFormat("yyyyMMdd");
        public final String filenamePrefix = "AT2_AVG_3PAARC";
        // todo - Use regexp instead
        public final Pattern filenamePattern = Pattern.compile("AT._AVG_3PAARC\\d\\d\\d\\d\\d\\d\\d.*\\.nc");
        public final Grid GRID = Grid.createGlobalGrid(3600, 1800);

        @Override
        public Date getDate(String filename) {
            if (filename.startsWith(filenamePrefix)) {
                String dateString = filename.substring(filenamePrefix.length(), filenamePrefix.length() + 8);
                try {
                    return dateFormat.parse(dateString);
                } catch (ParseException e) {
                    // ok.
                }
            }
            return null;
        }

        @Override
        public Grid getGrid() {
            return GRID;
        }
    },

    ARC_L2P {
        @Override
        public Date getDate(String filename) {
            return null;
        }

        @Override
        public Grid getGrid() {
            return null;
        }
    },

    CCI_L3U {
        @Override
        public Date getDate(String filename) {
            return null;
        }


        @Override
        public Grid getGrid() {
            return null;
        }
    },

    CCI_L3C {
        @Override
        public Date getDate(String filename) {
            return null;
        }


        @Override
        public Grid getGrid() {
            return null;
        }
    },

    CCI_L4 {
        @Override
        public Date getDate(String filename) {
            return null;
        }


        @Override
        public Grid getGrid() {
            return null;
        }
    };

    public abstract Date getDate(String filename);
    public abstract Grid getGrid();
}
