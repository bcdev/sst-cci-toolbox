#! /bin/sh

if [ -z "$1" ]; then
    echo
    echo usage: mmsshapes.sh matchupId
    echo
    exit 2
fi

pgsql2shp -h 10.3.0.35 -u mms -f matchup$1-amsre -P mms mmdb "select o.location from mm_observation o, mm_matchup m, mm_coincidence c where m.id = $1 and c.matchup_id = m.id and c.observation_id = o.id and o.sensor = 'amsre'"
pgsql2shp -h 10.3.0.35 -u mms -f matchup$1-avhrr -P mms mmdb "select o.location from mm_observation o, mm_matchup m, mm_coincidence c where m.id = $1 and c.matchup_id = m.id and c.observation_id = o.id and o.sensor = 'avhrr'"
pgsql2shp -h 10.3.0.35 -u mms -f matchup$1-aatsr -P mms mmdb "select o.location from mm_observation o, mm_matchup m, mm_coincidence c where m.id = $1 and c.matchup_id = m.id and c.observation_id = o.id and o.sensor = 'aatsr'"
pgsql2shp -h 10.3.0.35 -u mms -f matchup$1-mds -P mms mmdb "select o.location from mm_observation o, mm_matchup m, mm_coincidence c where m.id = $1 and c.matchup_id = m.id and c.observation_id = o.id and (o.sensor = 'metop' or o.sensor = 'seviri')"
pgsql2shp -h 10.3.0.35 -u mms -f matchup$1-ref -P mms mmdb "select o.location from mm_observation o, mm_matchup m where m.id = $1 and m.refobs_id = o.id"
