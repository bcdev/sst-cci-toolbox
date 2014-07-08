#!/bin/bash

# TODO - replace paths with properties?

now=`date +'%Y-%m-%dT%H-%M-%S'`

# stop database from writing into tablespaces
${mms.pg.home}/bin/psql template1 << EOF
select pg_start_backup('full-backup-$now');
EOF

# move old write-ahead logs away
mv ${mms.pg.backup}/full_wal ${mms.pg.backup}/full_wal-$now
mkdir ${mms.pg.backup}/full_wal

# create base backup
cd $(dirname ${mms.pg.data})
tar cjf ${mms.pg.backup}/base/${now}-mmdb-backup.tar.bz2 mmdb

# synchronize tablespaces
${mms.pg.home}/bin/psql template1 << EOF
select pg_stop_backup();
EOF

echo "databases archived in ${mms.pg.backup}/base/${now}-mmdb-backup.tar.bz2"
echo
