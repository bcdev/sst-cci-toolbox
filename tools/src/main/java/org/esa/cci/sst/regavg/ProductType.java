package org.esa.cci.sst.regavg;

import org.esa.cci.sst.util.GridDef;
import org.esa.cci.sst.util.UTC;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents the product types handled by the {@link RegionalAverageTool}.
 *
 * @author Norman Fomferra
 */
public enum ProductType {
    /**
     * n/d = night or day
     * N/D = Nadir or Dual view
     * 2/3 = 2 or 3 channel retrieval (3 chan only valid during night)
     * b/m = bayes or min-bayes cloud screening
     */
    ARC_L3U {
        public final DateFormat dateFormat = UTC.getDateFormat("yyyyMMdd");
        public final int filenameDateOffset = "AT._AVG_3PAARC".length();
        public final Pattern filenamePattern = Pattern.compile("AT._AVG_3PAARC\\d\\d\\d\\d\\d\\d\\d.*\\.nc.gz");
        public final GridDef GRID_DEF = GridDef.createGlobalGrid(3600, 1800);

        @Override
        public Date getDate(String filename) throws ParseException {
            Matcher matcher = filenamePattern.matcher(filename);
            if (matcher.matches()) {
                String dateString = filename.substring(filenameDateOffset, filenameDateOffset + 8);
                return dateFormat.parse(dateString);
            }
            return null;
        }

        @Override
        public GridDef getGridDef() {
            return GRID_DEF;
        }

        @Override
        public ProcessingLevel getProcessingLevel() {
            return ProcessingLevel.L3U;
        }
    },

    ARC_L2P {
        @Override
        public Date getDate(String filename) {
            return null;
        }

        @Override
        public GridDef getGridDef() {
            return null;
        }

        @Override
        public ProcessingLevel getProcessingLevel() {
            return ProcessingLevel.L2P;
        }
    },

    CCI_L3U {
        @Override
        public Date getDate(String filename) {
            return null;
        }


        @Override
        public GridDef getGridDef() {
            return null;
        }

        @Override
        public ProcessingLevel getProcessingLevel() {
            return ProcessingLevel.L3U;
        }
    },

    CCI_L3C {
        @Override
        public Date getDate(String filename) {
            return null;
        }


        @Override
        public GridDef getGridDef() {
            return null;
        }

        @Override
        public ProcessingLevel getProcessingLevel() {
            return ProcessingLevel.L3C;
        }
    },

    CCI_L4 {
        @Override
        public Date getDate(String filename) {
            return null;
        }


        @Override
        public GridDef getGridDef() {
            return null;
        }

        @Override
        public ProcessingLevel getProcessingLevel() {
            return ProcessingLevel.L4;
        }
    };

    public abstract Date getDate(String filename) throws ParseException;

    public abstract GridDef getGridDef();

    public abstract ProcessingLevel getProcessingLevel();
}
