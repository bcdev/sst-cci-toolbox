from workflow import Workflow

usecase = 'mms12'
mmdtype = 'mmd12'

w = Workflow(usecase)
w.add_primary_sensor('avhrr_f.m01', '2012-12-13', '2016-01-04')
w.add_primary_sensor('avhrr_f.m02', '2007-03-01', '2016-01-04')
w.set_samples_per_month(0)

w.run(mmdtype, hosts=[('localhost', 8)], calls=[('sampling-start.sh', 1), ('coincidence-start.sh', 1)], with_history=True)
