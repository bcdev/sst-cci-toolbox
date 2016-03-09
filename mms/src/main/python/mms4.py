from workflow import Workflow

usecase = 'mms4'
mmdtype = 'mmd4'

w = Workflow(usecase)
# this is the full list
# w.add_primary_sensor('avhrr.n06', '1979-07-12', '1982-03-17')
# w.add_primary_sensor('avhrr.n07', '1981-09-01', '1985-02-01')
# w.add_primary_sensor('avhrr.n08', '1983-05-04', '1985-10-04')
# w.add_primary_sensor('avhrr.n09', '1985-02-27', '1988-11-08')
# w.add_primary_sensor('avhrr.n10', '1986-11-17', '1991-09-17')
# w.add_primary_sensor('avhrr.n11', '1988-11-08', '1995-01-01')
# w.add_primary_sensor('avhrr.n12', '1991-09-16', '1998-12-15')
# w.add_primary_sensor('avhrr.n14', '1995-01-01', '2002-10-08')
# w.add_primary_sensor('avhrr.n15', '1998-10-26', '2013-01-01')
# w.add_primary_sensor('avhrr.n16', '2001-01-01', '2013-01-01')
# w.add_primary_sensor('avhrr.n17', '2002-06-25', '2013-01-01')
# w.add_primary_sensor('avhrr.n18', '2005-05-20', '2015-12-01')
# w.add_primary_sensor('avhrr.n19', '2009-02-07', '2015-12-01')
# w.add_primary_sensor('avhrr.m02', '2006-10-30', '2015-12-01')

# this one does the after-failure-processing 2016-03-09
w.add_primary_sensor('avhrr.n14', '1995-01-01', '2002-10-08')
w.add_primary_sensor('avhrr.n15', '2006-12-01', '2013-01-01')
w.add_primary_sensor('avhrr.n16', '2006-01-01', '2013-01-01')
w.add_primary_sensor('avhrr.n17', '2005-12-01', '2013-01-01')
w.add_primary_sensor('avhrr.n18', '2005-11-01', '2015-12-01')
w.add_primary_sensor('avhrr.n19', '2009-02-07', '2015-12-01')
w.add_primary_sensor('avhrr.m02', '2006-10-30', '2015-12-01')
w.set_samples_per_month(0)

w.run(mmdtype, hosts=[('localhost', 8)], calls=[('sampling-start.sh', 1), ('coincidence-start.sh', 1)], with_history=True)
