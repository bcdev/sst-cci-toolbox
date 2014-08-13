from workflow import Workflow

usecase = 'mms3'
mmdtype = 'mmd3'

w = Workflow(usecase)
w.add_primary_sensor('atsr.1', '1991-08-01', '1997-12-18')
w.add_primary_sensor('atsr.2', '1995-06-01', '2003-06-23')
w.add_primary_sensor('atsr.3', '2002-05-20', '2012-04-09')
w.set_samples_per_month(0)

w.run(mmdtype, with_history=True)
