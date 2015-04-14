from workflow import Workflow

usecase = 'mms6a'
mmdtype = 'mmd6'

w = Workflow(usecase)
w.add_primary_sensor('avhrr_f.m01', '2012-12-13', '2014-04-01')
w.add_primary_sensor('avhrr_f.m02', '2007-03-01', '2014-04-01')
w.add_primary_sensor('amsr2', '2012-07-02', '2015-04-01')
w.set_samples_per_month(3000000)

w.run(mmdtype, hosts=[('localhost', 48)], calls=[('sampling-start', 1)], with_history=True, without_arc=True)
