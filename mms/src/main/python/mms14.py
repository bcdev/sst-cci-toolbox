from workflow import Workflow

usecase = 'mms14'
mmdtype = 'mmd14'

w = Workflow(usecase)
# the following lines define the full MMD tb 2016-08-01
#w.add_primary_sensor('atsr.3', '2002-05-20', '2012-04-09')
#w.add_primary_sensor('amsre', '2002-06-01', '2011-10-05')

# the following lines define the test MMD tb 2016-08-01
w.add_primary_sensor('atsr.3', '2008-05-01', '2008-06-01')
w.add_secondary_sensor('amsre', '2008-05-01', '2008-06-01')

w.set_samples_per_month(100000)
w.run(mmdtype, hosts=[('localhost', 8)])
