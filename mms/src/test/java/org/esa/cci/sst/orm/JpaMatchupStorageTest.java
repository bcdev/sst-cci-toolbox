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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class JpaMatchupStorageTest {

    private PersistenceManager persistenceManager;
    private JpaMatchupStorage matchupStorage;
    private Query query;

    @Before
    public void setUp() {
        persistenceManager = mock(PersistenceManager.class);
        query = mock(Query.class);
        matchupStorage = new JpaMatchupStorage(persistenceManager);
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
        final List<Matchup> resultList = createOneMatchupListWithId(12);

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
        final List<Matchup> resultList = createOneMatchupListWithId(13);

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

        final List<Matchup> resultList = createOneMatchupListWithId(14);

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

        final List<Matchup> resultList = createOneMatchupListWithId(15);

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

    @Test
    public void testGetSelectMatchupSql_history() {
        final String matchupSql = JpaMatchupStorage.getSelectMatchupSql("history");
        assertThat(matchupSql, containsString("and not exists ( select o.id from mm_coincidence c, mm_observation o where c.matchup_id = m.id and c.observation_id = o.id and o.sensor = ?1 ) "));
    }

    @Test
    public void testGetSelectMatchupSql_atsrOrMetopOrAvhrr() {
        final String atsrSql = JpaMatchupStorage.getSelectMatchupSql("atsr_md");
        assertThat(atsrSql, containsString("from mm_matchup m, mm_observation r, mm_datafile f where r.time >= ?2 and r.time < ?3 and r.sensor = ?1 and m.id = r.id"));

        final String metopSql = JpaMatchupStorage.getSelectMatchupSql("metop");
        assertEquals(atsrSql, metopSql);

        final String avhrrSql = JpaMatchupStorage.getSelectMatchupSql("avhrr_md");
        assertEquals(avhrrSql, metopSql);
    }

    @Test
    public void testGetSelectMatchupSql_implicit() {
        final String implicitSql = JpaMatchupStorage.getSelectMatchupSql("Implicit");
        assertEquals("select r.id from mm_matchup m, mm_observation r, mm_datafile f where r.time >= ?2 and r.time < ?3 and m.id = r.id and f.id = r.datafile_id order by f.path, r.time, r.id",
                implicitSql);
    }

    @Test
    public void testGetSelectMatchupSql_notImplicit() {
        final String anyOtherSql = JpaMatchupStorage.getSelectMatchupSql("Any Other Sensor");
        assertEquals("select r.id from mm_matchup m, mm_observation r, mm_coincidence c, mm_observation o, mm_datafile f where r.time >= ?2 and r.time < ?3 and m.id = r.id and c.matchup_id = r.id and c.observation_id = o.id and o.sensor = ?1 and o.datafile_id = f.id order by f.path, r.time, r.id",
                anyOtherSql);
    }

    @Test
    public void testApplyPatternAndCondition_patternAndCondition() {
        final String sql = "bla bla bla where r.time = yesterday";
        final String condition = "absolutely nonsense";

        final String sqlApplied = JpaMatchupStorage.applyPatternAndCondition(sql, condition, 987);
        assertEquals("bla bla bla where pattern & ?4 = ?4 and absolutely nonsense and r.time = yesterday", sqlApplied);
    }

    @Test
    public void testApplyPatternAndCondition_onlyCondition() {
        final String sql = "yada yada where r.time = christmas";
        final String condition = "want_gift = TRUE";

        final String sqlApplied = JpaMatchupStorage.applyPatternAndCondition(sql, condition, 0);
        assertEquals("yada yada where want_gift = TRUE and r.time = christmas", sqlApplied);
    }

    @Test
    public void testApplyPatternAndCondition_onlyPattern() {
        final String sql = "select something cool where r.time = easter_last_year";

        final String sqlApplied = JpaMatchupStorage.applyPatternAndCondition(sql, null, 564);
        assertEquals("select something cool where pattern & ?4 = ?4 and r.time = easter_last_year", sqlApplied);
    }

    @Test
    public void testApplyPatternAndCondition_neitherPatternNorCondition() {
        final String sql = "select beer where r.time = this_evening";

        final String sqlApplied = JpaMatchupStorage.applyPatternAndCondition(sql, null, 0);
        assertEquals(sql, sqlApplied);
    }

    @Test
    public void testGetForMmd_history_noPatternNoCondition() throws ParseException {
        final String sql = "select u.id from ((select r.id id, f.path p, r.time t from mm_matchup m, mm_observation r, mm_coincidence c, mm_observation o, mm_datafile f where r.time >= ?2 and r.time < ?3 and m.id = r.id and c.matchup_id = r.id and c.observation_id = o.id and o.sensor = ?1 and o.datafile_id = f.id ) union (select r.id id, f.path p, r.time t from mm_matchup m, mm_observation r, mm_datafile f where r.time >= ?2 and r.time < ?3 and m.id = r.id and f.id = r.datafile_id and not exists ( select o.id from mm_coincidence c, mm_observation o where c.matchup_id = m.id and c.observation_id = o.id and o.sensor = ?1 ) ) order by p, t, id) as u";
        final String sensorName = "history";
        final Date startDate = createDate("2011-07-03T00:00:00Z");
        final Date stopDate = createDate("2011-07-06T00:00:00Z");

        final List<Matchup> resultList = createOneMatchupListWithId(16);

        final MatchupQueryParameter parameter = new MatchupQueryParameter();
        parameter.setSensorName(sensorName);
        parameter.setStartDate(startDate);
        parameter.setStopDate(stopDate);

        when(query.getResultList()).thenReturn(resultList);
        when(persistenceManager.createNativeQuery(sql, Matchup.class)).thenReturn(query);

        final List<Matchup> forMmd = matchupStorage.getForMmd(parameter);
        assertEquals(1, forMmd.size());
        assertEquals(16, forMmd.get(0).getId());

        verify(persistenceManager, times(1)).createNativeQuery(sql, Matchup.class);
        verifyNoMoreInteractions(persistenceManager);

        verify(query, times(1)).setParameter(1, sensorName);
        verify(query, times(1)).setParameter(2, startDate);
        verify(query, times(1)).setParameter(3, stopDate);
        verify(query, times(1)).getResultList();
        verifyNoMoreInteractions(query);
    }

    @Test
    public void testGetForMmd_atsr_patternNoCondition() throws ParseException {
        final String sql = "select u.id from ((select r.id id, f.path p, r.time t from mm_matchup m, mm_observation r, mm_coincidence c, mm_observation o, mm_datafile f where pattern & ?4 = ?4 and r.time >= ?2 and r.time < ?3 and m.id = r.id and c.matchup_id = r.id and c.observation_id = o.id and o.sensor = ?1 and o.datafile_id = f.id ) union (select r.id id, f.path p, r.time t from mm_matchup m, mm_observation r, mm_datafile f where pattern & ?4 = ?4 and r.time >= ?2 and r.time < ?3 and r.sensor = ?1 and m.id = r.id and f.id = r.datafile_id) order by p, t, id) as u";
        final String sensorName = "atsr_md";
        final Date startDate = createDate("2012-08-04T00:00:00Z");
        final Date stopDate = createDate("2012-08-07T00:00:00Z");
        final int pattern = 6672;

        final List<Matchup> resultList = createOneMatchupListWithId(17);

        final MatchupQueryParameter parameter = new MatchupQueryParameter();
        parameter.setSensorName(sensorName);
        parameter.setStartDate(startDate);
        parameter.setStopDate(stopDate);
        parameter.setPattern(pattern);

        when(query.getResultList()).thenReturn(resultList);
        when(persistenceManager.createNativeQuery(sql, Matchup.class)).thenReturn(query);

        final List<Matchup> forMmd = matchupStorage.getForMmd(parameter);
        assertEquals(1, forMmd.size());
        assertEquals(17, forMmd.get(0).getId());

        verify(persistenceManager, times(1)).createNativeQuery(sql, Matchup.class);
        verifyNoMoreInteractions(persistenceManager);

        verify(query, times(1)).setParameter(1, sensorName);
        verify(query, times(1)).setParameter(2, startDate);
        verify(query, times(1)).setParameter(3, stopDate);
        verify(query, times(1)).setParameter(4, pattern);
        verify(query, times(1)).getResultList();
        verifyNoMoreInteractions(query);
    }

    @Test
    public void testGet_id() {
        final String sql = "select m from Matchup m where m.id = ?1";
        final int id = 8876;
        final long pattern = 12;
        final Matchup matchup = new Matchup();
        matchup.setPattern(pattern);
        matchup.setId(8876);

        when(persistenceManager.pick(sql, id)).thenReturn(matchup);

        final Matchup storageMatchup = matchupStorage.get(id);
        assertNotNull(storageMatchup);
        assertEquals(id, storageMatchup.getId());
        assertEquals(pattern, storageMatchup.getPattern());

        verify(persistenceManager, times(1)).pick(sql, id);
        verifyNoMoreInteractions(persistenceManager);
    }

    private Date createDate(String timeString) throws ParseException {
        return TimeUtil.parseCcsdsUtcFormat(timeString);
    }

    private List<Matchup> createOneMatchupListWithId(int id) {
        final List<Matchup> resultList = new ArrayList<>();
        final Matchup matchup = new Matchup();
        matchup.setId(id);
        resultList.add(matchup);
        return resultList;
    }
}
