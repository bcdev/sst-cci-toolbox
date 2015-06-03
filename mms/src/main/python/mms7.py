from workflow import Period
from workflow import Workflow

usecase = 'mms7'
mmdtype = 'mmd7'

w = Workflow(usecase, Period('2012-07-02', '2015-01-01'))
w.add_primary_sensor('avhrr_f.m01', '2012-12-13', '2014-04-01')
w.add_primary_sensor('avhrr_f.m02', '2007-03-01', '2014-04-01')
w.add_secondary_sensor('amsr2', '2012-07-02', '2015-04-01')
w.set_samples_per_month(5000000)

w.run(mmdtype, hosts=[('localhost', 12)], without_arc=True, miz_only=True)
