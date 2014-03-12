package org.esa.cci.sst.orm;

import org.esa.cci.sst.data.Matchup;
import org.esa.cci.sst.util.TimeUtil;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.Query;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class MatchupStorageImplTest {

    private PersistenceManager persistenceManager;
    private MatchupStorageImpl matchupStorage;
    private Query query;

    @Before
    public void setUp() {
        persistenceManager = mock(PersistenceManager.class);
        query = mock(Query.class);
        matchupStorage = new MatchupStorageImpl(persistenceManager);
    }

    @Test
    public void testInterfaceIsImplemented() {
        assertThat(matchupStorage, is(instanceOf(MatchupStorage.class)));
    }

    @Test
    public void testGetCount_noConditionNoPattern() throws ParseException {
        final String sql = "select count(m.id) from mm_matchup m, mm_observation r where r.time >= ?1 and r.time < ?2 and r.id = m.refobs_id";
        final Date startDate = createDate("2010-06-02T00:00:00Z");
        final Date stopDate = createDate("2010-06-05T00:00:00Z");

        final MatchupQueryParameter parameter = new MatchupQueryParameter();
        parameter.setStartDate(startDate);
        parameter.setStopDate(stopDate);

        when(query.getSingleResult()).thenReturn(19);
        when(persistenceManager.createNativeQuery(sql)).thenReturn(query);

        final int count = matchupStorage.getCount(parameter);
        assertEquals(19, count);

        verify(persistenceManager, times(1)).createNativeQuery(sql);
        verifyNoMoreInteractions(persistenceManager);

        verify(query, times(1)).setParameter(1, startDate);
        verify(query, times(1)).setParameter(2, stopDate);
        verify(query, times(1)).getSingleResult();
        verifyNoMoreInteractions(query);
    }

    @Test
    public void testGetCount_ConditionNoPattern() throws ParseException {
        final String sql = "select count(m.id) from mm_matchup m, mm_observation r where r.dataset = 0 and r.referenceflag = 2 and r.time >= ?1 and r.time < ?2 and r.id = m.refobs_id";
        final String condition = "r.dataset = 0 and r.referenceflag = 2";
        final Date startDate = createDate("2011-07-02T00:00:00Z");
        final Date stopDate = createDate("2011-07-05T00:00:00Z");

        final MatchupQueryParameter parameter = new MatchupQueryParameter();
        parameter.setStartDate(startDate);
        parameter.setStopDate(stopDate);
        parameter.setCondition(condition);

        when(query.getSingleResult()).thenReturn(20);
        when(persistenceManager.createNativeQuery(sql)).thenReturn(query);

        final int count = matchupStorage.getCount(parameter);
        assertEquals(20, count);

        verify(persistenceManager, times(1)).createNativeQuery(sql);
        verifyNoMoreInteractions(persistenceManager);

        verify(query, times(1)).setParameter(1, startDate);
        verify(query, times(1)).setParameter(2, stopDate);
        verify(query, times(1)).getSingleResult();
        verifyNoMoreInteractions(query);
    }

    @Test
    public void testGetCount_PatternNoCondition() throws ParseException {
        final String sql = "select count(m.id) from mm_matchup m, mm_observation r where m.pattern & ?3 = ?3 and r.time >= ?1 and r.time < ?2 and r.id = m.refobs_id";
        final Date startDate = createDate("2012-08-03T00:00:00Z");
        final Date stopDate = createDate("2012-08-06T00:00:00Z");

        final MatchupQueryParameter parameter = new MatchupQueryParameter();
        parameter.setStartDate(startDate);
        parameter.setStopDate(stopDate);
        parameter.setPattern(128);

        when(query.getSingleResult()).thenReturn(21);
        when(persistenceManager.createNativeQuery(sql)).thenReturn(query);

        final int count = matchupStorage.getCount(parameter);
        assertEquals(21, count);

        verify(persistenceManager, times(1)).createNativeQuery(sql);
        verifyNoMoreInteractions(persistenceManager);

        verify(query, times(1)).setParameter(1, startDate);
        verify(query, times(1)).setParameter(2, stopDate);
        verify(query, times(1)).setParameter(3, 128);
        verify(query, times(1)).getSingleResult();
        verifyNoMoreInteractions(query);
    }

    @Test
    public void testGetCount_PatternAndCondition() throws ParseException {
        final String sql = "select count(m.id) from mm_matchup m, mm_observation r where m.pattern & ?3 = ?3 and r.dataset = 0 and r.referenceflag = 2 and r.time >= ?1 and r.time < ?2 and r.id = m.refobs_id";
        final Date startDate = createDate("2012-08-03T00:00:00Z");
        final Date stopDate = createDate("2012-08-06T00:00:00Z");
        final String condition = "r.dataset = 0 and r.referenceflag = 2";

        final MatchupQueryParameter parameter = new MatchupQueryParameter();
        parameter.setStartDate(startDate);
        parameter.setStopDate(stopDate);
        parameter.setPattern(129);
        parameter.setCondition(condition);

        when(query.getSingleResult()).thenReturn(22);
        when(persistenceManager.createNativeQuery(sql)).thenReturn(query);

        final int count = matchupStorage.getCount(parameter);
        assertEquals(22, count);

        verify(persistenceManager, times(1)).createNativeQuery(sql);
        verifyNoMoreInteractions(persistenceManager);

        verify(query, times(1)).setParameter(1, startDate);
        verify(query, times(1)).setParameter(2, stopDate);
        verify(query, times(1)).setParameter(3, 129);
        verify(query, times(1)).getSingleResult();
        verifyNoMoreInteractions(query);
    }

    @Test
    public void testGet_noConditionNoPattern() throws ParseException {
        final String sql = "select m.id from mm_matchup m, mm_observation r where r.time >= ?1 and r.time < ?2 and r.id = m.refobs_id order by r.time, r.id";
        final Date startDate = createDate("2010-06-02T00:00:00Z");
        final Date stopDate = createDate("2010-06-05T00:00:00Z");
        final List<Matchup> resultList = new ArrayList<>();
        final Matchup matchup = new Matchup();
        matchup.setId(12);
        resultList.add(matchup);

        final MatchupQueryParameter parameter = new MatchupQueryParameter();
        parameter.setStartDate(startDate);
        parameter.setStopDate(stopDate);

        when(query.getResultList()).thenReturn(resultList);
        when(persistenceManager.createNativeQuery(sql, Matchup.class)).thenReturn(query);

        final List<Matchup> matchups = matchupStorage.get(parameter);
        assertNotNull(matchups);
        assertEquals(1, matchups.size());
        assertEquals(12, matchups.get(0).getId());

        verify(persistenceManager, times(1)).createNativeQuery(sql, Matchup.class);
        verifyNoMoreInteractions(persistenceManager);

        verify(query, times(1)).setParameter(1, startDate);
        verify(query, times(1)).setParameter(2, stopDate);
        verify(query, times(1)).getResultList();
        verifyNoMoreInteractions(query);
    }

    @Test
    public void testGet_conditionNoPattern() throws ParseException {
        final String sql = "select m.id from mm_matchup m, mm_observation r where r.referenceflag = 5 and r.time >= ?1 and r.time < ?2 and r.id = m.refobs_id order by r.time, r.id";
        final Date startDate = createDate("2010-06-02T00:00:00Z");
        final Date stopDate = createDate("2010-06-05T00:00:00Z");
        final String condition = "r.referenceflag = 5";
        final List<Matchup> resultList = new ArrayList<>();
        final Matchup matchup = new Matchup();
        matchup.setId(13);
        resultList.add(matchup);

        final MatchupQueryParameter parameter = new MatchupQueryParameter();
        parameter.setStartDate(startDate);
        parameter.setStopDate(stopDate);
        parameter.setCondition(condition);

        when(query.getResultList()).thenReturn(resultList);
        when(persistenceManager.createNativeQuery(sql, Matchup.class)).thenReturn(query);

        final List<Matchup> matchups = matchupStorage.get(parameter);
        assertNotNull(matchups);
        assertEquals(1, matchups.size());
        assertEquals(13, matchups.get(0).getId());

        verify(persistenceManager, times(1)).createNativeQuery(sql, Matchup.class);
        verifyNoMoreInteractions(persistenceManager);

        verify(query, times(1)).setParameter(1, startDate);
        verify(query, times(1)).setParameter(2, stopDate);
        verify(query, times(1)).getResultList();
        verifyNoMoreInteractions(query);
    }

    @Test
    public void testGet_patternNoCondition() throws ParseException {
        final String sql = "select m.id from mm_matchup m, mm_observation r where r.time >= ?1 and r.time < ?2 and r.id = m.refobs_id and m.pattern & ?3 = ?3 order by r.time, r.id";
        final Date startDate = createDate("2010-06-02T00:00:00Z");
        final Date stopDate = createDate("2010-06-05T00:00:00Z");
        final int pattern = 7765;

        final List<Matchup> resultList = new ArrayList<>();
        final Matchup matchup = new Matchup();
        matchup.setId(14);
        resultList.add(matchup);

        final MatchupQueryParameter parameter = new MatchupQueryParameter();
        parameter.setStartDate(startDate);
        parameter.setStopDate(stopDate);
        parameter.setPattern(pattern);

        when(query.getResultList()).thenReturn(resultList);
        when(persistenceManager.createNativeQuery(sql, Matchup.class)).thenReturn(query);

        final List<Matchup> matchups = matchupStorage.get(parameter);
        assertNotNull(matchups);
        assertEquals(1, matchups.size());
        assertEquals(14, matchups.get(0).getId());

        verify(persistenceManager, times(1)).createNativeQuery(sql, Matchup.class);
        verifyNoMoreInteractions(persistenceManager);

        verify(query, times(1)).setParameter(1, startDate);
        verify(query, times(1)).setParameter(2, stopDate);
        verify(query, times(1)).setParameter(3, pattern);
        verify(query, times(1)).getResultList();
        verifyNoMoreInteractions(query);
    }

    @Test
    public void testGet_patternAndCondition() throws ParseException {
        final String sql = "select m.id from mm_matchup m, mm_observation r where r.referenceflag = 6 and r.time >= ?1 and r.time < ?2 and r.id = m.refobs_id and m.pattern & ?3 = ?3 order by r.time, r.id";
        final Date startDate = createDate("2010-06-02T00:00:00Z");
        final Date stopDate = createDate("2010-06-05T00:00:00Z");
        final int pattern = 7765;
        final String condition = "r.referenceflag = 6";

        final List<Matchup> resultList = new ArrayList<>();
        final Matchup matchup = new Matchup();
        matchup.setId(15);
        resultList.add(matchup);

        final MatchupQueryParameter parameter = new MatchupQueryParameter();
        parameter.setStartDate(startDate);
        parameter.setStopDate(stopDate);
        parameter.setPattern(pattern);
        parameter.setCondition(condition);

        when(query.getResultList()).thenReturn(resultList);
        when(persistenceManager.createNativeQuery(sql, Matchup.class)).thenReturn(query);

        final List<Matchup> matchups = matchupStorage.get(parameter);
        assertNotNull(matchups);
        assertEquals(1, matchups.size());
        assertEquals(15, matchups.get(0).getId());

        verify(persistenceManager, times(1)).createNativeQuery(sql, Matchup.class);
        verifyNoMoreInteractions(persistenceManager);

        verify(query, times(1)).setParameter(1, startDate);
        verify(query, times(1)).setParameter(2, stopDate);
        verify(query, times(1)).setParameter(3, pattern);
        verify(query, times(1)).getResultList();
        verifyNoMoreInteractions(query);
    }

    private Date createDate(String timeString) throws ParseException {
        return TimeUtil.parseCcsdsUtcFormat(timeString);
    }
}
