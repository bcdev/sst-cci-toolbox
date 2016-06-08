__author__ = 'Ralf Quast'

from svrworkflow import SvrWorkflow

usecase = 'l3u'
version = 'v2.2.0'
archive_root = '/group_workspaces/cems2/esacci_sst/output'
report_root = '/group_workspaces/cems2/esacci_sst/mms/svr'

w = SvrWorkflow(usecase, version, archive_root, report_root)
#w.add_sensor('ATSR1', '1991-08-01', '1996-09-01')
#w.add_sensor('ATSR1', '1996-10-01', '1996-11-01')
#w.add_sensor('ATSR1', '1996-12-30', '1997-02-01')
#w.add_sensor('ATSR1', '1997-03-01', '1997-04-01')
#w.add_sensor('ATSR1', '1997-05-01', '1997-06-01')
#w.add_sensor('ATSR1', '1997-07-01', '1997-09-01')
#w.add_sensor('ATSR1', '1997-10-01', '1997-11-01')
#w.add_sensor('ATSR1', '1997-12-01', '1997-12-18')
#w.add_sensor('ATSR2', '1995-06-01', '1996-01-01')
#w.add_sensor('ATSR2', '1996-07-01', '2003-06-23')
#w.add_sensor('AATSR', '2002-05-20', '2012-04-09')

#w.add_sensor('AVHRR06_G', '1979-07-12', '1982-03-18')
#w.add_sensor('AVHRR07_G', '1981-09-01', '1985-01-31')
#w.add_sensor('AVHRR08_G', '1983-05-04', '1985-10-04')
#w.add_sensor('AVHRR09_G', '1985-02-27', '1988-11-08')
w.add_sensor('AVHRR10_G', '1986-11-17', '1991-09-17')
#w.add_sensor('AVHRR11_G', '1988-11-08', '1995-01-01')
#w.add_sensor('AVHRR12_G', '1991-09-16', '1998-12-15')
#w.add_sensor('AVHRR14_G', '1995-01-01', '2002-10-08')
#w.add_sensor('AVHRR15_G', '1998-10-26', '2011-01-01')
#w.add_sensor('AVHRR16_G', '2001-01-01', '2011-01-01')
#w.add_sensor('AVHRR17_G', '2002-06-25', '2011-01-01')
#w.add_sensor('AVHRR18_G', '2005-05-20', '2015-10-01')
#w.add_sensor('AVHRR19_G', '2009-02-07', '2015-10-01')
#w.add_sensor('AVHRRMTA_G', '2006-10-30', '2015-10-01')
w.run()
