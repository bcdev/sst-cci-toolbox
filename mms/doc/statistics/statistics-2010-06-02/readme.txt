observation statistics queries
==============================

mygisdb=> select sensor, count(*) from mm_observation group by sensor order by sensor;
  sensor   | count
-----------+-------
 aatsr.ref |  1966
 metop     |  7969
 metop.ref |  8253
 seviri    |  5800
(4 rows)

mygisdb=> select sensor, clearsky, count(*) from mm_observation group by sensor, clearsky order by sensor, clearsky;
  sensor   | clearsky | count
-----------+----------+-------
 metop.ref | t        |  4282
 metop.ref | f        |  3971
 seviri    | t        |  5800
 aatsr.ref | t        |   207
 metop     | t        |  4137
 aatsr.ref | f        |  1759
 metop     | f        |  3832
(7 rows)

matchup statistics query
========================

mygisdb=> select o1.sensor, o2.sensor, count(*) from mm_matchup m, mm_observation o1, mm_observation o2, mm_coincidence c2 where m.refobs_id = o1.id and c2.matchup_id = m.id and c2.observation_id = o2.id group by o1.sensor, o2.sensor order by o1.sensor, o2.sensor;
  sensor   | sensor | count
-----------+--------+-------
 aatsr.ref | metop  |   627
 aatsr.ref | seviri |   334
 metop.ref | seviri |  2674

mygisdb=> select o1.sensor, o2.sensor, o1.clearsky, count(*) from mm_matchup m, mm_observation o1, mm_observation o2, mm_coincidence c2 where m.refobs_id = o1.id and c2.matchup_id = m.id and c2.observation_id = o2.id group by o1.sensor, o2.sensor, o1.clearsky order by o1.sensor, o2.sensor, o1.clearsky desc;
  sensor   | sensor | clearsky | count
-----------+--------+----------+-------
 aatsr.ref | metop  | t        |   160
 aatsr.ref | metop  | f        |   467
 aatsr.ref | seviri | t        |    89
 aatsr.ref | seviri | f        |   245
 metop.ref | seviri | t        |  1449
 metop.ref | seviri | f        |  1225
(6 rows)

mygisdb=> select o1.sensor, o2.sensor, o3.sensor, count(*) from mm_matchup m, mm_observation o1, mm_observation o2, mm_observation o3, mm_coincidence c2, mm_coincidence c3 where m.refobs_id = o1.id and c2.matchup_id = m.id and c2.observation_id = o2.id and c3.matchup_id = m.id and c3.observation_id = o3.id and o2.sensor < o3.sensor group by o1.sensor, o2.sensor, o3.sensor order by o1.sensor, o2.sensor, o3.sensor;
  sensor   | sensor | sensor | count
-----------+--------+--------+-------
 aatsr.ref | metop  | seviri |   205

 mygisdb=> select o1.sensor, o2.sensor, o3.sensor, o1.clearsky, count(*) from mm_matchup m, mm_observation o1, mm_observation o2, mm_observation o3, mm_coincidence c2, mm_coincidence c3 where m.refobs_id = o1.id and c2.matchup_id = m.id and c2.observation_id = o2.id and c3.matchup_id = m.id and c3.observation_id = o3.id and o2.sensor < o3.sensor group by o1.sensor, o2.sensor, o3.sensor, o1.clearsky order by o1.sensor, o2.sensor, o3.sensor, o1.clearsky desc;
  sensor   | sensor | sensor | clearsky | count
-----------+--------+--------+----------+-------
 aatsr.ref | metop  | seviri | t        |    77
 aatsr.ref | metop  | seviri | f        |   128
(2 rows)

mygisdb=> select o1.sensor, count(*) from mm_matchup m, mm_observation o1 where m.refobs_id = o1.id group by o1.sensor;
  sensor   | count
-----------+-------
 aatsr.ref |   756
 metop.ref |  2674
(2 rows)

mygisdb=> select o1.sensor, o1.clearsky, count(*) from mm_matchup m, mm_observation o1 where m.refobs_id = o1.id group by o1.sensor, o1.clearsky order by o1.sensor, o1.clearsky desc;
  sensor   | clearsky | count
-----------+----------+-------
 aatsr.ref | t        |   172
 aatsr.ref | f        |   584
 metop.ref | t        |  1449
 metop.ref | f        |  1225
(4 rows)

aatsr-seviri coincidences:

select o1.time, o1.time - o2.time as deltat, o1.clearsky, o1.name, o1.recordNo, o2.name, o2.recordNo
from mm_matchup m, mm_observation o1, mm_observation o2, mm_coincidence c2
where o1.sensor = 'aatsr.ref' and o2.sensor = 'seviri' and m.refobs_id = o1.id and c2.matchup_id = m.id and c2.observation_id = o2.id
order by o1.time;

select o1.time, o1.sensor, o1.name, o1.recordno, o2.sensor, o2.name, o2.recordno, o3.sensor, o3.name, o3.recordno, o2.time-o1.time as deltat2, o3.time - o1.time as deltat3
from mm_matchup m, mm_observation o1, mm_observation o2, mm_observation o3, mm_coincidence c2, mm_coincidence c3
where m.refobs_id = o1.id and c2.matchup_id = m.id and c2.observation_id = o2.id and c3.matchup_id = m.id and c3.observation_id = o3.id and o2.sensor < o3.sensor and o1.clearsky = true
order by o1.time;
