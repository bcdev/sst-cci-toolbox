package org.esa.cci.sst.regavg;

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
    ARC {
        public final DateFormat DATE_FORMAT = UTC.getDateFormat("yyyyMMdd");
        public final String FILENAME_PREFIX = "AT2_AVG_3PAARC";
        // todo - Use regexp instead
        public final Pattern  FILENAME_PATTERN = Pattern.compile("AT._AVG_3PAARC\\d\\d\\d\\d\\d\\d\\d.*\\.nc");


        @Override
        public Date getDate(String filename) {
            if (filename.startsWith(FILENAME_PREFIX)) {
                String dateString = filename.substring(FILENAME_PREFIX.length(), FILENAME_PREFIX.length() + 8);
                try {
                    return DATE_FORMAT.parse(dateString);
                } catch (ParseException e) {
                    // ok.
                }
            }
            return null;
        }
    },

    ARC_L3U {
        @Override
        public Date getDate(String filename) {
            return null;
        }
    },

    SST_CCI_L3U {
        @Override
        public Date getDate(String filename) {
            return null;
        }
    },

    SST_CCI_L3C {
        @Override
        public Date getDate(String filename) {
            return null;
        }
    };

    public abstract Date getDate(String filename);
}
