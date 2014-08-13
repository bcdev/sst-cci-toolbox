from workflow import Workflow

usecase = 'mms1a'
mmdtype = 'mmd1'

w = Workflow(usecase)
w.add_primary_sensor('atsr.2', '1995-06-01', '2003-06-23')
w.add_secondary_sensor('atsr.1', '1991-08-01', '1997-12-18')
w.set_samples_per_month(15000000)

w.run(mmdtype)
