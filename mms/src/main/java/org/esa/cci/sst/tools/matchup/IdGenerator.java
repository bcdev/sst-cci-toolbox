package org.esa.cci.sst.tools.matchup;

import org.esa.cci.sst.tool.Configuration;
import org.esa.cci.sst.tools.samplepoint.TimeRange;
import org.esa.cci.sst.util.ConfigUtil;
import org.esa.cci.sst.util.TimeUtil;

import java.util.Date;

class IdGenerator {

    private final int year;
    private final int month;
    private final int sensorId_0;
    private final int sensorId_1;

    private int current;
    private int currentUnique;

    static IdGenerator create(Configuration configuration) {
        final TimeRange timeRange = ConfigUtil.getTimeRange(Configuration.KEY_MMS_SAMPLING_START_TIME,
                Configuration.KEY_MMS_SAMPLING_STOP_TIME,
                configuration);
        final Date centerDate = TimeUtil.getCenterTime(timeRange.getStartDate(), timeRange.getStopDate());
        final int year = TimeUtil.getYear(centerDate);
        final int month = TimeUtil.getMonth(centerDate);

        final String[] sensorNames = configuration.getStringValue(Configuration.KEY_MMS_SAMPLING_SENSOR).split(",", 2);
        final int id_1 = SensorMap.idForName(sensorNames[0].trim());
        int id_2 = 0;
        if (sensorNames.length > 1) {
            id_2 = SensorMap.idForName(sensorNames[1].trim());
        }
        return new IdGenerator(year, month, id_1, id_2);
    }

    IdGenerator(int year, int month, int sensorId_0, int sensorId_1) {
        this.year = year;
        this.month = month;
        this.sensorId_0 = sensorId_0;
        this.sensorId_1 = sensorId_1;
    }

    int next() {
        return current++;
    }

    long nextUnique() {
        long result = currentUnique++;

        result += year * 1000000000000000L;
        result += month * 10000000000000L;
        result += sensorId_0 * 100000000000L;
        result += sensorId_1 * 1000000000L;

        return result;
    }
}
