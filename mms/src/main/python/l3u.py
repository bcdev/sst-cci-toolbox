__author__ = 'Ralf Quast'

from svrworkflow import SvrWorkflow

usecase = 'l3u'
version = 'EXP1.2-rc2'
archive_root = '/group_workspaces/cems2/esacci_sst/output'
report_root = '/group_workspaces/cems2/esacci_sst/mms/svr'

w = SvrWorkflow(usecase, version, archive_root, report_root)
w.add_sensor('ATSR1', '1991-08-01', '1996-09-01')
w.add_sensor('ATSR1', '1996-10-01', '1996-11-01')
w.add_sensor('ATSR1', '1996-12-30', '1997-02-01')
w.add_sensor('ATSR1', '1997-03-01', '1997-04-01')
w.add_sensor('ATSR1', '1997-05-01', '1997-06-01')
w.add_sensor('ATSR1', '1997-07-01', '1997-09-01')
w.add_sensor('ATSR1', '1997-10-01', '1997-11-01')
w.add_sensor('ATSR1', '1997-12-01', '1997-12-18')
w.add_sensor('ATSR2', '1995-06-01', '1996-01-01')
w.add_sensor('ATSR2', '1996-07-01', '2003-06-23')
w.add_sensor('AATSR', '2002-05-20', '2012-04-09')
w.run()
