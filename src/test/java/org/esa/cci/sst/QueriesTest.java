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
import org.esa.cci.sst.data.ReferenceObservation;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.tools.Constants;
import org.esa.cci.sst.util.TimeUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(QueriesTestRunner.class)
public class QueriesTest {

    private PersistenceManager pm;

    @Before
    public void initPersistence() throws IOException {
        final Properties configuration = new Properties();

        InputStream is = null;
        try {
            is = new FileInputStream("mms-config.properties");
            configuration.load(is);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {
                }
            }
        }

        pm = new PersistenceManager(Constants.PERSISTENCE_UNIT_NAME, configuration);
    }

    @Test
    public void testGetMatchupCount() throws ParseException {
        final Date startDate = TimeUtil.parseCcsdsUtcFormat("2010-06-02T00:00:00Z");
        final Date stopDate = TimeUtil.parseCcsdsUtcFormat("2010-06-03T00:00:00Z");
        @SuppressWarnings({"unchecked"})
        final int matchupCount = Queries.getMatchupCount(pm, startDate, stopDate);

        assertEquals(14488, matchupCount);
    }

    @Test
    public void testGetAllColumns() {
        @SuppressWarnings({"unchecked"})
        final List<? extends Item> columnList = Queries.getAllColumns(pm);

        assertNotNull(columnList);
        assertEquals(353, columnList.size());
    }

    @Test
    public void testGetMatchups() throws ParseException {
        final Date startDate = TimeUtil.parseCcsdsUtcFormat("2010-06-02T00:00:00Z");
        final Date stopDate = TimeUtil.parseCcsdsUtcFormat("2010-06-03T00:00:00Z");
        @SuppressWarnings({"unchecked"})
        final List<Matchup> matchupList = Queries.getMatchups(pm, startDate, stopDate);

        assertNotNull(matchupList);
        assertEquals(14488, matchupList.size());
    }

    @Test
    public void testGetMatchupsForSensor() throws ParseException {
        final Date startDate = TimeUtil.parseCcsdsUtcFormat("2010-06-02T00:00:00Z");
        final Date stopDate = TimeUtil.parseCcsdsUtcFormat("2010-06-03T00:00:00Z");
        @SuppressWarnings({"unchecked"})
        final List<Matchup> matchupList = Queries.getMatchups(pm, startDate, stopDate, 1);

        assertNotNull(matchupList);
        assertEquals(1966, matchupList.size());
    }

    @Test
    public void testGetReferenceObservation() throws ParseException {
        final Date startDate = TimeUtil.parseCcsdsUtcFormat("2010-06-02T00:00:00Z");
        final Date stopDate = TimeUtil.parseCcsdsUtcFormat("2010-06-03T00:00:00Z");
        @SuppressWarnings({"unchecked"})
        final List<Matchup> matchupList = Queries.getMatchups(pm, startDate, stopDate);

        final Matchup matchup = matchupList.get(0);
        final ReferenceObservation expectedReferenceObservation = matchup.getRefObs();
        final int matchupId = matchup.getId();

        final ReferenceObservation referenceObservation = Queries.getReferenceObservationForMatchup(pm, matchupId);

        assertEquals(expectedReferenceObservation.getId(), referenceObservation.getId());
    }
}
