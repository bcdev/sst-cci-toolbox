from pmonitor import PMonitor

from workflow import Period
from workflow import Workflow

usecase = 'mms10a'
mmdtype = 'mmd10'

w = Workflow(usecase, Period('1991-01-01', '1992-01-01'))
w.add_primary_sensor('atsr.1', '1991-08-01', '1996-09-01')
w.add_primary_sensor('atsr.1', '1996-10-01', '1997-02-01')
w.add_primary_sensor('atsr.1', '1997-03-01', '1997-04-01')
w.add_primary_sensor('atsr.1', '1997-05-01', '1997-06-01')
w.add_primary_sensor('atsr.1', '1997-07-01', '1997-09-01')
w.add_primary_sensor('atsr.1', '1997-10-01', '1997-11-01')
w.add_primary_sensor('atsr.1', '1997-12-01', '1997-12-18')
w.add_primary_sensor('atsr.2', '1995-06-01', '1996-01-01')
w.add_primary_sensor('atsr.2', '1996-07-01', '2003-06-23')
w.add_primary_sensor('atsr.3', '2002-05-20', '2012-04-09')
w.add_secondary_sensor('avhrr.n10', '1986-11-17', '1991-09-17')
w.add_secondary_sensor('avhrr.n11', '1988-11-08', '1995-01-01')
w.add_secondary_sensor('avhrr.n12', '1991-09-16', '1998-12-15')
w.set_samples_per_month(500000)

hosts = [('localhost', 60)]

w.run(mmdtype, hosts)
