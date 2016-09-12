from workflow import Period
from workflow import Workflow

usecase = 'mms6c'
mmdtype = 'mmd6'

w = Workflow(usecase, Period('2002-06-01', '2011-10-05'))
w.add_primary_sensor('amsre', '2002-06-01', '2011-10-05')
w.set_samples_per_month(3000000)

w.run(mmdtype, hosts=[('localhost', 24)],
      calls=[('sampling-start.sh', 1), ('coincidence-start.sh', 2), ('sub-start.sh', 2), ('mmd-start.sh', 2)],
      with_history=True, without_arc=True)
