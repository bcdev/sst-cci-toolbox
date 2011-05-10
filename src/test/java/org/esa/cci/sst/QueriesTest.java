/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.cci.sst;

import org.esa.cci.sst.data.Item;
import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.tools.Constants;
import org.esa.cci.sst.util.TimeUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(QueriesTestRunner.class)
public class QueriesTest {

    private PersistenceManager pm;

    @Before
    public void setUp() throws IOException {
        final Properties configuration = new Properties();
        configuration.load(new FileInputStream("mms-config.properties"));

        pm = new PersistenceManager(Constants.PERSISTENCE_UNIT_NAME, configuration);
    }

    @Test
    public void testGetMatchupCount() throws ParseException {
        final Date startDate = TimeUtil.parseCcsdsUtcFormat("2010-06-02T00:00:00Z");
        final Date stopDate = TimeUtil.parseCcsdsUtcFormat("2010-06-03T00:00:00Z");
        @SuppressWarnings({"unchecked"})
        final int matchupCount = Queries.getMatchupCount(pm, startDate, stopDate);

        assertTrue(matchupCount >= 0);
    }

    @Test
    public void testGetAllColumns() {
        @SuppressWarnings({"unchecked"})
        final List<? extends Item> columnList = Queries.getAllColumns(pm);

        assertNotNull(columnList);
    }

    @Test
    public void testGetMatchups() throws ParseException {
        final Date startDate = TimeUtil.parseCcsdsUtcFormat("2010-06-02T00:00:00Z");
        final Date stopDate = TimeUtil.parseCcsdsUtcFormat("2010-06-03T00:00:00Z");
        @SuppressWarnings({"unchecked"})
        final List<Matchup> columnList = Queries.getMatchups(pm, startDate, stopDate);

        assertNotNull(columnList);
    }

}
