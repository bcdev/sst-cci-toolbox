package org.esa.cci.sst.orm;

import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

public class MatchupQueryParameterTest {

    private MatchupQueryParameter parameter;

    @Before
    public void setUp() {
        parameter = new MatchupQueryParameter();
    }

    @Test
    public void testSetGetStartDate() {
        final Date startDate = new Date(88778734388L);

        parameter.setStartDate(startDate);
        assertEquals(startDate.getTime(), parameter.getStartDate().getTime());
    }

    @Test
    public void testSetGetStopDate() {
        final Date stopDate = new Date(88766387234L);

        parameter.setStopDate(stopDate);
        assertEquals(stopDate.getTime(), parameter.getStopDate().getTime());
    }

    @Test
    public void testSetGetCondition() {
        final String condition = "whenever true";

        parameter.setCondition(condition);
        assertEquals(condition, parameter.getCondition());
    }

    @Test
    public void testSetGetPattern() {
        final int pattern = 775623;

        parameter.setPattern(pattern);
        assertEquals(pattern, parameter.getPattern());
    }
}
