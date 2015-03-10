from workflow import Workflow

usecase = 'mms6b'
mmdtype = 'mmd6'

w = Workflow(usecase)
w.add_primary_sensor('amsr2', '2012-07-02', '2015-01-01')
w.set_samples_per_month(300000)

w.run(mmdtype, hosts=[('localhost', 48)], calls=[('sampling-start', 1)], with_history=True, without_arc=True)
